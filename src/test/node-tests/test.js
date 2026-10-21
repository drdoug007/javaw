const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const assert = require('assert');
const { instantiateJavaw } = require('../../main/resources/javaw-runtime.js');

function readWasmString(buffer, ptr) {
    if (!ptr) return '';
    const u8 = new Uint8Array(buffer, ptr);
    let len = 0;
    while (u8[len] !== 0) len++;
    return new TextDecoder('utf-8').decode(u8.subarray(0, len));
}

function writeWasmString(instance, str) {
    const bytes = Buffer.from(str, 'utf8');
    const ptr = instance.exports.__str_alloc(bytes.length);
    const view = new Uint8Array(instance.exports.memory.buffer);
    for (let i = 0; i < bytes.length; i++) {
        view[ptr + i] = bytes[i];
    }
    view[ptr + bytes.length] = 0;
    return ptr;
}

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
        assert.ok(testContent.includes('(func $calculateFactorial'), 'Must contain calculateFactorial');
        assert.ok(testContent.includes('(func $sumRangeWithSkips'), 'Must contain sumRangeWithSkips');
        assert.ok(testContent.includes('(func $sumArray'), 'Must contain sumArray');
        assert.ok(testContent.includes('(func $sumArrayWithSkips'), 'Must contain sumArrayWithSkips');
        assert.ok(testContent.includes('(func $sumDoubleArray'), 'Must contain sumDoubleArray');
        assert.ok(testContent.includes('(func $sumInlineArray'), 'Must contain sumInlineArray');
        assert.ok(testContent.includes('(func $createAndSumArray'), 'Must contain createAndSumArray');
        assert.ok(testContent.includes('(func $evaluateSwitch'), 'Must contain evaluateSwitch');
        assert.ok(testContent.includes('(func $switchFallthrough'), 'Must contain switchFallthrough');
        assert.ok(testContent.includes('(func $switchInsideLoop'), 'Must contain switchInsideLoop');

        if (fs.existsSync(TESTCLASS_WASM)) {
            const wasmBytes = fs.readFileSync(TESTCLASS_WASM);
            const { instance } = await WebAssembly.instantiate(wasmBytes);
            assert.strictEqual(instance.exports.addNumbers(15, 27), 42);
            assert.ok(Math.abs(instance.exports.getPi() - 3.14159) < 0.0001);
            assert.strictEqual(instance.exports.calculateFactorial(5), 120);
            assert.strictEqual(instance.exports.sumRangeWithSkips(10), 33);

            // Test for-each loop with array inline creation & allocation
            assert.strictEqual(instance.exports.sumInlineArray(), 100);
            assert.strictEqual(instance.exports.createAndSumArray(15, 25, 35), 75);

            // Test for-each loop with memory buffer array passing
            const memory = instance.exports.memory;
            const intArrPtr = 80000;
            const intArrView = new Int32Array(memory.buffer, intArrPtr, 6);
            intArrView[0] = 5; // length
            intArrView[1] = 10;
            intArrView[2] = 20;
            intArrView[3] = 30;
            intArrView[4] = 40;
            intArrView[5] = 50;
            assert.strictEqual(instance.exports.sumArray(intArrPtr), 150);

            // Test for-each loop with skips (continue on 3, break on >8)
            const skipArrPtr = 81000;
            const skipArrView = new Int32Array(memory.buffer, skipArrPtr, 6);
            skipArrView[0] = 5; // length
            skipArrView[1] = 1;
            skipArrView[2] = 2;
            skipArrView[3] = 3; // skipped
            skipArrView[4] = 4;
            skipArrView[5] = 10; // triggers break
            assert.strictEqual(instance.exports.sumArrayWithSkips(skipArrPtr), 7); // 1 + 2 + 4 = 7

            // Test for-each loop over double[] array
            const dblArrPtr = 82000;
            new Int32Array(memory.buffer, dblArrPtr, 1)[0] = 3; // length
            const dblView = new Float64Array(memory.buffer, dblArrPtr + 8, 3);
            dblView[0] = 1.5;
            dblView[1] = 2.5;
            dblView[2] = 3.5;
            assert.ok(Math.abs(instance.exports.sumDoubleArray(dblArrPtr) - 7.5) < 0.0001);

            // Test Switch statements
            assert.strictEqual(instance.exports.evaluateSwitch(1), 100);
            assert.strictEqual(instance.exports.evaluateSwitch(2), 200);
            assert.strictEqual(instance.exports.evaluateSwitch(3), 300);
            assert.strictEqual(instance.exports.evaluateSwitch(99), -1);

            // Test Switch fallthrough
            assert.strictEqual(instance.exports.switchFallthrough(1), 30); // 10 + 20 (falls through to case 2 and breaks)
            assert.strictEqual(instance.exports.switchFallthrough(2), 20); // 20
            assert.strictEqual(instance.exports.switchFallthrough(3), 30); // 30
            assert.strictEqual(instance.exports.switchFallthrough(99), 5); // default

            // Test Switch inside loop
            assert.strictEqual(instance.exports.switchInsideLoop(5), 63); // i=1(1) + i=2(20) + i=3(1) + i=4(40) + i=5(1) = 63

            // Test Dynamic String Concatenation & Core String Methods
            const helloStrPtr = instance.exports.concatStrings();
            const helloStr = readWasmString(instance.exports.memory.buffer, helloStrPtr);
            assert.strictEqual(helloStr, 'Hello, WebAssembly!');

            const alicePtr = writeWasmString(instance, 'Alice');
            const formattedPtr = instance.exports.formatUser(alicePtr, 28);
            const formatted = readWasmString(instance.exports.memory.buffer, formattedPtr);
            assert.strictEqual(formatted, 'User: Alice (age: 28)');

            assert.strictEqual(instance.exports.getStringLength(), 11);
            assert.strictEqual(instance.exports.getCharAtIndex(3), 'A'.charCodeAt(0));

            const subStrPtr = instance.exports.getSubString(6, 11);
            const subStr = readWasmString(instance.exports.memory.buffer, subStrPtr);
            assert.strictEqual(subStr, 'World');

            const applePtr1 = writeWasmString(instance, 'Apple');
            const applePtr2 = writeWasmString(instance, 'Apple');
            const orangePtr = writeWasmString(instance, 'Orange');
            assert.strictEqual(instance.exports.compareStrings(applePtr1, applePtr2), 1);
            assert.strictEqual(instance.exports.compareStrings(applePtr1, orangePtr), 0);
            assert.strictEqual(instance.exports.findSubstringIndex(), 6);
            assert.strictEqual(instance.exports.findMissingSubstringIndex(), -1);

            // Test StringBuilder operations
            const builtStrPtr = instance.exports.buildCustomString(5);
            const builtStr = readWasmString(instance.exports.memory.buffer, builtStrPtr);
            assert.strictEqual(builtStr, 'Count: 0 1 2 3 4 ');

            const sbManipPtr = instance.exports.manipulateStringBuilder();
            const sbManip = readWasmString(instance.exports.memory.buffer, sbManipPtr);
            assert.strictEqual(sbManip, 'javaw');

            console.log('✓ Dynamic String & StringBuilder assertions passed.');

            // Test Labeled Break and Continue
            assert.strictEqual(instance.exports.labeledBreakNestedLoops(5, 5), 11);
            assert.strictEqual(instance.exports.labeledContinueNestedLoops(4, 4), 8);
            assert.strictEqual(instance.exports.labeledBlockBreak(1), 15);
            assert.strictEqual(instance.exports.labeledBlockBreak(0), 30);
            assert.strictEqual(instance.exports.labeledWhileBreak(3), 4);

            // Test Object-Oriented Programming (OOP), Polymorphism & Memory Allocation
            assert.strictEqual(instance.exports.testPointObject(3, 4), 52);
            assert.strictEqual(instance.exports.testDirectFieldAccess(10, 20), 45);
            assert.strictEqual(instance.exports.testPolymorphismArea(1, 5, 0), 75);
            assert.strictEqual(instance.exports.testPolymorphismArea(2, 4, 6), 24);
            assert.strictEqual(instance.exports.testInheritedColor(1, 100, 5, 0), 100);
            assert.strictEqual(instance.exports.testInheritedColor(2, 200, 4, 6), 200);
            assert.strictEqual(instance.exports.testInstanceOf(1), 1);
            assert.strictEqual(instance.exports.testInstanceOf(2), 2);
            assert.strictEqual(instance.exports.testObjectArray(), 91);

            console.log('✓ OOP & Polymorphism assertions passed (objects, constructors, fields, vtable dispatch, instanceof, object arrays).');
            console.log('✓ TestClass execution assertions passed (including for loops, for-each loops, arrays, switches, labeled break/continue, compound assignments, dynamic strings, and OOP).');
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
        assert.ok(appWat.includes('(memory (export "memory")'), 'Must export linear memory');
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
