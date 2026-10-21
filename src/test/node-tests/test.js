const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const assert = require('assert');
const { instantiateJavaw } = require('../../main/resources/javaw-runtime.js');

// Configuration
const JAR_PATH = path.resolve(__dirname, '../../../target/javaw-1.0-SNAPSHOT.jar');
const TESTCLASS_JAVA = path.resolve(__dirname, '../java/com/example/javaw/TestClass.java');
const TESTCLASS_WAT = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wat');
const TESTCLASS_WASM = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wasm');

const APP_JAVA = path.resolve(__dirname, '../java/com/example/javaw/App.java');
const APP_WAT = path.resolve(__dirname, '../java/com/example/javaw/App.wat');
const APP_WASM = path.resolve(__dirname, '../java/com/example/javaw/App.wasm');

// Lightweight DOM Mock for Node.js testing
class MockElement {
    constructor(tagName) {
        this.tagName = tagName.toUpperCase();
        this.attributes = {};
        this.children = [];
        this.innerText = '';
        this.listeners = {};
    }

    setAttribute(name, value) {
        this.attributes[name] = value;
        if (name === 'id' && this.doc) {
            this.doc.registerElement(value, this);
        }
    }

    getAttribute(name) {
        return this.attributes[name];
    }

    appendChild(child) {
        this.children.push(child);
    }

    addEventListener(event, handler) {
        if (!this.listeners[event]) this.listeners[event] = [];
        this.listeners[event].push(handler);
    }

    click() {
        const handlers = this.listeners['click'] || [];
        for (const h of handlers) {
            h({ type: 'click' });
        }
    }
}

class MockDocument {
    constructor() {
        this.elementsById = new Map();
        this.body = new MockElement('BODY');
        this.body.doc = this;
    }

    createElement(tag) {
        const el = new MockElement(tag);
        el.doc = this;
        return el;
    }

    getElementById(id) {
        return this.elementsById.get(id) || null;
    }

    registerElement(id, el) {
        this.elementsById.set(id, el);
    }
}

async function runTest() {
    console.log('Starting javaw end-to-end test suite...');

    try {
        // ==========================================
        // Part 1: Arithmetic & Function Exports
        // ==========================================
        console.log('\n--- Testing TestClass.java (Arithmetic & Primitives) ---');
        if (fs.existsSync(TESTCLASS_WAT)) fs.unlinkSync(TESTCLASS_WAT);
        if (fs.existsSync(TESTCLASS_WASM)) fs.unlinkSync(TESTCLASS_WASM);

        execSync(`java -jar ${JAR_PATH} ${TESTCLASS_JAVA}`, { stdio: 'inherit' });
        assert.strictEqual(fs.existsSync(TESTCLASS_WAT), true, 'TestClass.wat must exist');

        const testContent = fs.readFileSync(TESTCLASS_WAT, 'utf8');
        assert.ok(testContent.includes('(func $addNumbers'), 'Must contain addNumbers');
        assert.ok(testContent.includes('(func $getPi'), 'Must contain getPi');

        if (fs.existsSync(TESTCLASS_WASM)) {
            const wasmBytes = fs.readFileSync(TESTCLASS_WASM);
            const { instance } = await WebAssembly.instantiate(wasmBytes);
            assert.strictEqual(instance.exports.addNumbers(15, 27), 42);
            assert.ok(Math.abs(instance.exports.getPi() - 3.14159) < 0.0001);
            console.log('✓ TestClass execution assertions passed.');
        }

        // ==========================================
        // Part 2: DOM Operations & Event Handling
        // ==========================================
        console.log('\n--- Testing App.java (DOM Operations & Events) ---');
        if (fs.existsSync(APP_WAT)) fs.unlinkSync(APP_WAT);
        if (fs.existsSync(APP_WASM)) fs.unlinkSync(APP_WASM);

        execSync(`java -jar ${JAR_PATH} ${APP_JAVA}`, { stdio: 'inherit' });
        assert.strictEqual(fs.existsSync(APP_WAT), true, 'App.wat must exist');

        const appWat = fs.readFileSync(APP_WAT, 'utf8');
        assert.ok(appWat.includes('(import "dom" "getElementById"'), 'Must declare getElementById import');
        assert.ok(appWat.includes('(import "dom" "createElement"'), 'Must declare createElement import');
        assert.ok(appWat.includes('(import "dom" "setInnerText"'), 'Must declare setInnerText import');
        assert.ok(appWat.includes('(import "dom" "setAttribute"'), 'Must declare setAttribute import');
        assert.ok(appWat.includes('(import "dom" "appendChild"'), 'Must declare appendChild import');
        assert.ok(appWat.includes('(import "dom" "addEventListener"'), 'Must declare addEventListener import');
        assert.ok(appWat.includes('(memory (export "memory") 1)'), 'Must export linear memory');
        console.log('✓ App.wat import signatures and memory declarations verified.');

        if (fs.existsSync(APP_WASM)) {
            console.log('Testing App.wasm execution with Mock DOM...');
            const mockDoc = new MockDocument();
            const rootApp = mockDoc.createElement('div');
            rootApp.setAttribute('id', 'app');
            mockDoc.body.appendChild(rootApp);

            const appBytes = fs.readFileSync(APP_WASM);
            const { instance, exports } = await instantiateJavaw(appBytes, {}, mockDoc);

            assert.strictEqual(typeof exports.start, 'function', 'start() method should be exported');
            assert.strictEqual(typeof exports.increment, 'function', 'increment() method should be exported');

            // Run application initialization
            exports.start();

            // Verify DOM mutations
            assert.strictEqual(rootApp.children.length, 3, 'App container should contain 3 children');
            const [heading, status, btn] = rootApp.children;

            assert.strictEqual(heading.tagName, 'H1');
            assert.strictEqual(heading.getAttribute('class'), 'app-title');
            assert.strictEqual(heading.innerText, 'Java to WebAssembly DOM App');
            console.log('✓ Heading element created and styled correctly.');

            assert.strictEqual(status.tagName, 'P');
            assert.strictEqual(status.getAttribute('id'), 'status-text');
            assert.strictEqual(status.innerText, 'Count: 0');
            console.log('✓ Status paragraph created with initial count.');

            assert.strictEqual(btn.tagName, 'BUTTON');
            assert.strictEqual(btn.getAttribute('id'), 'btn-increment');
            assert.strictEqual(btn.innerText, 'Increment Counter');
            console.log('✓ Button created with click listener attached.');

            // Test Interactive Event Dispatch
            btn.click();
            assert.strictEqual(status.innerText, 'Count: 1 click', 'Status text after 1st click');
            console.log('✓ Click 1: status updated to "Count: 1 click".');

            btn.click();
            assert.strictEqual(status.innerText, 'Count: 2 clicks', 'Status text after 2nd click');
            console.log('✓ Click 2: status updated to "Count: 2 clicks".');

            btn.click();
            assert.strictEqual(status.innerText, 'Count: multiple clicks', 'Status text after 3rd click');
            console.log('✓ Click 3: status updated to "Count: multiple clicks".');
        }

        console.log('\n==========================================');
        console.log('🎉 ALL INTEGRATION TESTS PASSED SUCCESSFULLY!');
        console.log('==========================================');

    } catch (error) {
        console.error('\n❌ TEST FAILED!');
        console.error(error.message);
        console.error(error.stack);
        process.exit(1);
    }
}

runTest();
