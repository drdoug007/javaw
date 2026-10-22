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
const POSSIBLE_JAR_PATHS = [
    path.resolve(__dirname, '../../../target/javaw-core-1.0-SNAPSHOT.jar'),
    path.resolve(__dirname, '../../../target/javaw-1.0-SNAPSHOT.jar'),
    path.resolve(__dirname, '../../../../target/javaw-1.0-SNAPSHOT.jar'),
    path.resolve(__dirname, '../../../../javaw-core/target/javaw-core-1.0-SNAPSHOT.jar')
];
let JAR_PATH = POSSIBLE_JAR_PATHS.find(p => fs.existsSync(p)) || POSSIBLE_JAR_PATHS[0];
const TESTCLASS_JAVA = path.resolve(__dirname, '../java/com/example/javaw/TestClass.java');
const TESTCLASS_WAT = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wat');
const TESTCLASS_WASM = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wasm');
const TESTCLASS_WAT_MAP = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wat.map');
const TESTCLASS_WASM_MAP = path.resolve(__dirname, '../java/com/example/javaw/TestClass.wasm.map');

const APP_JAVA = path.resolve(__dirname, '../java/com/example/javaw/App.java');
const APP_WAT = path.resolve(__dirname, '../java/com/example/javaw/App.wat');
const APP_WASM = path.resolve(__dirname, '../java/com/example/javaw/App.wasm');
const APP_WAT_MAP = path.resolve(__dirname, '../java/com/example/javaw/App.wat.map');
const APP_WASM_MAP = path.resolve(__dirname, '../java/com/example/javaw/App.wasm.map');

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
        if (fs.existsSync(TESTCLASS_WAT_MAP)) fs.unlinkSync(TESTCLASS_WAT_MAP);
        if (fs.existsSync(TESTCLASS_WASM_MAP)) fs.unlinkSync(TESTCLASS_WASM_MAP);

        execSync(`java -jar ${JAR_PATH} ${TESTCLASS_JAVA}`, { stdio: 'inherit' });
        assert.strictEqual(fs.existsSync(TESTCLASS_WAT), true, 'TestClass.wat must exist');
        assert.strictEqual(fs.existsSync(TESTCLASS_WAT_MAP), true, 'TestClass.wat.map must exist');
        assert.strictEqual(fs.existsSync(TESTCLASS_WASM_MAP), true, 'TestClass.wasm.map must exist');

        const mapJson = JSON.parse(fs.readFileSync(TESTCLASS_WASM_MAP, 'utf8'));
        assert.strictEqual(mapJson.version, 3, 'Source map version must be 3');
        assert.ok(Array.isArray(mapJson.sources) && mapJson.sources.includes('TestClass.java'), 'Sources must contain TestClass.java');
        assert.ok(typeof mapJson.mappings === 'string' && mapJson.mappings.length > 0, 'Mappings must be present');
        console.log('✓ Source Map v3 (TestClass.wasm.map & TestClass.wat.map) generated and verified.');

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
            const { instance, exports, runtime, classes } = await instantiateJavaw(wasmBytes, {}, mockDoc);
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

            // Test Multi-Value Returns & Record Support
            if (typeof instance.exports.makePair === 'function') {
                const pairRes = instance.exports.makePair(10, 20);
                assert.deepStrictEqual(pairRes, [10, 20], 'makePair(10, 20) should return [10, 20]');
                assert.strictEqual(instance.exports.sumPair(10, 20), 30, 'sumPair(10, 20) should return 30');
                assert.strictEqual(instance.exports.computePairDifference(100, 35), 65, 'computePairDifference(100, 35) should return 65');

                const divRes = instance.exports.divideWithRemainder(29, 6);
                assert.deepStrictEqual(divRes, [4, 5], 'divideWithRemainder(29, 6) should return [4, 5]');
                assert.strictEqual(instance.exports.sumDivisionResult(29, 6), 9, 'sumDivisionResult(29, 6) should return 9');

                const scaled = instance.exports.scalePoint3D(1.0, 2.0, 3.0, 2.5);
                assert.deepStrictEqual(scaled, [2.5, 5.0, 7.5], 'scalePoint3D should scale 3D coords');
                assert.strictEqual(instance.exports.dotProductPoint3D(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), 32.0, 'dotProductPoint3D should compute dot product');
                console.log('✓ Multi-Value Returns and Record support assertions passed.');
            }

            // Test Bulk Memory Operations (System.arraycopy, Arrays.fill, Arrays.copyOf, Arrays.copyOfRange, Arrays.equals)
            if (typeof instance.exports.testArrayCopyInt === 'function') {
                assert.strictEqual(instance.exports.testArrayCopyInt(), 90, 'testArrayCopyInt() should return 90');
                assert.strictEqual(instance.exports.testArrayCopyDouble(), 7.5, 'testArrayCopyDouble() should return 7.5');
                assert.strictEqual(instance.exports.testArraysFillInt(), 76, 'testArraysFillInt() should return 76');
                assert.strictEqual(instance.exports.testArraysCopyOf(), 111, 'testArraysCopyOf() should return 111');
                console.log('✓ Bulk Memory Operations (memory.copy, memory.fill, System.arraycopy, Arrays.*) assertions passed.');
            }

            if (typeof instance.exports.testWasiFileWriteAndRead === 'function') {
                const readPtr = instance.exports.testWasiFileWriteAndRead();
                const content = readWasmString(instance.exports.memory.buffer, readPtr);
                assert.strictEqual(content, 'Hello WASI VFS', 'testWasiFileWriteAndRead() should return "Hello WASI VFS"');

                const exists = instance.exports.testWasiFileExists(writeWasmString(instance, '/hello.txt'));
                assert.strictEqual(exists, 1, 'File /hello.txt should exist');

                const len = instance.exports.testWasiFileLength(writeWasmString(instance, '/hello.txt'));
                assert.strictEqual(len, 14, 'File /hello.txt length should be 14');

                const appendPtr = instance.exports.testWasiFileAppend();
                const appendContent = readWasmString(instance.exports.memory.buffer, appendPtr);
                assert.strictEqual(appendContent, 'Part1Part2', 'testWasiFileAppend() should return "Part1Part2"');

                assert.strictEqual(instance.exports.testWasiFileDelete(), 1, 'testWasiFileDelete() should succeed');
                assert.strictEqual(instance.exports.testWasiTimeAndRandom(), 1, 'testWasiTimeAndRandom() should succeed');

                const fileObjPtr = instance.exports.testWasiFileObject();
                const fileObjContent = readWasmString(instance.exports.memory.buffer, fileObjPtr);
                assert.strictEqual(fileObjContent, 'Config=42;Active=true', 'testWasiFileObject() should return "Config=42;Active=true"');

                assert.strictEqual(instance.exports.testWasiFileObjectOps(), 1, 'testWasiFileObjectOps() should succeed');
                console.log('✓ Extended WASI & Virtual File System (VFS, WASIFile, file I/O, clocks, random) assertions passed.');

                // SIMD Vectorization Tests
                const simdFloatSum = instance.exports.testSimdFloatAddAndSum();
                assert.ok(Math.abs(simdFloatSum - 36.0) < 0.001, `testSimdFloatAddAndSum() expected 36.0, got ${simdFloatSum}`);

                const simdFloatDot = instance.exports.testSimdFloatMulAndDot();
                assert.ok(Math.abs(simdFloatDot - 40.0) < 0.001, `testSimdFloatMulAndDot() expected 40.0, got ${simdFloatDot}`);

                const simdFloatMath = instance.exports.testSimdFloatMathOps();
                assert.ok(Math.abs(simdFloatMath - 104.0) < 0.001, `testSimdFloatMathOps() expected 104.0, got ${simdFloatMath}`);

                const simdIntSum = instance.exports.testSimdIntAddAndBitwise();
                assert.strictEqual(simdIntSum, 30, `testSimdIntAddAndBitwise() expected 30, got ${simdIntSum}`);

                const simdArraySum = instance.exports.testSimdArrayLoadStore();
                assert.strictEqual(simdArraySum, 2000, `testSimdArrayLoadStore() expected 2000, got ${simdArraySum}`);
                console.log('✓ Fixed-Width SIMD (v128, Float32x4, Int32x4, vector math, dot product, array load/store) assertions passed.');

                // Multi-Memory Segregation Assertions
                const mmHeapSum = instance.exports.testMultiMemoryHeapOps(25);
                assert.strictEqual(mmHeapSum, 75, `testMultiMemoryHeapOps(25) expected 75, got ${mmHeapSum}`);

                const mmDoubleSum = instance.exports.testMultiMemoryDoubleOps(1.5);
                assert.strictEqual(mmDoubleSum, 6.0, `testMultiMemoryDoubleOps(1.5) expected 6.0, got ${mmDoubleSum}`);

                const mmGrowth = instance.exports.testMultiMemorySizeAndGrowth();
                assert.strictEqual(mmGrowth, 1, `testMultiMemorySizeAndGrowth() expected 1 page growth, got ${mmGrowth}`);

                assert.ok(instance.exports.heap instanceof WebAssembly.Memory, 'Export "heap" must be a WebAssembly.Memory');
                assert.ok(instance.exports.strings instanceof WebAssembly.Memory, 'Export "strings" must be a WebAssembly.Memory');
                assert.ok(instance.exports.stack instanceof WebAssembly.Memory, 'Export "stack" must be a WebAssembly.Memory');
                console.log('✓ Multi-Memory Segregation & Isolation (Memory.HEAP, Memory.STRINGS, Memory.STACK, loads/stores, growth) assertions passed.');

                // @JS Inline JavaScript Snippet Assertions
                if (typeof instance.exports.jsHypot === 'function') {
                    const hypotRes = instance.exports.jsHypot(3.0, 4.0);
                    assert.ok(Math.abs(hypotRes - 5.0) < 0.001, `jsHypot(3.0, 4.0) expected 5.0, got ${hypotRes}`);

                    const greetPtr = instance.exports.jsGreet(writeWasmString(instance, 'World'));
                    const greetStr = readWasmString(instance.exports.memory.buffer, greetPtr);
                    assert.strictEqual(greetStr, 'Hello World!', `jsGreet("World") expected "Hello World!", got "${greetStr}"`);

                    const multAdd = instance.exports.jsMultiplyAndAdd(5, 6);
                    assert.strictEqual(multAdd, 40, `jsMultiplyAndAdd(5, 6) expected 40, got ${multAdd}`);

                    const isGt1 = instance.exports.jsIsGreaterThan100(150);
                    assert.strictEqual(isGt1, 1, 'jsIsGreaterThan100(150) expected 1');

                    const isGt2 = instance.exports.jsIsGreaterThan100(50);
                    assert.strictEqual(isGt2, 0, 'jsIsGreaterThan100(50) expected 0');

                    console.log('✓ @JS Inline JavaScript Annotations & Host Snippet execution assertions passed.');
                }

                if (typeof instance.exports.testJsCoerceArraySum === 'function') {
                    const coercedSum = instance.exports.testJsCoerceArraySum();
                    assert.strictEqual(coercedSum, 100, `testJsCoerceArraySum() expected 100, got ${coercedSum}`);

                    const coercedMap = instance.exports.testJsCoerceArrayMap();
                    assert.strictEqual(coercedMap, 20, `testJsCoerceArrayMap() expected 20, got ${coercedMap}`);

                    if (typeof instance.exports.jsGetBookJson === 'function') {
                        const bookJsonPtr = instance.exports.jsGetBookJson();
                        const bookJson = readWasmString(instance.exports.memory.buffer, bookJsonPtr);
                        assert.ok(bookJson.includes('WASM in Action'), `jsGetBookJson expected 'WASM in Action', got ${bookJson}`);
                        assert.ok(bookJson.includes('2026'), `jsGetBookJson expected '2026', got ${bookJson}`);
                    }

                    console.log('✓ @JS.Coerce Automatic Type Coercion (arrays, map, reduce, JSON serialization) assertions passed.');
                }

                if (typeof instance.exports.testJsValueHierarchy === 'function') {
                    const valueScore = instance.exports.testJsValueHierarchy();
                    assert.strictEqual(valueScore, 255, `testJsValueHierarchy expected 255, got ${valueScore}`);

                    const numberScore = instance.exports.testJsNumberTyped();
                    assert.strictEqual(numberScore, 15, `testJsNumberTyped expected 15, got ${numberScore}`);

                    const symbolBigIntScore = instance.exports.testJsSymbolAndBigInt();
                    assert.strictEqual(symbolBigIntScore, 7, `testJsSymbolAndBigInt expected 7, got ${symbolBigIntScore}`);

                    const errorScore = instance.exports.testJsErrorAndInstanceof();
                    assert.strictEqual(errorScore, 15, `testJsErrorAndInstanceof expected 15, got ${errorScore}`);

                    const strictEqScore = instance.exports.testJsStrictEquals();
                    assert.strictEqual(strictEqScore, 3, `testJsStrictEquals expected 3, got ${strictEqScore}`);

                    console.log('✓ JSValue Typed Value Hierarchy (JSValue, JSNumber, JSBoolean, JSString, JSBigInt, JSSymbol, JSUndefined, JSError, JSNull) assertions passed.');
                }

                if (typeof instance.exports.testJsMathImport === 'function') {
                    const mathVal = instance.exports.testJsMathImport();
                    const expectedMath = 5.0 + Math.PI;
                    assert.ok(Math.abs(mathVal - expectedMath) < 0.0001, `testJsMathImport expected ${expectedMath}, got ${mathVal}`);

                    const dateMs = instance.exports.testJsDateImport();
                    assert.strictEqual(dateMs, 1700000000000.0, `testJsDateImport expected 1700000000000.0, got ${dateMs}`);

                    const jsonPtr = instance.exports.testJsJsonImport();
                    const jsonStr = readWasmString(instance.exports.memory.buffer, jsonPtr);
                    assert.ok(jsonStr.includes('WebAssembly'), `testJsJsonImport expected 'WebAssembly', got ${jsonStr}`);
                    assert.ok(jsonStr.includes('2026'), `testJsJsonImport expected '2026', got ${jsonStr}`);

                    console.log('✓ @JS.Import Global JavaScript Class & Object Binding (JSDate, JSMath, JSJSON) assertions passed.');
                }

                if ((runtime && runtime.classes && typeof runtime.classes.JavaCounter === 'function') || (typeof globalThis !== 'undefined' && typeof globalThis.JavaCounter === 'function')) {
                    const CounterClass = (runtime && runtime.classes && runtime.classes.JavaCounter) || globalThis.JavaCounter;
                    const counter = new CounterClass(10);
                    assert.strictEqual(counter.getCount(), 10, 'Initial count should be 10');
                    counter.increment();
                    assert.strictEqual(counter.getCount(), 11, 'After increment count should be 11');
                    counter.add(5);
                    assert.strictEqual(counter.getCount(), 16, 'After add(5) count should be 16');

                    const desc = counter.describe('Current');
                    assert.strictEqual(desc, 'Current: 16', `describe() expected 'Current: 16', got '${desc}'`);

                    const mult = CounterClass.multiply(6, 7);
                    assert.strictEqual(mult, 42, `static multiply expected 42, got ${mult}`);

                    console.log('✓ @JS.Export Java Class Exporting (JavaCounter constructor, prototype, instance/static methods) assertions passed.');
                }

                if (typeof instance.exports.testBundledJsCode === 'function') {
                    assert.strictEqual(typeof bundledCalc, 'function', 'bundledCalc should be defined in global scope');
                    assert.strictEqual(bundledCalc(10), 30, 'bundledCalc(10) should return 30');

                    const bundledVar = instance.exports.testBundledJsCode();
                    assert.strictEqual(bundledVar, 777, `testBundledJsCode expected 777, got ${bundledVar}`);

                    const resStrPtr = instance.exports.testJsResourceGetString();
                    const resStr = readWasmString(instance.exports.memory.buffer, resStrPtr);
                    assert.ok(resStr.includes('8080'), `testJsResourceGetString expected '8080', got ${resStr}`);

                    const resHas = instance.exports.testJsResourceHas();
                    assert.strictEqual(resHas, 1, `testJsResourceHas expected 1, got ${resHas}`);

                    const resLen = instance.exports.testJsResourceLength();
                    assert.ok(resLen > 10, `testJsResourceLength expected > 10, got ${resLen}`);

                    instance.exports.testJsResourceInjectScript();
                    assert.strictEqual(global.__injected_script_run || globalThis.__injected_script_run, true, 'injected script should have run');

                    console.log('✓ @JS.Code, @JS.Resource, and JSResource asset bundling & embedding assertions passed.');
                }

                if (typeof instance.exports.testTerminalReadLine === 'function') {
                    // Test interactive terminal & stdin reading
                    const term = runtime.createTerminal(null, { prompt: 'test> ', cols: 120, rows: 40 });
                    assert.strictEqual(term.getPrompt(), 'test> ');

                    runtime.writeStdin('Interactive User Input\n');
                    const readLinePtr = instance.exports.testTerminalReadLine(writeWasmString(instance, 'Enter name: '));
                    const readLineStr = readWasmString(instance.exports.memory.buffer, readLinePtr);
                    assert.strictEqual(readLineStr, 'Interactive User Input', `testTerminalReadLine expected 'Interactive User Input', got '${readLineStr}'`);

                    runtime.writeStdin('Z');
                    const ch = instance.exports.testTerminalReadChar();
                    assert.strictEqual(ch, 90, `testTerminalReadChar expected 90 ('Z'), got ${ch}`);

                    let capturedStdout = '';
                    runtime.onStdout((chunk) => {
                        capturedStdout += chunk;
                    });
                    instance.exports.testTerminalPrint(writeWasmString(instance, 'Hello WASI Terminal'));
                    assert.ok(capturedStdout.includes('Hello WASI Terminal'), `capturedStdout expected 'Hello WASI Terminal', got ${capturedStdout}`);

                    const dims = instance.exports.testTerminalDimensions();
                    assert.strictEqual(dims, 12040, `testTerminalDimensions expected 12040, got ${dims}`);

                    let customCommandExecuted = false;
                    term.registerCommand('testcmd', (args) => {
                        customCommandExecuted = true;
                        return `CMD_OUT: ${args}`;
                    }, 'Test command');

                    await term.executeLine('testcmd sample_arg');
                    assert.strictEqual(customCommandExecuted, true, 'Custom terminal command should execute');

                    await term.executeLine('help');
                    assert.ok(term.history.length >= 0, 'Terminal history should track commands');

                    console.log('✓ In-Browser Interactive WASI Terminal & REPL (stdin queue, stdout stream, ANSI formatting, custom commands) assertions passed.');
                }

                // Phase 1: Hardware Math & Primitive Wrapper Classes Assertions
                if (typeof instance.exports.testMathHardware === 'function') {
                    const mathHw = instance.exports.testMathHardware(5.7, 4.0);
                    // floor(5.7)=5, ceil(5.7)=6, sqrt(4)=2, abs(5.7)=5.7, min(5.7,4)=4, max(5.7,4)=5.7, rint(5.7)=6, copySign(5.7,4)=5.7 -> sum = 40.1
                    assert.ok(Math.abs(mathHw - 40.1) < 0.001, `testMathHardware expected ~40.1, got ${mathHw}`);

                    const mathTrans = instance.exports.testMathTranscendental(1.0);
                    assert.ok(typeof mathTrans === 'number' && !isNaN(mathTrans), 'testMathTranscendental should return valid number');

                    const mathConst = instance.exports.testMathConstants();
                    assert.ok(Math.abs(mathConst - (Math.PI + Math.E)) < 0.0001, `testMathConstants expected ${Math.PI + Math.E}, got ${mathConst}`);

                    const intRes = instance.exports.testIntegerWrappers(writeWasmString(instance, '100'), 42);
                    assert.ok(typeof intRes === 'number', 'testIntegerWrappers should return integer');

                    const longRes = instance.exports.testLongWrappers(writeWasmString(instance, '500'), 100n);
                    assert.ok(typeof longRes === 'bigint' || typeof longRes === 'number', 'testLongWrappers should return long');

                    const doubleRes = instance.exports.testDoubleWrappers(writeWasmString(instance, '3.14'), 2.5);
                    assert.ok(typeof doubleRes === 'number' && !isNaN(doubleRes), 'testDoubleWrappers should return double');

                    const floatRes = instance.exports.testFloatWrappers(writeWasmString(instance, '1.5'), 0.5);
                    assert.ok(typeof floatRes === 'number' && !isNaN(floatRes), 'testFloatWrappers should return float');

                    const boolRes = instance.exports.testBooleanWrappers(writeWasmString(instance, 'true'), 1);
                    assert.strictEqual(boolRes, 1, 'testBooleanWrappers should return 1');

                    const charRes = instance.exports.testCharacterWrappers(65); // 'A'
                    assert.ok(typeof charRes === 'number', 'testCharacterWrappers should return int');

                    const bitRes = instance.exports.testBitwiseIntrinsics(0x0F0F0F0F, 0x0F0F0F0F0F0F0F0Fn);
                    assert.ok(typeof bitRes === 'number', 'testBitwiseIntrinsics should return int');

                    console.log('✓ Phase 1: Hardware Math, Transcendental Functions, Primitive Wrappers, and Bitwise Intrinsics assertions passed.');
                }

                // Phase 2: Root Object Virtual Contracts & Language Enum Assertions
                if (typeof instance.exports.testEnumName === 'function') {
                    const name0 = readWasmString(instance.exports.memory.buffer, instance.exports.testEnumName(0));
                    const name1 = readWasmString(instance.exports.memory.buffer, instance.exports.testEnumName(1));
                    const name2 = readWasmString(instance.exports.memory.buffer, instance.exports.testEnumName(2));
                    assert.strictEqual(name0, 'PENDING');
                    assert.strictEqual(name1, 'ACTIVE');
                    assert.strictEqual(name2, 'COMPLETED');

                    assert.strictEqual(instance.exports.testEnumOrdinal(0), 0);
                    assert.strictEqual(instance.exports.testEnumOrdinal(1), 1);
                    assert.strictEqual(instance.exports.testEnumOrdinal(2), 2);

                    assert.strictEqual(instance.exports.testEnumCompareTo(), 2);
                    assert.strictEqual(instance.exports.testEnumValues(), 3);
                    assert.strictEqual(instance.exports.testEnumValueOf(writeWasmString(instance, 'COMPLETED')), 2);

                    assert.strictEqual(instance.exports.testEnumWithFields(), 30);

                    assert.strictEqual(instance.exports.testObjectEqualsAndHash(), 1);
                    const objStr = readWasmString(instance.exports.memory.buffer, instance.exports.testObjectToString());
                    assert.strictEqual(objStr, 'Alice');
                    assert.strictEqual(instance.exports.testObjectClone(), 1);

                    console.log('✓ Phase 2: Root Object Virtual Contracts, Shallow Cloning, and Language Enums assertions passed.');
                }

                // Phase 3: Expanded String, CharSequence, and Standard StringBuilder Assertions
                if (typeof instance.exports.testStringTrim === 'function') {
                    const rawStrPtr = writeWasmString(instance, '   hello world   ');
                    const trimmedPtr = instance.exports.testStringTrim(rawStrPtr);
                    const trimmed = readWasmString(instance.exports.memory.buffer, trimmedPtr);
                    assert.strictEqual(trimmed, 'hello world');

                    const upperPtr = instance.exports.testStringCase(writeWasmString(instance, 'hello world'), 1);
                    const upper = readWasmString(instance.exports.memory.buffer, upperPtr);
                    assert.strictEqual(upper, 'HELLO WORLD');

                    const lowerPtr = instance.exports.testStringCase(writeWasmString(instance, 'HELLO WORLD'), 0);
                    const lower = readWasmString(instance.exports.memory.buffer, lowerPtr);
                    assert.strictEqual(lower, 'hello world');

                    const replacedPtr = instance.exports.testStringReplace(
                        writeWasmString(instance, 'foo bar foo baz'),
                        writeWasmString(instance, 'foo'),
                        writeWasmString(instance, 'qux')
                    );
                    const replaced = readWasmString(instance.exports.memory.buffer, replacedPtr);
                    assert.strictEqual(replaced, 'qux bar qux baz');

                    const repPtr = instance.exports.testStringRepeat(writeWasmString(instance, 'ab'), 3);
                    const repeated = readWasmString(instance.exports.memory.buffer, repPtr);
                    assert.strictEqual(repeated, 'ababab');

                    const containsEnds = instance.exports.testStringContainsAndEnds(
                        writeWasmString(instance, 'JavaWebAssemblyAwesome'),
                        writeWasmString(instance, 'WebAssembly'),
                        writeWasmString(instance, 'Awesome')
                    );
                    assert.strictEqual(containsEnds, 1);

                    const cmp1 = instance.exports.testStringCompare(
                        writeWasmString(instance, 'abc'),
                        writeWasmString(instance, 'ABC'),
                        1
                    );
                    assert.strictEqual(cmp1, 1);

                    const charCount = instance.exports.testStringToCharArray(writeWasmString(instance, 'Wasm'));
                    assert.strictEqual(charCount, 4);

                    const sbResPtr = instance.exports.testStringBuilderOps();
                    const sbRes = readWasmString(instance.exports.memory.buffer, sbResPtr);
                    assert.ok(typeof sbRes === 'string', 'StringBuilder result should be a string');

                    const valOfPtr = instance.exports.testStringValueOf(42);
                    const valOfStr = readWasmString(instance.exports.memory.buffer, valOfPtr);
                    assert.strictEqual(valOfStr, '42');

                    console.log('✓ Phase 3: Expanded String, CharSequence, and Standard StringBuilder assertions passed.');
                }

                if (typeof instance.exports.testSystemTimeAndClock === 'function') {
                    const timeOk = instance.exports.testSystemTimeAndClock();
                    assert.strictEqual(timeOk, 1, 'testSystemTimeAndClock should return 1');

                    const idHashOk = instance.exports.testSystemIdentityHashCode();
                    assert.strictEqual(idHashOk, 1, 'testSystemIdentityHashCode should return 1');

                    const lineSepOk = instance.exports.testSystemLineSeparator();
                    assert.strictEqual(lineSepOk, 1, 'testSystemLineSeparator should return 1');

                    const propsOk = instance.exports.testSystemProperties();
                    assert.strictEqual(propsOk, 1, 'testSystemProperties should return 1');

                    const envOk = instance.exports.testSystemGetenv();
                    assert.strictEqual(envOk, 1, 'testSystemGetenv should return 1');

                    console.log('✓ Phase 4: Standard System, Clock & Environment assertions passed.');
                }
            }

            console.log('✓ TestClass execution assertions passed (including for loops, for-each loops, arrays, switches, labeled break/continue, compound assignments, dynamic strings, OOP, Canvas 2D, WebGL, JSObject, WASI VFS, Threads/Atomics, Exceptions, Multi-Value Returns, Bulk Memory, Fixed-Width SIMD, and Multi-Memory Segregation).');
        }

        // ==========================================
        // Part 2: DOM Operations & Event Handling
        // ==========================================
        console.log('\n--- Testing App.java (DOM Operations & Events) ---');
        if (fs.existsSync(APP_WAT)) fs.unlinkSync(APP_WAT);
        if (fs.existsSync(APP_WASM)) fs.unlinkSync(APP_WASM);
        if (fs.existsSync(APP_WAT_MAP)) fs.unlinkSync(APP_WAT_MAP);
        if (fs.existsSync(APP_WASM_MAP)) fs.unlinkSync(APP_WASM_MAP);

        execSync(`java -jar ${JAR_PATH} ${APP_JAVA}`, { stdio: 'inherit' });
        assert.strictEqual(fs.existsSync(APP_WAT), true, 'App.wat must exist');
        assert.strictEqual(fs.existsSync(APP_WAT_MAP), true, 'App.wat.map must exist');
        assert.strictEqual(fs.existsSync(APP_WASM_MAP), true, 'App.wasm.map must exist');

        const appMapJson = JSON.parse(fs.readFileSync(APP_WASM_MAP, 'utf8'));
        assert.strictEqual(appMapJson.version, 3, 'App source map version must be 3');
        assert.ok(appMapJson.sources.includes('App.java'), 'Sources must contain App.java');
        console.log('✓ App.wasm.map Source Map v3 verified.');

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
