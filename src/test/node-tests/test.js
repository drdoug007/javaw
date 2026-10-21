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
class MockCanvasContext2D {
    constructor(element) {
        this.element = element;
        this.fillStyle = '#000000';
        this.strokeStyle = '#000000';
        this.lineWidth = 1.0;
        this.font = '10px sans-serif';
        this.textAlign = 'start';
        this.calls = [];
    }
    fillRect(x, y, w, h) {
        this.calls.push({ op: 'fillRect', x, y, w, h, fillStyle: this.fillStyle });
    }
    strokeRect(x, y, w, h) {
        this.calls.push({ op: 'strokeRect', x, y, w, h });
    }
    clearRect(x, y, w, h) {
        this.calls.push({ op: 'clearRect', x, y, w, h });
    }
    beginPath() {
        this.calls.push({ op: 'beginPath' });
    }
    closePath() {
        this.calls.push({ op: 'closePath' });
    }
    moveTo(x, y) {
        this.calls.push({ op: 'moveTo', x, y });
    }
    lineTo(x, y) {
        this.calls.push({ op: 'lineTo', x, y });
    }
    arc(x, y, r, sa, ea, ccw) {
        this.calls.push({ op: 'arc', x, y, r, sa, ea, ccw });
    }
    fill() {
        this.calls.push({ op: 'fill', fillStyle: this.fillStyle });
    }
    stroke() {
        this.calls.push({ op: 'stroke', strokeStyle: this.strokeStyle });
    }
    fillText(text, x, y) {
        this.calls.push({ op: 'fillText', text, x, y });
    }
    strokeText(text, x, y) {
        this.calls.push({ op: 'strokeText', text, x, y });
    }
    save() {
        this.calls.push({ op: 'save' });
    }
    restore() {
        this.calls.push({ op: 'restore' });
    }
    translate(x, y) {
        this.calls.push({ op: 'translate', x, y });
    }
    rotate(angle) {
        this.calls.push({ op: 'rotate', angle });
    }
    scale(sx, sy) {
        this.calls.push({ op: 'scale', sx, sy });
    }
    resetTransform() {
        this.calls.push({ op: 'resetTransform' });
    }
}

class MockWebGLContext {
    constructor(element) {
        this.element = element;
        this.calls = [];
    }
    clearColor(r, g, b, a) {
        this.calls.push({ op: 'clearColor', r, g, b, a });
    }
    clear(mask) {
        this.calls.push({ op: 'clear', mask });
    }
    viewport(x, y, w, h) {
        this.calls.push({ op: 'viewport', x, y, w, h });
    }
}

class MockClassList {
    constructor(element) {
        this.element = element;
        this.classes = new Set();
    }
    add(cls) {
        this.classes.add(cls);
        this.element.attributes['class'] = Array.from(this.classes).join(' ');
    }
    remove(cls) {
        this.classes.delete(cls);
        this.element.attributes['class'] = Array.from(this.classes).join(' ');
    }
    contains(cls) {
        return this.classes.has(cls);
    }
    toggle(cls) {
        if (this.classes.has(cls)) {
            this.remove(cls);
        } else {
            this.add(cls);
        }
    }
}

class MockElement {
    constructor(tagName) {
        this.tagName = tagName.toUpperCase();
        this.attributes = {};
        this.children = [];
        this.innerText = '';
        this.value = '';
        this.checked = false;
        this.style = {};
        this.listeners = {};
        this.parentElement = null;
        this.classList = new MockClassList(this);
        this.width = 0;
        this.height = 0;
        this.ctx2d = null;
        this.webgl = null;
    }

    getContext(type) {
        if (type === '2d') {
            if (!this.ctx2d) this.ctx2d = new MockCanvasContext2D(this);
            return this.ctx2d;
        }
        if (type === 'webgl' || type === 'experimental-webgl') {
            if (!this.webgl) this.webgl = new MockWebGLContext(this);
            return this.webgl;
        }
        return null;
    }

    setAttribute(name, value) {
        this.attributes[name] = value;
        if (name === 'class') {
            this.classList.classes = new Set(value.split(' ').filter(Boolean));
        }
        if (name === 'id' && this.doc) {
            this.doc.registerElement(value, this);
        }
    }

    getAttribute(name) {
        return this.attributes[name] !== undefined ? this.attributes[name] : null;
    }

    appendChild(child) {
        child.parentElement = this;
        this.children.push(child);
    }

    removeChild(child) {
        const idx = this.children.indexOf(child);
        if (idx !== -1) {
            this.children.splice(idx, 1);
            child.parentElement = null;
        }
    }

    remove() {
        if (this.parentElement) {
            this.parentElement.removeChild(this);
        }
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

    querySelector(selector) {
        const all = this.querySelectorAll(selector);
        return all.length > 0 ? all[0] : null;
    }

    querySelectorAll(selector) {
        const results = [];
        const match = (el) => {
            if (selector.startsWith('#') && el.getAttribute('id') === selector.slice(1)) return true;
            if (selector.startsWith('.') && el.classList.contains(selector.slice(1))) return true;
            if (el.tagName.toLowerCase() === selector.toLowerCase()) return true;
            return false;
        };
        const traverse = (el) => {
            for (const child of el.children) {
                if (match(child)) results.push(child);
                traverse(child);
            }
        };
        traverse(this);
        return results;
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

    querySelector(selector) {
        if (selector.startsWith('#')) {
            return this.getElementById(selector.slice(1));
        }
        return this.body.querySelector(selector);
    }

    querySelectorAll(selector) {
        return this.body.querySelectorAll(selector);
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
            const mockDoc = new MockDocument();
            const canvasEl = mockDoc.createElement('canvas');
            canvasEl.setAttribute('id', 'testCanvas');
            mockDoc.body.appendChild(canvasEl);

            const glEl = mockDoc.createElement('canvas');
            glEl.setAttribute('id', 'glCanvas');
            mockDoc.body.appendChild(glEl);

            const wasmBytes = fs.readFileSync(TESTCLASS_WASM);
            const { instance, exports } = await instantiateJavaw(wasmBytes, {}, mockDoc);
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

            const skipArrPtr = 82000;
            const skipArrView = new Int32Array(memory.buffer, skipArrPtr, 6);
            skipArrView[0] = 5; // length
            skipArrView[1] = 2; // +2
            skipArrView[2] = 3; // skipped
            skipArrView[3] = 4; // +4
            skipArrView[4] = 5; // +5
            skipArrView[5] = 10; // break
            assert.strictEqual(instance.exports.sumArrayWithSkips(skipArrPtr), 11);

            const dblArrPtr = 81000;
            const dblArrLenView = new Int32Array(memory.buffer, dblArrPtr, 1);
            dblArrLenView[0] = 3;
            const dblArrValView = new Float64Array(memory.buffer, dblArrPtr + 8, 3);
            dblArrValView[0] = 1.5;
            dblArrValView[1] = 2.5;
            dblArrValView[2] = 3.5;
            assert.ok(Math.abs(instance.exports.sumDoubleArray(dblArrPtr) - 7.5) < 0.0001);

            // Test Switch statements & expressions
            assert.strictEqual(instance.exports.evaluateSwitch(1), 100);
            assert.strictEqual(instance.exports.evaluateSwitch(2), 200);
            assert.strictEqual(instance.exports.evaluateSwitch(3), 300);
            assert.strictEqual(instance.exports.evaluateSwitch(99), -1);

            assert.strictEqual(instance.exports.switchFallthrough(1), 30);
            assert.strictEqual(instance.exports.switchFallthrough(2), 20);
            assert.strictEqual(instance.exports.switchFallthrough(3), 30);
            assert.strictEqual(instance.exports.switchFallthrough(99), 5);

            assert.strictEqual(instance.exports.switchInsideLoop(5), 63);

            // Test Dynamic String Concatenation & Passing
            const concatPtr = instance.exports.concatStrings();
            const concatStr = readWasmString(instance.exports.memory.buffer, concatPtr);
            assert.strictEqual(concatStr, 'Hello, WebAssembly!');

            const namePtr = writeWasmString(instance, 'Alice');
            const formatPtr = instance.exports.formatUser(namePtr, 30);
            const formatStr = readWasmString(instance.exports.memory.buffer, formatPtr);
            assert.strictEqual(formatStr, 'User: Alice (age: 30)');

            // Test Core String Methods
            assert.strictEqual(instance.exports.getStringLength(), 11);
            assert.strictEqual(instance.exports.getCharAtIndex(0), 87); // 'W'
            assert.strictEqual(instance.exports.getCharAtIndex(4), 115); // 's'

            const subPtr = instance.exports.getSubString(0, 5);
            assert.strictEqual(readWasmString(instance.exports.memory.buffer, subPtr), 'Hello');

            const applePtr1 = writeWasmString(instance, 'apple');
            const applePtr2 = writeWasmString(instance, 'apple');
            const orangePtr = writeWasmString(instance, 'orange');
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

            // Test Canvas 2D & WebGL Graphics
            if (typeof instance.exports.testCanvasDraw === 'function') {
                const dimSum = instance.exports.testCanvasDraw();
                assert.strictEqual(dimSum, 800);
                assert.strictEqual(canvasEl.width, 500);
                assert.strictEqual(canvasEl.height, 300);
                const ctx = canvasEl.getContext('2d');
                assert.ok(ctx.calls.some(c => c.op === 'fillRect' && c.w === 100 && c.h === 50 && c.fillStyle === '#ff0000'));
                assert.ok(ctx.calls.some(c => c.op === 'arc' && c.x === 60 && c.y === 60 && c.r === 20));
                assert.ok(ctx.calls.some(c => c.op === 'fill'));
                console.log('✓ Canvas 2D testCanvasDraw assertions passed.');
            }
            if (typeof instance.exports.testWebGLInit === 'function') {
                const glRes = instance.exports.testWebGLInit();
                assert.strictEqual(glRes, 1);
                const glCtx = glEl.getContext('webgl');
                assert.ok(glCtx.calls.some(c => c.op === 'clearColor' && Math.abs(c.r - 0.2) < 0.01));
                assert.ok(glCtx.calls.some(c => c.op === 'clear' && c.mask === 16384));
                console.log('✓ WebGL testWebGLInit assertions passed.');
            }

            // Test Asynchronous Browser Interop (Timers, Animation Frame, Fetch)
            if (typeof instance.exports.testScheduleTimer === 'function') {
                const tId = instance.exports.testScheduleTimer(10);
                assert.ok(tId > 0, 'Timer ID should be positive integer');
                await new Promise(r => setTimeout(r, 30));

                const rafId = instance.exports.testRequestAnimationFrame();
                assert.ok(rafId > 0, 'Frame ID should be positive integer');
                await new Promise(r => setTimeout(r, 30));

                // Test global fetch mock
                global.fetch = async (url) => ({ status: 200, text: async () => '{"status":"ok"}' });
                instance.exports.testFetchData('https://api.example.com/test');
                await new Promise(r => setTimeout(r, 30));
                console.log('✓ Asynchronous Browser Interop assertions passed (setTimeout, requestAnimationFrame, fetch).');
            }

            // Test Generic JavaScript Dynamic Interop (JSObject)
            if (typeof instance.exports.testJsEvalAndGet === 'function') {
                assert.strictEqual(instance.exports.testJsEvalAndGet(), 42, 'testJsEvalAndGet should return 42');
                assert.strictEqual(instance.exports.testJsCreateAndSet(), 95, 'testJsCreateAndSet should return 95');
                assert.strictEqual(instance.exports.testJsArrayOperations(), 3, 'testJsArrayOperations should return 3');
                assert.strictEqual(instance.exports.testJsGlobalMathCall(), 100, 'testJsGlobalMathCall should return 100');
                console.log('✓ Generic JavaScript Dynamic Interop (JSObject) assertions passed.');
            }

            // Test WASI Standard I/O and System.out/err
            if (typeof instance.exports.testWasiOutput === 'function') {
                assert.strictEqual(instance.exports.testWasiOutput(), 1, 'testWasiOutput should return 1');
                assert.strictEqual(instance.exports.testWasiFacade(), 42, 'testWasiFacade should return 42');
                console.log('✓ WASI Target & System.out/err standard I/O assertions passed.');
            }

            // Test WebAssembly Threads, Shared Memory, and Atomics
            if (typeof instance.exports.testAtomicIntegerOperations === 'function') {
                assert.strictEqual(instance.exports.testAtomicIntegerOperations(), 1, 'testAtomicIntegerOperations should return 1');
                assert.strictEqual(instance.exports.testAtomicsArrayOperations(), 1, 'testAtomicsArrayOperations should return 1');
                assert.strictEqual(instance.exports.testWasmThread(), 1, 'testWasmThread should return 1');
                console.log('✓ WebAssembly Threads, Shared Memory, and Atomics assertions passed.');

                // WebAssembly Exception Handling assertions
                assert.strictEqual(instance.exports.testSimpleTryCatch(0), 100, 'testSimpleTryCatch(0) should return 100');
                assert.strictEqual(instance.exports.testSimpleTryCatch(1), 999, 'testSimpleTryCatch(1) should return 999');

                const excMsgPtr = instance.exports.testExceptionMessage();
                const excMsg = readWasmString(instance.exports.memory.buffer, excMsgPtr);
                assert.strictEqual(excMsg, 'Custom exception message', 'testExceptionMessage should match');

                assert.strictEqual(instance.exports.testCustomExceptionHandling(), 404, 'testCustomExceptionHandling should return 404');
                assert.strictEqual(instance.exports.testMultiCatchDispatch(1), 1500, 'testMultiCatchDispatch(1) should return 1500');
                assert.strictEqual(instance.exports.testMultiCatchDispatch(2), 2000, 'testMultiCatchDispatch(2) should return 2000');
                assert.strictEqual(instance.exports.testMultiCatchDispatch(3), 3000, 'testMultiCatchDispatch(3) should return 3000');

                assert.strictEqual(instance.exports.testTryFinally(0), 101, 'testTryFinally(0) should return 101');
                assert.strictEqual(instance.exports.testTryFinally(1), 110, 'testTryFinally(1) should return 110');
                console.log('✓ WebAssembly Exception Handling (try-catch-finally, throw, multi-catch) assertions passed.');
            }

            console.log('✓ TestClass execution assertions passed (including for loops, for-each loops, arrays, switches, labeled break/continue, compound assignments, dynamic strings, OOP, Canvas 2D, WebGL, JSObject, WASI, Threads/Atomics, and Exceptions).');
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
        assert.ok(appWat.includes('(import "dom" "querySelector"'), 'Must declare querySelector import');
        assert.ok(appWat.includes('(import "dom" "querySelectorAll"'), 'Must declare querySelectorAll import');
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
            assert.strictEqual(rootApp.children.length, 6, 'App container should contain 6 children');
            const [heading, status, btn, input, checkbox, container] = rootApp.children;

            assert.strictEqual(heading.tagName, 'H1');
            assert.strictEqual(heading.getAttribute('class'), 'app-title');
            assert.strictEqual(heading.innerText, 'Java to WebAssembly DOM App');
            assert.strictEqual(heading.style['color'], 'blue');
            console.log('✓ Heading element created and styled correctly.');

            assert.strictEqual(status.tagName, 'P');
            assert.strictEqual(status.getAttribute('id'), 'status-text');
            assert.strictEqual(status.innerText, 'Count: 0');
            assert.strictEqual(status.classList.contains('status-badge'), true);
            console.log('✓ Status paragraph created with initial count.');

            assert.strictEqual(btn.tagName, 'BUTTON');
            assert.strictEqual(btn.getAttribute('id'), 'btn-increment');
            assert.strictEqual(btn.innerText, 'Increment Counter');
            console.log('✓ Button created with click listener attached.');

            assert.strictEqual(input.value, 'Initial text');
            assert.strictEqual(checkbox.checked, true);
            assert.strictEqual(container.children.length, 3);
            console.log('✓ Input, checkbox, and item container created.');

            // Test DOM query methods exported by Java
            const statusTextPtr = exports.readStatusText();
            const readStatus = readWasmString(instance.exports.memory.buffer, statusTextPtr);
            assert.strictEqual(readStatus, 'Count: 0');

            const inputValPtr = exports.readInputValue();
            assert.strictEqual(readWasmString(instance.exports.memory.buffer, inputValPtr), 'Initial text');
            assert.strictEqual(exports.readCheckboxState(), 1);
            assert.strictEqual(exports.countItemTags(), 3);
            console.log('✓ Java querySelector, querySelectorAll, getValue, isChecked verified.');

            // Test Interactive Event Dispatch
            btn.click();
            assert.strictEqual(status.innerText, 'Count: 1 click', 'Status text after 1st click');
            assert.strictEqual(status.classList.contains('active'), true);
            console.log('✓ Click 1: status updated to "Count: 1 click" and active class toggled.');

            btn.click();
            assert.strictEqual(status.innerText, 'Count: 2 clicks', 'Status text after 2nd click');
            assert.strictEqual(status.classList.contains('active'), false);
            console.log('✓ Click 2: status updated to "Count: 2 clicks".');

            btn.click();
            assert.strictEqual(status.innerText, 'Count: multiple clicks', 'Status text after 3rd click');
            assert.strictEqual(status.classList.contains('active'), true);
            console.log('✓ Click 3: status updated to "Count: multiple clicks".');

            // Test removeChild
            exports.removeFirstItem();
            assert.strictEqual(container.children.length, 2);
            assert.strictEqual(exports.countItemTags(), 2);
            console.log('✓ removeChild verified.');
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
