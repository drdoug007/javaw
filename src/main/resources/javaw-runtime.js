/**
 * javaw-runtime.js
 * Lightweight host runtime bridge for Java WebAssembly applications interacting with the DOM.
 */

(function (global) {
    const textDecoder = new TextDecoder('utf-8');
    const textEncoder = new TextEncoder();

    class JavawRuntime {
        constructor(doc) {
            this.doc = doc || (typeof document !== 'undefined' ? document : null);
            this.handleMap = new Map();
            this.reverseMap = new Map();
            this.nextHandle = 1;
            this.instance = null;
            this.vfs = new Map();
            this.fds = new Map();
            this.nextFd = 3;
            this.inlineJsFunctions = new Map();
        }

        registerInlineJs(snippets) {
            if (!snippets) return;
            if (typeof snippets === 'string') {
                try {
                    snippets = JSON.parse(snippets);
                } catch (e) {
                    console.error('Failed to parse JS snippets JSON:', e);
                    return;
                }
            }
            if (typeof snippets === 'object' && !Array.isArray(snippets)) {
                snippets = Object.values(snippets);
            }
            if (!Array.isArray(snippets)) return;
            for (const def of snippets) {
                if (def && def.importName) {
                    const fn = this.createInlineJsFunction(def);
                    this.inlineJsFunctions.set(def.importName, fn);
                }
            }
        }

        createInlineJsFunction(def) {
            const runtime = this;
            const params = def.parameters || [];
            const jsCode = def.jsCode || '';
            const returnType = def.returnType || 'void';
            const isMethodCoerced = Boolean(def.isCoerced);
            const isStatic = Boolean(def.isStatic);

            const userParams = isStatic ? params : params.filter((p, idx) => idx > 0 || p.name !== 'this');
            const userParamNames = userParams.map(p => p.name);
            let userFn;
            try {
                let body = jsCode.trim();
                if (!body.includes('return') && !body.includes(';') && !body.includes('\n') && returnType !== 'void') {
                    body = 'return (' + body + ');';
                }
                userFn = new Function(...userParamNames, 'runtime', body);
            } catch (e) {
                console.error('Error compiling inline JS snippet ' + def.importName + ':', e);
                userFn = () => {};
            }

            return function (...args) {
                let thisObj = null;
                const userArgs = [];
                let argIdx = 0;

                if (!isStatic) {
                    const handle = args[argIdx++];
                    thisObj = runtime.getElement(handle);
                }

                for (const p of userParams) {
                    const pType = p.javaType;
                    const isParamCoerced = isMethodCoerced || Boolean(p.coerced);

                    if (pType === 'String' || pType === 'java.lang.String') {
                        const ptr = args[argIdx++];
                        const len = args[argIdx++];
                        userArgs.push(runtime.readString(ptr, len));
                    } else if (pType === 'boolean') {
                        const val = args[argIdx++];
                        userArgs.push(Boolean(val));
                    } else if (pType === 'int[]' || pType === 'Integer[]') {
                        const ptr = args[argIdx++];
                        if (isParamCoerced) {
                            userArgs.push(runtime.readIntArray(ptr));
                        } else {
                            userArgs.push(ptr);
                        }
                    } else if (pType === 'double[]' || pType === 'Double[]' || pType === 'float[]' || pType === 'Float[]') {
                        const ptr = args[argIdx++];
                        if (isParamCoerced) {
                            userArgs.push(runtime.readDoubleArray(ptr));
                        } else {
                            userArgs.push(ptr);
                        }
                    } else if (pType === 'String[]' || pType === 'java.lang.String[]') {
                        const ptr = args[argIdx++];
                        if (isParamCoerced) {
                            userArgs.push(runtime.readStringArray(ptr));
                        } else {
                            userArgs.push(ptr);
                        }
                    } else if (pType === 'JSObject' || pType === 'Element' || pType === 'JSValue' || (pType && (pType.endsWith('.JSObject') || pType.endsWith('.Element') || pType.endsWith('.JSValue') || pType.startsWith('JS')))) {
                        const handle = args[argIdx++];
                        userArgs.push(runtime.getElement(handle));
                    } else if (pType && pType.endsWith('[]')) {
                        const ptr = args[argIdx++];
                        userArgs.push(ptr);
                    } else {
                        const val = args[argIdx++];
                        userArgs.push(val);
                    }
                }

                let result;
                try {
                    result = userFn.apply(thisObj, [...userArgs, runtime]);
                } catch (err) {
                    console.error('Error executing inline JS snippet ' + def.importName + ':', err);
                    throw err;
                }

                if (returnType === 'void') {
                    return;
                } else if (returnType === 'String' || returnType === 'java.lang.String') {
                    if (result === null || result === undefined) {
                        return runtime.writeString('');
                    }
                    if (typeof result === 'object') {
                        return runtime.writeString(JSON.stringify(result));
                    }
                    return runtime.writeString(String(result));
                } else if (returnType === 'boolean') {
                    return result ? 1 : 0;
                } else if (returnType === 'int' || returnType === 'short' || returnType === 'byte' || returnType === 'char') {
                    return Math.trunc(Number(result)) | 0;
                } else if (returnType === 'double' || returnType === 'float') {
                    return Number(result);
                } else if (returnType === 'long') {
                    return typeof BigInt !== 'undefined' ? BigInt(result) : Math.trunc(Number(result));
                } else if (returnType === 'int[]' || returnType === 'Integer[]') {
                    if (Array.isArray(result) || (result && typeof result[Symbol.iterator] === 'function')) {
                        return runtime.writeIntArray(Array.from(result));
                    }
                    return typeof result === 'number' ? result : 0;
                } else if (returnType === 'double[]' || returnType === 'Double[]' || returnType === 'float[]') {
                    if (Array.isArray(result) || (result && typeof result[Symbol.iterator] === 'function')) {
                        return runtime.writeDoubleArray(Array.from(result));
                    }
                    return typeof result === 'number' ? result : 0;
                } else if (returnType === 'String[]' || returnType === 'java.lang.String[]') {
                    if (Array.isArray(result) || (result && typeof result[Symbol.iterator] === 'function')) {
                        return runtime.writeStringArray(Array.from(result));
                    }
                    return typeof result === 'number' ? result : 0;
                } else if (returnType === 'JSObject' || returnType === 'Element' || returnType === 'JSValue' || (returnType && (returnType.endsWith('.JSObject') || returnType.endsWith('.Element') || returnType.endsWith('.JSValue') || returnType.startsWith('JS')))) {
                    return runtime.getOrCreateHandle(result);
                } else {
                    return typeof result === 'number' ? result : runtime.getOrCreateHandle(result);
                }
            };
        }

        readIntArray(ptr) {
            if (!ptr || ptr <= 0) return [];
            const memory = this.getMemory();
            if (!memory || !memory.buffer) return [];
            const view = new DataView(memory.buffer);
            const len = view.getInt32(ptr, true);
            const arr = [];
            for (let i = 0; i < len; i++) {
                arr.push(view.getInt32(ptr + 4 + i * 4, true));
            }
            return arr;
        }

        readDoubleArray(ptr) {
            if (!ptr || ptr <= 0) return [];
            const memory = this.getMemory();
            if (!memory || !memory.buffer) return [];
            const view = new DataView(memory.buffer);
            const len = view.getInt32(ptr, true);
            const arr = [];
            for (let i = 0; i < len; i++) {
                arr.push(view.getFloat64(ptr + 8 + i * 8, true));
            }
            return arr;
        }

        readStringArray(ptr) {
            if (!ptr || ptr <= 0) return [];
            const memory = this.getMemory();
            if (!memory || !memory.buffer) return [];
            const view = new DataView(memory.buffer);
            const len = view.getInt32(ptr, true);
            const arr = [];
            for (let i = 0; i < len; i++) {
                const strPtr = view.getInt32(ptr + 4 + i * 4, true);
                arr.push(this.readWasmStringFromPtr(strPtr));
            }
            return arr;
        }

        readWasmStringFromPtr(ptr) {
            if (!ptr || ptr <= 0) return '';
            let memory = this.getMemory('strings');
            if (!memory || !memory.buffer) memory = this.getMemory();
            if (!memory || !memory.buffer) return '';
            const u8 = new Uint8Array(memory.buffer, ptr);
            let len = 0;
            while (ptr + len < memory.buffer.byteLength && u8[len] !== 0) len++;
            return textDecoder.decode(u8.subarray(0, len));
        }

        writeIntArray(arr) {
            if (!arr) return 0;
            const len = arr.length;
            const byteSize = 4 + len * 4;
            let ptr = 0;
            if (this.instance && typeof this.instance.exports.__heap_alloc === 'function') {
                ptr = this.instance.exports.__heap_alloc(byteSize);
            }
            if (ptr > 0) {
                const view = new DataView(this.getMemory().buffer);
                view.setInt32(ptr, len, true);
                for (let i = 0; i < len; i++) {
                    view.setInt32(ptr + 4 + i * 4, Math.trunc(Number(arr[i])) | 0, true);
                }
            }
            return ptr;
        }

        writeDoubleArray(arr) {
            if (!arr) return 0;
            const len = arr.length;
            const byteSize = 8 + len * 8;
            let ptr = 0;
            if (this.instance && typeof this.instance.exports.__heap_alloc === 'function') {
                ptr = this.instance.exports.__heap_alloc(byteSize);
            }
            if (ptr > 0) {
                const view = new DataView(this.getMemory().buffer);
                view.setInt32(ptr, len, true);
                for (let i = 0; i < len; i++) {
                    view.setFloat64(ptr + 8 + i * 8, Number(arr[i]), true);
                }
            }
            return ptr;
        }

        writeStringArray(arr) {
            if (!arr) return 0;
            const len = arr.length;
            const byteSize = 4 + len * 4;
            let ptr = 0;
            if (this.instance && typeof this.instance.exports.__heap_alloc === 'function') {
                ptr = this.instance.exports.__heap_alloc(byteSize);
            }
            if (ptr > 0) {
                const view = new DataView(this.getMemory().buffer);
                view.setInt32(ptr, len, true);
                for (let i = 0; i < len; i++) {
                    const strPtr = this.writeString(arr[i] != null ? String(arr[i]) : '');
                    view.setInt32(ptr + 4 + i * 4, strPtr, true);
                }
            }
            return ptr;
        }

        loadCustomSections(module) {
            if (!module || typeof WebAssembly.Module.customSections !== 'function') return;
            try {
                const sections = WebAssembly.Module.customSections(module, 'javaw_js_snippets');
                if (sections && sections.length > 0) {
                    for (const sec of sections) {
                        const uint8 = new Uint8Array(sec);
                        let jsonStr = textDecoder.decode(uint8);
                        const firstChar = jsonStr.indexOf('[');
                        if (firstChar >= 0) {
                            jsonStr = jsonStr.substring(firstChar);
                        }
                        this.registerInlineJs(jsonStr);
                    }
                }
            } catch (e) {
                console.warn('Error reading custom section javaw_js_snippets:', e);
            }
        }

        normalizePath(p) {
            if (!p) return '/';
            p = p.replace(/\\/g, '/');
            if (!p.startsWith('/')) p = '/' + p;
            const segments = p.split('/').filter(Boolean);
            const stack = [];
            for (const seg of segments) {
                if (seg === '.') continue;
                if (seg === '..') {
                    if (stack.length > 0) stack.pop();
                } else {
                    stack.push(seg);
                }
            }
            return '/' + stack.join('/');
        }

        mount(path, content) {
            const norm = this.normalizePath(path);
            let bytes;
            if (typeof content === 'string') {
                bytes = textEncoder.encode(content);
            } else if (content instanceof Uint8Array) {
                bytes = new Uint8Array(content);
            } else if (content instanceof ArrayBuffer) {
                bytes = new Uint8Array(content);
            } else {
                bytes = new Uint8Array(0);
            }
            this.vfs.set(norm, { content: bytes, isDirectory: false, mtime: Date.now() });
        }

        writeFile(path, content) {
            this.mount(path, content);
        }

        readFile(path) {
            const norm = this.normalizePath(path);
            const entry = this.vfs.get(norm);
            if (!entry) return null;
            return textDecoder.decode(entry.content);
        }

        readFileBytes(path) {
            const norm = this.normalizePath(path);
            const entry = this.vfs.get(norm);
            return entry ? entry.content : null;
        }

        exists(path) {
            const norm = this.normalizePath(path);
            return this.vfs.has(norm);
        }

        unlink(path) {
            const norm = this.normalizePath(path);
            return this.vfs.delete(norm);
        }

        mkdir(path) {
            const norm = this.normalizePath(path);
            this.vfs.set(norm, { content: new Uint8Array(0), isDirectory: true, mtime: Date.now() });
            return true;
        }

        listFiles() {
            return Array.from(this.vfs.keys());
        }

        setInstance(instance) {
            this.instance = instance;
        }

        getMemory(name) {
            if (!this.instance || !this.instance.exports) return null;
            if (name && this.instance.exports[name]) return this.instance.exports[name];
            return this.instance.exports.memory || this.instance.exports.strings || this.instance.exports.heap || null;
        }

        getHeapMemory() {
            return this.getMemory('heap');
        }

        getStringsMemory() {
            return this.getMemory('strings');
        }

        getStackMemory() {
            return this.getMemory('stack');
        }

        getExportedFunction(name) {
            if (!this.instance || !this.instance.exports) return null;
            if (typeof this.instance.exports[name] === 'function') {
                return this.instance.exports[name];
            }
            for (const key of Object.keys(this.instance.exports)) {
                if (key.endsWith('_' + name) && typeof this.instance.exports[key] === 'function') {
                    return this.instance.exports[key];
                }
            }
            return null;
        }

        readString(ptr, len, memName) {
            const memory = this.getMemory(memName || 'strings');
            if (!memory || len === 0) return '';
            const bytes = new Uint8Array(memory.buffer, ptr, len);
            return textDecoder.decode(bytes);
        }

        writeString(str) {
            if (str === null || str === undefined) str = '';
            const bytes = textEncoder.encode(str);
            const len = bytes.length;
            let ptr = 0;
            if (this.instance && typeof this.instance.exports.__str_alloc === 'function') {
                ptr = this.instance.exports.__str_alloc(len);
            } else if (this.instance && typeof this.instance.exports.__heap_alloc === 'function') {
                ptr = this.instance.exports.__heap_alloc(len + 1);
            }
            if (ptr > 0) {
                const memView = new Uint8Array(this.getMemory().buffer, ptr, len + 1);
                memView.set(bytes);
                memView[len] = 0;
            }
            return ptr;
        }

        writeHandleArray(elements) {
            const count = elements.length;
            const byteSize = 4 + count * 4;
            let ptr = 0;
            if (this.instance && typeof this.instance.exports.__heap_alloc === 'function') {
                ptr = this.instance.exports.__heap_alloc(byteSize);
            }
            if (ptr > 0) {
                const view = new DataView(this.getMemory().buffer);
                view.setInt32(ptr, count, true);
                for (let i = 0; i < count; i++) {
                    const handle = this.getOrCreateHandle(elements[i]);
                    view.setInt32(ptr + 4 + i * 4, handle, true);
                }
            }
            return ptr;
        }

        getOrCreateHandle(element) {
            if (element === null) return 0;
            if (element === undefined) {
                if (this.reverseMap.has(undefined)) {
                    return this.reverseMap.get(undefined);
                }
                const handle = this.nextHandle++;
                this.handleMap.set(handle, undefined);
                this.reverseMap.set(undefined, handle);
                return handle;
            }
            if (this.reverseMap.has(element)) {
                return this.reverseMap.get(element);
            }
            const handle = this.nextHandle++;
            this.handleMap.set(handle, element);
            this.reverseMap.set(element, handle);
            return handle;
        }

        getElement(handle) {
            if (!handle || handle <= 0) return null;
            if (this.handleMap.has(handle)) {
                return this.handleMap.get(handle);
            }
            return null;
        }

        // Host import implementations
        getElementById(ptr, len) {
            const id = this.readString(ptr, len);
            const el = this.doc ? this.doc.getElementById(id) : null;
            return this.getOrCreateHandle(el);
        }

        createElement(ptr, len) {
            const tag = this.readString(ptr, len);
            const el = this.doc ? this.doc.createElement(tag) : null;
            return this.getOrCreateHandle(el);
        }

        getBody() {
            const el = this.doc ? (this.doc.body || this.doc.documentElement) : null;
            return this.getOrCreateHandle(el);
        }

        querySelector(ptr, len) {
            const sel = this.readString(ptr, len);
            const el = this.doc ? this.doc.querySelector(sel) : null;
            return this.getOrCreateHandle(el);
        }

        querySelectorAll(ptr, len) {
            const sel = this.readString(ptr, len);
            const elements = this.doc ? Array.from(this.doc.querySelectorAll(sel)) : [];
            return this.writeHandleArray(elements);
        }

        setInnerText(handle, ptr, len) {
            const text = this.readString(ptr, len);
            const el = this.getElement(handle);
            if (el) {
                if ('innerText' in el) {
                    el.innerText = text;
                } else {
                    el.textContent = text;
                }
            }
        }

        getInnerText(handle) {
            const el = this.getElement(handle);
            const text = el ? (el.innerText !== undefined ? el.innerText : el.textContent) : '';
            return this.writeString(text || '');
        }

        setAttribute(handle, namePtr, nameLen, valPtr, valLen) {
            const name = this.readString(namePtr, nameLen);
            const value = this.readString(valPtr, valLen);
            const el = this.getElement(handle);
            if (el && typeof el.setAttribute === 'function') {
                el.setAttribute(name, value);
            }
        }

        getAttribute(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            const val = el && typeof el.getAttribute === 'function' ? el.getAttribute(name) : '';
            return this.writeString(val || '');
        }

        appendChild(parentHandle, childHandle) {
            const parent = this.getElement(parentHandle);
            const child = this.getElement(childHandle);
            if (parent && child && typeof parent.appendChild === 'function') {
                parent.appendChild(child);
            }
        }

        removeChild(parentHandle, childHandle) {
            const parent = this.getElement(parentHandle);
            const child = this.getElement(childHandle);
            if (parent && child && typeof parent.removeChild === 'function') {
                parent.removeChild(child);
            }
        }

        remove(handle) {
            const el = this.getElement(handle);
            if (el && typeof el.remove === 'function') {
                el.remove();
            }
        }

        getParent(handle) {
            const el = this.getElement(handle);
            const parent = el ? (el.parentElement || el.parentNode) : null;
            return this.getOrCreateHandle(parent);
        }

        getValue(handle) {
            const el = this.getElement(handle);
            const val = (el && 'value' in el) ? el.value : (el ? (el.getAttribute && el.getAttribute('value')) || '' : '');
            return this.writeString(val || '');
        }

        setValue(handle, ptr, len) {
            const val = this.readString(ptr, len);
            const el = this.getElement(handle);
            if (el) {
                el.value = val;
            }
        }

        isChecked(handle) {
            const el = this.getElement(handle);
            return (el && el.checked) ? 1 : 0;
        }

        setChecked(handle, checked) {
            const el = this.getElement(handle);
            if (el) {
                el.checked = (checked !== 0);
            }
        }

        setStyle(handle, namePtr, nameLen, valPtr, valLen) {
            const name = this.readString(namePtr, nameLen);
            const val = this.readString(valPtr, valLen);
            const el = this.getElement(handle);
            if (el && el.style) {
                el.style[name] = val;
            }
        }

        getStyle(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            const val = (el && el.style) ? (el.style[name] || '') : '';
            return this.writeString(val || '');
        }

        addClass(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            if (el && el.classList && typeof el.classList.add === 'function') {
                el.classList.add(name);
            }
        }

        removeClass(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            if (el && el.classList && typeof el.classList.remove === 'function') {
                el.classList.remove(name);
            }
        }

        hasClass(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            return (el && el.classList && typeof el.classList.contains === 'function' && el.classList.contains(name)) ? 1 : 0;
        }

        toggleClass(handle, namePtr, nameLen) {
            const name = this.readString(namePtr, nameLen);
            const el = this.getElement(handle);
            if (el && el.classList && typeof el.classList.toggle === 'function') {
                el.classList.toggle(name);
            }
        }

        addEventListener(handle, eventPtr, eventLen, handlerPtr, handlerLen) {
            const eventName = this.readString(eventPtr, eventLen);
            const handlerName = this.readString(handlerPtr, handlerLen);
            const el = this.getElement(handle);
            if (el && typeof el.addEventListener === 'function') {
                el.addEventListener(eventName, (event) => {
                    const fn = this.getExportedFunction(handlerName);
                    if (fn) {
                        fn();
                    }
                });
            }
        }

        setTimeout(handlerPtr, handlerLen, delayMs) {
            const handlerName = this.readString(handlerPtr, handlerLen);
            const timer = setTimeout(() => {
                const fn = this.getExportedFunction(handlerName);
                if (fn) {
                    fn();
                }
            }, delayMs);
            return typeof timer === 'number' ? timer : (timer && timer[Symbol.toPrimitive] ? Number(timer) : (this.nextHandle++));
        }

        clearTimeout(timerId) {
            clearTimeout(timerId);
        }

        setInterval(handlerPtr, handlerLen, delayMs) {
            const handlerName = this.readString(handlerPtr, handlerLen);
            const timer = setInterval(() => {
                const fn = this.getExportedFunction(handlerName);
                if (fn) {
                    fn();
                }
            }, delayMs);
            return typeof timer === 'number' ? timer : (timer && timer[Symbol.toPrimitive] ? Number(timer) : (this.nextHandle++));
        }

        clearInterval(intervalId) {
            clearInterval(intervalId);
        }

        requestAnimationFrame(handlerPtr, handlerLen) {
            const handlerName = this.readString(handlerPtr, handlerLen);
            const rafFn = (typeof requestAnimationFrame !== 'undefined') ? requestAnimationFrame :
                          (fn => setTimeout(() => fn(Date.now()), 16));
            const frameId = rafFn((timestamp) => {
                const fn = this.getExportedFunction(handlerName);
                if (fn) {
                    if (fn.length === 0) {
                        fn();
                    } else {
                        fn(timestamp);
                    }
                }
            });
            return typeof frameId === 'number' ? frameId : (frameId && frameId[Symbol.toPrimitive] ? Number(frameId) : 1);
        }

        cancelAnimationFrame(frameId) {
            const cancelRafFn = (typeof cancelAnimationFrame !== 'undefined') ? cancelAnimationFrame : clearTimeout;
            cancelRafFn(frameId);
        }

        fetch(urlPtr, urlLen, handlerPtr, handlerLen) {
            const url = this.readString(urlPtr, urlLen);
            const handlerName = this.readString(handlerPtr, handlerLen);
            const fetchFn = (typeof fetch !== 'undefined') ? fetch : (typeof globalThis !== 'undefined' && globalThis.fetch);
            if (!fetchFn) return;
            fetchFn(url)
                .then(async (res) => {
                    const text = await res.text();
                    const fn = this.getExportedFunction(handlerName);
                    if (fn) {
                        const ptr = this.writeString(text);
                        if (fn.length === 2) {
                            fn(res.status, ptr);
                        } else if (fn.length === 1) {
                            fn(ptr);
                        } else {
                            fn();
                        }
                    }
                })
                .catch((err) => {
                    const errFn = this.getExportedFunction(handlerName + "_error");
                    if (errFn) {
                        const ptr = this.writeString(err ? (err.message || String(err)) : "Error");
                        errFn(ptr);
                    }
                });
        }

        alert(msgPtr, msgLen) {
            const msg = this.readString(msgPtr, msgLen);
            if (typeof alert !== 'undefined') {
                alert(msg);
            } else if (typeof console !== 'undefined') {
                console.log("[ALERT]", msg);
            }
        }

        confirm(msgPtr, msgLen) {
            const msg = this.readString(msgPtr, msgLen);
            if (typeof confirm !== 'undefined') {
                return confirm(msg) ? 1 : 0;
            }
            return 1;
        }

        prompt(msgPtr, msgLen) {
            const msg = this.readString(msgPtr, msgLen);
            if (typeof prompt !== 'undefined') {
                const res = prompt(msg);
                return res !== null ? this.writeString(res) : 0;
            }
            return 0;
        }

        getCanvasContext2D(idPtr, idLen) {
            const id = this.readString(idPtr, idLen);
            const el = this.doc ? this.doc.getElementById(id) : null;
            const ctx = el && typeof el.getContext === 'function' ? el.getContext('2d') : null;
            return this.getOrCreateHandle(ctx);
        }

        getContext2D(handle) {
            const el = this.getElement(handle);
            const ctx = el && typeof el.getContext === 'function' ? el.getContext('2d') : null;
            return this.getOrCreateHandle(ctx);
        }

        getContext(handle, typePtr, typeLen) {
            const type = this.readString(typePtr, typeLen);
            const el = this.getElement(handle);
            const ctx = el && typeof el.getContext === 'function' ? el.getContext(type) : null;
            return this.getOrCreateHandle(ctx);
        }

        getWebGLContext(handle) {
            const el = this.getElement(handle);
            const ctx = el && typeof el.getContext === 'function' ? (el.getContext('webgl') || el.getContext('experimental-webgl')) : null;
            return this.getOrCreateHandle(ctx);
        }

        getWidth(handle) {
            const el = this.getElement(handle);
            return (el && el.width !== undefined) ? (el.width | 0) : 0;
        }

        setWidth(handle, width) {
            const el = this.getElement(handle);
            if (el) el.width = width;
        }

        getHeight(handle) {
            const el = this.getElement(handle);
            return (el && el.height !== undefined) ? (el.height | 0) : 0;
        }

        setHeight(handle, height) {
            const el = this.getElement(handle);
            if (el) el.height = height;
        }

        fillRect(handle, x, y, w, h) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.fillRect === 'function') ctx.fillRect(x, y, w, h);
        }

        strokeRect(handle, x, y, w, h) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.strokeRect === 'function') ctx.strokeRect(x, y, w, h);
        }

        clearRect(handle, x, y, w, h) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.clearRect === 'function') ctx.clearRect(x, y, w, h);
        }

        beginPath(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.beginPath === 'function') ctx.beginPath();
        }

        closePath(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.closePath === 'function') ctx.closePath();
        }

        moveTo(handle, x, y) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.moveTo === 'function') ctx.moveTo(x, y);
        }

        lineTo(handle, x, y) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.lineTo === 'function') ctx.lineTo(x, y);
        }

        arc(handle, x, y, radius, startAngle, endAngle, counterclockwise) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.arc === 'function') ctx.arc(x, y, radius, startAngle, endAngle, counterclockwise !== 0);
        }

        rect(handle, x, y, w, h) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.rect === 'function') ctx.rect(x, y, w, h);
        }

        ellipse(handle, x, y, radiusX, radiusY, rotation, startAngle, endAngle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.ellipse === 'function') ctx.ellipse(x, y, radiusX, radiusY, rotation, startAngle, endAngle);
        }

        fill(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.fill === 'function') ctx.fill();
        }

        stroke(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.stroke === 'function') ctx.stroke();
        }

        fillText(handle, textPtr, textLen, x, y) {
            const text = this.readString(textPtr, textLen);
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.fillText === 'function') ctx.fillText(text, x, y);
        }

        strokeText(handle, textPtr, textLen, x, y) {
            const text = this.readString(textPtr, textLen);
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.strokeText === 'function') ctx.strokeText(text, x, y);
        }

        setFillStyle(handle, stylePtr, styleLen) {
            const style = this.readString(stylePtr, styleLen);
            const ctx = this.getElement(handle);
            if (ctx) ctx.fillStyle = style;
        }

        getFillStyle(handle) {
            const ctx = this.getElement(handle);
            const style = (ctx && ctx.fillStyle) ? String(ctx.fillStyle) : '';
            return this.writeString(style);
        }

        setStrokeStyle(handle, stylePtr, styleLen) {
            const style = this.readString(stylePtr, styleLen);
            const ctx = this.getElement(handle);
            if (ctx) ctx.strokeStyle = style;
        }

        getStrokeStyle(handle) {
            const ctx = this.getElement(handle);
            const style = (ctx && ctx.strokeStyle) ? String(ctx.strokeStyle) : '';
            return this.writeString(style);
        }

        setLineWidth(handle, width) {
            const ctx = this.getElement(handle);
            if (ctx) ctx.lineWidth = width;
        }

        getLineWidth(handle) {
            const ctx = this.getElement(handle);
            return (ctx && ctx.lineWidth !== undefined) ? ctx.lineWidth : 1.0;
        }

        setFont(handle, fontPtr, fontLen) {
            const font = this.readString(fontPtr, fontLen);
            const ctx = this.getElement(handle);
            if (ctx) ctx.font = font;
        }

        getFont(handle) {
            const ctx = this.getElement(handle);
            const font = (ctx && ctx.font) ? String(ctx.font) : '';
            return this.writeString(font);
        }

        setTextAlign(handle, alignPtr, alignLen) {
            const align = this.readString(alignPtr, alignLen);
            const ctx = this.getElement(handle);
            if (ctx) ctx.textAlign = align;
        }

        getTextAlign(handle) {
            const ctx = this.getElement(handle);
            const align = (ctx && ctx.textAlign) ? String(ctx.textAlign) : '';
            return this.writeString(align);
        }

        drawImage(handle, imgHandle, dx, dy, dw, dh) {
            const ctx = this.getElement(handle);
            const img = this.getElement(imgHandle);
            if (ctx && img && typeof ctx.drawImage === 'function') {
                if (dw !== undefined && dh !== undefined && (dw > 0 || dh > 0)) {
                    ctx.drawImage(img, dx, dy, dw, dh);
                } else {
                    ctx.drawImage(img, dx, dy);
                }
            }
        }

        drawImageFull(handle, imgHandle, sx, sy, sw, sh, dx, dy, dw, dh) {
            const ctx = this.getElement(handle);
            const img = this.getElement(imgHandle);
            if (ctx && img && typeof ctx.drawImage === 'function') {
                ctx.drawImage(img, sx, sy, sw, sh, dx, dy, dw, dh);
            }
        }

        save(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.save === 'function') ctx.save();
        }

        restore(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.restore === 'function') ctx.restore();
        }

        translate(handle, x, y) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.translate === 'function') ctx.translate(x, y);
        }

        rotate(handle, angle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.rotate === 'function') ctx.rotate(angle);
        }

        scale(handle, sx, sy) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.scale === 'function') ctx.scale(sx, sy);
        }

        resetTransform(handle) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.resetTransform === 'function') ctx.resetTransform();
        }

        setGlobalAlpha(handle, alpha) {
            const ctx = this.getElement(handle);
            if (ctx) ctx.globalAlpha = alpha;
        }

        getGlobalAlpha(handle) {
            const ctx = this.getElement(handle);
            return (ctx && ctx.globalAlpha !== undefined) ? ctx.globalAlpha : 1.0;
        }

        webgl_clearColor(handle, r, g, b, a) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.clearColor === 'function') ctx.clearColor(r, g, b, a);
        }

        webgl_clear(handle, mask) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.clear === 'function') ctx.clear(mask);
        }

        webgl_viewport(handle, x, y, width, height) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.viewport === 'function') ctx.viewport(x, y, width, height);
        }

        webgl_enable(handle, cap) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.enable === 'function') ctx.enable(cap);
        }

        webgl_disable(handle, cap) {
            const ctx = this.getElement(handle);
            if (ctx && typeof ctx.disable === 'function') ctx.disable(cap);
        }

        // Generic JS dynamic interop host methods
        js_global() {
            const g = (typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : global));
            return this.getOrCreateHandle(g);
        }

        js_eval(ptr, len) {
            const code = this.readString(ptr, len);
            let res = null;
            try {
                res = (0, eval)(code);
            } catch (e) {
                res = null;
            }
            return this.getOrCreateHandle(res);
        }

        js_createObject() {
            return this.getOrCreateHandle({});
        }

        js_createArray() {
            return this.getOrCreateHandle([]);
        }

        js_get(handle, propPtr, propLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            const res = obj ? obj[prop] : null;
            return this.getOrCreateHandle(res);
        }

        js_getString(handle, propPtr, propLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            const val = (obj && obj[prop] !== undefined && obj[prop] !== null) ? String(obj[prop]) : '';
            return this.writeString(val);
        }

        js_getInt(handle, propPtr, propLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            const val = obj ? Number(obj[prop]) || 0 : 0;
            return val | 0;
        }

        js_getDouble(handle, propPtr, propLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            return obj ? (Number(obj[prop]) || 0.0) : 0.0;
        }

        js_getBoolean(handle, propPtr, propLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            return (obj && obj[prop]) ? 1 : 0;
        }

        js_setString(handle, propPtr, propLen, valPtr, valLen) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            const val = this.readString(valPtr, valLen);
            if (obj) obj[prop] = val;
        }

        js_setInt(handle, propPtr, propLen, val) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            if (obj) obj[prop] = val;
        }

        js_setDouble(handle, propPtr, propLen, val) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            if (obj) obj[prop] = val;
        }

        js_setBoolean(handle, propPtr, propLen, val) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            if (obj) obj[prop] = (val !== 0);
        }

        js_setObject(handle, propPtr, propLen, valHandle) {
            const obj = this.getElement(handle);
            const prop = this.readString(propPtr, propLen);
            const val = this.getElement(valHandle);
            if (obj) obj[prop] = val;
        }

        js_call0(handle, mPtr, mLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName]();
            }
            return this.getOrCreateHandle(res);
        }

        js_call1_str(handle, mPtr, mLen, aPtr, aLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg = this.readString(aPtr, aLen);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg);
            }
            return this.getOrCreateHandle(res);
        }

        js_call1_int(handle, mPtr, mLen, arg) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg);
            }
            return this.getOrCreateHandle(res);
        }

        js_call1_double(handle, mPtr, mLen, arg) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg);
            }
            return this.getOrCreateHandle(res);
        }

        js_call1_bool(handle, mPtr, mLen, arg) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg !== 0);
            }
            return this.getOrCreateHandle(res);
        }

        js_call1_obj(handle, mPtr, mLen, aHandle) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg = this.getElement(aHandle);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg);
            }
            return this.getOrCreateHandle(res);
        }

        js_call2_str_str(handle, mPtr, mLen, a1Ptr, a1Len, a2Ptr, a2Len) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg1 = this.readString(a1Ptr, a1Len);
            const arg2 = this.readString(a2Ptr, a2Len);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg1, arg2);
            }
            return this.getOrCreateHandle(res);
        }

        js_call2_str_int(handle, mPtr, mLen, a1Ptr, a1Len, arg2) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg1 = this.readString(a1Ptr, a1Len);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg1, arg2);
            }
            return this.getOrCreateHandle(res);
        }

        js_call2_str_obj(handle, mPtr, mLen, a1Ptr, a1Len, a2Handle) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg1 = this.readString(a1Ptr, a1Len);
            const arg2 = this.getElement(a2Handle);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg1, arg2);
            }
            return this.getOrCreateHandle(res);
        }

        js_call2_obj_obj(handle, mPtr, mLen, a1Handle, a2Handle) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg1 = this.getElement(a1Handle);
            const arg2 = this.getElement(a2Handle);
            let res = null;
            if (obj && typeof obj[mName] === 'function') {
                res = obj[mName](arg1, arg2);
            }
            return this.getOrCreateHandle(res);
        }

        js_callString0(handle, mPtr, mLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = '';
            if (obj && typeof obj[mName] === 'function') {
                const val = obj[mName]();
                res = (val !== undefined && val !== null) ? String(val) : '';
            }
            return this.writeString(res);
        }

        js_callString1_str(handle, mPtr, mLen, aPtr, aLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            const arg = this.readString(aPtr, aLen);
            let res = '';
            if (obj && typeof obj[mName] === 'function') {
                const val = obj[mName](arg);
                res = (val !== undefined && val !== null) ? String(val) : '';
            }
            return this.writeString(res);
        }

        js_callInt0(handle, mPtr, mLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = 0;
            if (obj && typeof obj[mName] === 'function') {
                res = Number(obj[mName]()) || 0;
            }
            return res | 0;
        }

        js_callInt1_int(handle, mPtr, mLen, arg) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            let res = 0;
            if (obj && typeof obj[mName] === 'function') {
                res = Number(obj[mName](arg)) || 0;
            }
            return res | 0;
        }

        js_callDouble0(handle, mPtr, mLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            return (obj && typeof obj[mName] === 'function') ? (Number(obj[mName]()) || 0.0) : 0.0;
        }

        js_callBoolean0(handle, mPtr, mLen) {
            const obj = this.getElement(handle);
            const mName = this.readString(mPtr, mLen);
            return (obj && typeof obj[mName] === 'function' && obj[mName]()) ? 1 : 0;
        }

        js_asString(handle) {
            const obj = this.getElement(handle);
            return this.writeString((obj !== null && obj !== undefined) ? String(obj) : '');
        }

        js_asInt(handle) {
            const obj = this.getElement(handle);
            return (Number(obj) || 0) | 0;
        }

        js_asDouble(handle) {
            const obj = this.getElement(handle);
            return Number(obj) || 0.0;
        }

        js_asBoolean(handle) {
            const obj = this.getElement(handle);
            return obj ? 1 : 0;
        }

        js_isArray(handle) {
            const obj = this.getElement(handle);
            return Array.isArray(obj) ? 1 : 0;
        }

        js_length(handle) {
            const obj = this.getElement(handle);
            return (obj && typeof obj.length === 'number') ? obj.length : 0;
        }

        js_getIndex(handle, index) {
            const obj = this.getElement(handle);
            const item = (obj && obj[index] !== undefined) ? obj[index] : null;
            return this.getOrCreateHandle(item);
        }

        js_setIndexObj(handle, index, valHandle) {
            const obj = this.getElement(handle);
            const item = this.getElement(valHandle);
            if (obj) obj[index] = item;
        }

        js_setIndexStr(handle, index, ptr, len) {
            const obj = this.getElement(handle);
            const str = this.readString(ptr, len);
            if (obj) obj[index] = str;
        }

        js_setIndexInt(handle, index, val) {
            const obj = this.getElement(handle);
            if (obj) obj[index] = val;
        }

        js_setIndexDouble(handle, index, val) {
            const obj = this.getElement(handle);
            if (obj) obj[index] = val;
        }

        js_setIndexBoolean(handle, index, val) {
            const obj = this.getElement(handle);
            if (obj) obj[index] = (val !== 0);
        }

        js_pushObj(handle, valHandle) {
            const obj = this.getElement(handle);
            const item = this.getElement(valHandle);
            if (obj && typeof obj.push === 'function') obj.push(item);
        }

        js_pushStr(handle, ptr, len) {
            const obj = this.getElement(handle);
            const str = this.readString(ptr, len);
            if (obj && typeof obj.push === 'function') obj.push(str);
        }

        js_pushInt(handle, val) {
            const obj = this.getElement(handle);
            if (obj && typeof obj.push === 'function') obj.push(val);
        }

        js_pushDouble(handle, val) {
            const obj = this.getElement(handle);
            if (obj && typeof obj.push === 'function') obj.push(val);
        }

        js_pushBoolean(handle, val) {
            const obj = this.getElement(handle);
            if (obj && typeof obj.push === 'function') obj.push(val !== 0);
        }

        js_number_of_int(val) {
            return this.getOrCreateHandle(Number(val));
        }

        js_number_of_double(val) {
            return this.getOrCreateHandle(Number(val));
        }

        js_boolean_of(val) {
            return this.getOrCreateHandle(Boolean(val));
        }

        js_string_of(ptr, len) {
            const s = this.readString(ptr, len);
            return this.getOrCreateHandle(s);
        }

        js_bigint_of_i64(val) {
            return this.getOrCreateHandle(typeof BigInt !== 'undefined' ? BigInt(val) : Number(val));
        }

        js_bigint_of_str(ptr, len) {
            const s = this.readString(ptr, len);
            return this.getOrCreateHandle(typeof BigInt !== 'undefined' ? BigInt(s) : Number(s));
        }

        js_symbol_of(ptr, len) {
            const s = this.readString(ptr, len);
            return this.getOrCreateHandle(Symbol(s));
        }

        js_symbol_for(ptr, len) {
            const s = this.readString(ptr, len);
            return this.getOrCreateHandle(Symbol.for(s));
        }

        js_undefined() {
            return this.getOrCreateHandle(undefined);
        }

        js_null() {
            return this.getOrCreateHandle(null);
        }

        js_error_of(ptr, len) {
            const msg = this.readString(ptr, len);
            return this.getOrCreateHandle(new Error(msg));
        }

        js_type_error_of(ptr, len) {
            const msg = this.readString(ptr, len);
            return this.getOrCreateHandle(new TypeError(msg));
        }

        js_range_error_of(ptr, len) {
            const msg = this.readString(ptr, len);
            return this.getOrCreateHandle(new RangeError(msg));
        }

        js_syntax_error_of(ptr, len) {
            const msg = this.readString(ptr, len);
            return this.getOrCreateHandle(new SyntaxError(msg));
        }

        js_typeof(handle) {
            if (handle === 0) return this.writeString('object');
            const val = this.getElement(handle);
            return this.writeString(typeof val);
        }

        js_isNumber(handle) {
            const val = this.getElement(handle);
            return typeof val === 'number' ? 1 : 0;
        }

        js_isBoolean(handle) {
            const val = this.getElement(handle);
            return typeof val === 'boolean' ? 1 : 0;
        }

        js_isString(handle) {
            const val = this.getElement(handle);
            return typeof val === 'string' ? 1 : 0;
        }

        js_isBigInt(handle) {
            const val = this.getElement(handle);
            return typeof val === 'bigint' ? 1 : 0;
        }

        js_isSymbol(handle) {
            const val = this.getElement(handle);
            return typeof val === 'symbol' ? 1 : 0;
        }

        js_isUndefined(handle) {
            if (handle === 0) return 0;
            const val = this.getElement(handle);
            return val === undefined ? 1 : 0;
        }

        js_isNull(handle) {
            if (handle === 0) return 1;
            const val = this.getElement(handle);
            return val === null ? 1 : 0;
        }

        js_isObject(handle) {
            if (handle === 0) return 0;
            const val = this.getElement(handle);
            return (val !== null && typeof val === 'object') ? 1 : 0;
        }

        js_isFunction(handle) {
            const val = this.getElement(handle);
            return typeof val === 'function' ? 1 : 0;
        }

        js_isError(handle) {
            const val = this.getElement(handle);
            return (val instanceof Error) ? 1 : 0;
        }

        js_instanceof(handle, namePtr, nameLen) {
            const val = this.getElement(handle);
            const name = this.readString(namePtr, nameLen);
            const g = (typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : global));
            const ctor = g[name];
            return (ctor && typeof ctor === 'function' && val instanceof ctor) ? 1 : 0;
        }

        js_strictEquals(h1, h2) {
            const v1 = this.getElement(h1);
            const v2 = this.getElement(h2);
            return (v1 === v2) ? 1 : 0;
        }

        js_isNaN(handle) {
            const val = this.getElement(handle);
            return Number.isNaN(val) ? 1 : 0;
        }

        js_isFinite(handle) {
            const val = this.getElement(handle);
            return Number.isFinite(val) ? 1 : 0;
        }

        js_isInteger(handle) {
            const val = this.getElement(handle);
            return Number.isInteger(val) ? 1 : 0;
        }

        js_symbolDescription(handle) {
            const val = this.getElement(handle);
            const desc = (typeof val === 'symbol' && val.description) ? val.description : '';
            return this.writeString(desc);
        }

        js_errorMessage(handle) {
            const val = this.getElement(handle);
            return this.writeString((val && val.message) ? String(val.message) : '');
        }

        js_errorName(handle) {
            const val = this.getElement(handle);
            return this.writeString((val && val.name) ? String(val.name) : 'Error');
        }

        js_errorStack(handle) {
            const val = this.getElement(handle);
            return this.writeString((val && val.stack) ? String(val.stack) : '');
        }

        thread_spawn(namePtr, nameLen, arg) {
            const funcName = this.readString(namePtr, nameLen);
            if (this.instance && this.instance.exports && typeof this.instance.exports[funcName] === 'function') {
                if (typeof setTimeout !== 'undefined') {
                    setTimeout(() => {
                        this.instance.exports[funcName](arg);
                    }, 0);
                } else {
                    this.instance.exports[funcName](arg);
                }
                return 1;
            }
            return 0;
        }

        thread_sleep(ms) {
            if (typeof Atomics !== 'undefined' && typeof SharedArrayBuffer !== 'undefined') {
                const sleepBuf = new Int32Array(new SharedArrayBuffer(4));
                Atomics.wait(sleepBuf, 0, 0, ms);
            }
        }

        thread_current_id() {
            return 0;
        }

        thread_hardware_concurrency() {
            if (typeof navigator !== 'undefined' && navigator.hardwareConcurrency) {
                return navigator.hardwareConcurrency;
            }
            if (typeof require !== 'undefined') {
                try {
                    const os = require('os');
                    if (os && os.cpus) return os.cpus().length;
                } catch (e) {}
            }
            return 4;
        }

        getImports(customImports = {}) {
            const inlineObj = {};
            for (const [key, fn] of this.inlineJsFunctions.entries()) {
                inlineObj[key] = fn;
            }
            return {
                dom: {
                    ...inlineObj,
                    getElementById: (ptr, len) => this.getElementById(ptr, len),
                    createElement: (ptr, len) => this.createElement(ptr, len),
                    getBody: () => this.getBody(),
                    querySelector: (ptr, len) => this.querySelector(ptr, len),
                    querySelectorAll: (ptr, len) => this.querySelectorAll(ptr, len),
                    getCanvasContext2D: (ptr, len) => this.getCanvasContext2D(ptr, len),
                    getContext2D: (h) => this.getContext2D(h),
                    getContext: (h, tp, tl) => this.getContext(h, tp, tl),
                    getWebGLContext: (h) => this.getWebGLContext(h),
                    getWidth: (h) => this.getWidth(h),
                    setWidth: (h, w) => this.setWidth(h, w),
                    getHeight: (h) => this.getHeight(h),
                    setHeight: (h, ht) => this.setHeight(h, ht),
                    setInnerText: (handle, ptr, len) => this.setInnerText(handle, ptr, len),
                    getInnerText: (handle) => this.getInnerText(handle),
                    setAttribute: (handle, np, nl, vp, vl) => this.setAttribute(handle, np, nl, vp, vl),
                    getAttribute: (handle, np, nl) => this.getAttribute(handle, np, nl),
                    appendChild: (ph, ch) => this.appendChild(ph, ch),
                    removeChild: (ph, ch) => this.removeChild(ph, ch),
                    remove: (handle) => this.remove(handle),
                    getParent: (handle) => this.getParent(handle),
                    getValue: (handle) => this.getValue(handle),
                    setValue: (handle, vp, vl) => this.setValue(handle, vp, vl),
                    isChecked: (handle) => this.isChecked(handle),
                    setChecked: (handle, checked) => this.setChecked(handle, checked),
                    setStyle: (handle, np, nl, vp, vl) => this.setStyle(handle, np, nl, vp, vl),
                    getStyle: (handle, np, nl) => this.getStyle(handle, np, nl),
                    addClass: (handle, np, nl) => this.addClass(handle, np, nl),
                    removeClass: (handle, np, nl) => this.removeClass(handle, np, nl),
                    hasClass: (handle, np, nl) => this.hasClass(handle, np, nl),
                    toggleClass: (handle, np, nl) => this.toggleClass(handle, np, nl),
                    fillRect: (h, x, y, w, ht) => this.fillRect(h, x, y, w, ht),
                    strokeRect: (h, x, y, w, ht) => this.strokeRect(h, x, y, w, ht),
                    clearRect: (h, x, y, w, ht) => this.clearRect(h, x, y, w, ht),
                    beginPath: (h) => this.beginPath(h),
                    closePath: (h) => this.closePath(h),
                    moveTo: (h, x, y) => this.moveTo(h, x, y),
                    lineTo: (h, x, y) => this.lineTo(h, x, y),
                    arc: (h, x, y, r, sa, ea, ccw) => this.arc(h, x, y, r, sa, ea, ccw),
                    rect: (h, x, y, w, ht) => this.rect(h, x, y, w, ht),
                    ellipse: (h, x, y, rx, ry, rot, sa, ea) => this.ellipse(h, x, y, rx, ry, rot, sa, ea),
                    fill: (h) => this.fill(h),
                    stroke: (h) => this.stroke(h),
                    fillText: (h, tp, tl, x, y) => this.fillText(h, tp, tl, x, y),
                    strokeText: (h, tp, tl, x, y) => this.strokeText(h, tp, tl, x, y),
                    setFillStyle: (h, sp, sl) => this.setFillStyle(h, sp, sl),
                    getFillStyle: (h) => this.getFillStyle(h),
                    setStrokeStyle: (h, sp, sl) => this.setStrokeStyle(h, sp, sl),
                    getStrokeStyle: (h) => this.getStrokeStyle(h),
                    setLineWidth: (h, w) => this.setLineWidth(h, w),
                    getLineWidth: (h) => this.getLineWidth(h),
                    setFont: (h, fp, fl) => this.setFont(h, fp, fl),
                    getFont: (h) => this.getFont(h),
                    setTextAlign: (h, ap, al) => this.setTextAlign(h, ap, al),
                    getTextAlign: (h) => this.getTextAlign(h),
                    drawImage: (h, ih, dx, dy, dw, dh) => this.drawImage(h, ih, dx, dy, dw, dh),
                    drawImageFull: (h, ih, sx, sy, sw, sh, dx, dy, dw, dh) => this.drawImageFull(h, ih, sx, sy, sw, sh, dx, dy, dw, dh),
                    save: (h) => this.save(h),
                    restore: (h) => this.restore(h),
                    translate: (h, x, y) => this.translate(h, x, y),
                    rotate: (h, a) => this.rotate(h, a),
                    scale: (h, sx, sy) => this.scale(h, sx, sy),
                    resetTransform: (h) => this.resetTransform(h),
                    setGlobalAlpha: (h, a) => this.setGlobalAlpha(h, a),
                    getGlobalAlpha: (h) => this.getGlobalAlpha(h),
                    webgl_clearColor: (h, r, g, b, a) => this.webgl_clearColor(h, r, g, b, a),
                    webgl_clear: (h, m) => this.webgl_clear(h, m),
                    webgl_viewport: (h, x, y, w, ht) => this.webgl_viewport(h, x, y, w, ht),
                    webgl_enable: (h, c) => this.webgl_enable(h, c),
                    webgl_disable: (h, c) => this.webgl_disable(h, c),
                    addEventListener: (handle, ep, el, hp, hl) => this.addEventListener(handle, ep, el, hp, hl),
                    setTimeout: (hp, hl, d) => this.setTimeout(hp, hl, d),
                    clearTimeout: (id) => this.clearTimeout(id),
                    setInterval: (hp, hl, d) => this.setInterval(hp, hl, d),
                    clearInterval: (id) => this.clearInterval(id),
                    requestAnimationFrame: (hp, hl) => this.requestAnimationFrame(hp, hl),
                    cancelAnimationFrame: (id) => this.cancelAnimationFrame(id),
                    fetch: (up, ul, hp, hl) => this.fetch(up, ul, hp, hl),
                    alert: (mp, ml) => this.alert(mp, ml),
                    confirm: (mp, ml) => this.confirm(mp, ml),
                    prompt: (mp, ml) => this.prompt(mp, ml),
                    js_global: () => this.js_global(),
                    js_eval: (ptr, len) => this.js_eval(ptr, len),
                    js_createObject: () => this.js_createObject(),
                    js_createArray: () => this.js_createArray(),
                    js_get: (h, pp, pl) => this.js_get(h, pp, pl),
                    js_getString: (h, pp, pl) => this.js_getString(h, pp, pl),
                    js_getInt: (h, pp, pl) => this.js_getInt(h, pp, pl),
                    js_getDouble: (h, pp, pl) => this.js_getDouble(h, pp, pl),
                    js_getBoolean: (h, pp, pl) => this.js_getBoolean(h, pp, pl),
                    js_setString: (h, pp, pl, vp, vl) => this.js_setString(h, pp, pl, vp, vl),
                    js_setInt: (h, pp, pl, v) => this.js_setInt(h, pp, pl, v),
                    js_setDouble: (h, pp, pl, v) => this.js_setDouble(h, pp, pl, v),
                    js_setBoolean: (h, pp, pl, v) => this.js_setBoolean(h, pp, pl, v),
                    js_setObject: (h, pp, pl, vh) => this.js_setObject(h, pp, pl, vh),
                    js_call0: (h, mp, ml) => this.js_call0(h, mp, ml),
                    js_call1_str: (h, mp, ml, ap, al) => this.js_call1_str(h, mp, ml, ap, al),
                    js_call1_int: (h, mp, ml, a) => this.js_call1_int(h, mp, ml, a),
                    js_call1_double: (h, mp, ml, a) => this.js_call1_double(h, mp, ml, a),
                    js_call1_bool: (h, mp, ml, a) => this.js_call1_bool(h, mp, ml, a),
                    js_call1_obj: (h, mp, ml, ah) => this.js_call1_obj(h, mp, ml, ah),
                    js_call2_str_str: (h, mp, ml, a1p, a1l, a2p, a2l) => this.js_call2_str_str(h, mp, ml, a1p, a1l, a2p, a2l),
                    js_call2_str_int: (h, mp, ml, a1p, a1l, a2) => this.js_call2_str_int(h, mp, ml, a1p, a1l, a2),
                    js_call2_str_obj: (h, mp, ml, a1p, a1l, a2h) => this.js_call2_str_obj(h, mp, ml, a1p, a1l, a2h),
                    js_call2_obj_obj: (h, mp, ml, a1h, a2h) => this.js_call2_obj_obj(h, mp, ml, a1h, a2h),
                    js_callString0: (h, mp, ml) => this.js_callString0(h, mp, ml),
                    js_callString1_str: (h, mp, ml, ap, al) => this.js_callString1_str(h, mp, ml, ap, al),
                    js_callInt0: (h, mp, ml) => this.js_callInt0(h, mp, ml),
                    js_callInt1_int: (h, mp, ml, a) => this.js_callInt1_int(h, mp, ml, a),
                    js_callDouble0: (h, mp, ml) => this.js_callDouble0(h, mp, ml),
                    js_callBoolean0: (h, mp, ml) => this.js_callBoolean0(h, mp, ml),
                    js_asString: (h) => this.js_asString(h),
                    js_asInt: (h) => this.js_asInt(h),
                    js_asDouble: (h) => this.js_asDouble(h),
                    js_asBoolean: (h) => this.js_asBoolean(h),
                    js_isArray: (h) => this.js_isArray(h),
                    js_length: (h) => this.js_length(h),
                    js_getIndex: (h, idx) => this.js_getIndex(h, idx),
                    js_setIndexObj: (h, idx, vh) => this.js_setIndexObj(h, idx, vh),
                    js_setIndexStr: (h, idx, p, l) => this.js_setIndexStr(h, idx, p, l),
                    js_setIndexInt: (h, idx, v) => this.js_setIndexInt(h, idx, v),
                    js_setIndexDouble: (h, idx, v) => this.js_setIndexDouble(h, idx, v),
                    js_setIndexBoolean: (h, idx, v) => this.js_setIndexBoolean(h, idx, v),
                    js_pushObj: (h, vh) => this.js_pushObj(h, vh),
                    js_pushStr: (h, p, l) => this.js_pushStr(h, p, l),
                    js_pushInt: (h, v) => this.js_pushInt(h, v),
                    js_pushDouble: (h, v) => this.js_pushDouble(h, v),
                    js_pushBoolean: (h, v) => this.js_pushBoolean(h, v),
                    js_number_of_int: (v) => this.js_number_of_int(v),
                    js_number_of_double: (v) => this.js_number_of_double(v),
                    js_boolean_of: (v) => this.js_boolean_of(v),
                    js_string_of: (p, l) => this.js_string_of(p, l),
                    js_bigint_of_i64: (v) => this.js_bigint_of_i64(v),
                    js_bigint_of_str: (p, l) => this.js_bigint_of_str(p, l),
                    js_symbol_of: (p, l) => this.js_symbol_of(p, l),
                    js_symbol_for: (p, l) => this.js_symbol_for(p, l),
                    js_undefined: () => this.js_undefined(),
                    js_null: () => this.js_null(),
                    js_error_of: (p, l) => this.js_error_of(p, l),
                    js_type_error_of: (p, l) => this.js_type_error_of(p, l),
                    js_range_error_of: (p, l) => this.js_range_error_of(p, l),
                    js_syntax_error_of: (p, l) => this.js_syntax_error_of(p, l),
                    js_typeof: (h) => this.js_typeof(h),
                    js_isNumber: (h) => this.js_isNumber(h),
                    js_isBoolean: (h) => this.js_isBoolean(h),
                    js_isString: (h) => this.js_isString(h),
                    js_isBigInt: (h) => this.js_isBigInt(h),
                    js_isSymbol: (h) => this.js_isSymbol(h),
                    js_isUndefined: (h) => this.js_isUndefined(h),
                    js_isNull: (h) => this.js_isNull(h),
                    js_isObject: (h) => this.js_isObject(h),
                    js_isFunction: (h) => this.js_isFunction(h),
                    js_isError: (h) => this.js_isError(h),
                    js_instanceof: (h, p, l) => this.js_instanceof(h, p, l),
                    js_strictEquals: (h1, h2) => this.js_strictEquals(h1, h2),
                    js_isNaN: (h) => this.js_isNaN(h),
                    js_isFinite: (h) => this.js_isFinite(h),
                    js_isInteger: (h) => this.js_isInteger(h),
                    js_symbolDescription: (h) => this.js_symbolDescription(h),
                    js_errorMessage: (h) => this.js_errorMessage(h),
                    js_errorName: (h) => this.js_errorName(h),
                    js_errorStack: (h) => this.js_errorStack(h),
                    thread_spawn: (p, l, a) => this.thread_spawn(p, l, a),
                    thread_sleep: (ms) => this.thread_sleep(ms),
                    thread_current_id: () => this.thread_current_id(),
                    thread_hardware_concurrency: () => this.thread_hardware_concurrency(),
                    ...(customImports.dom || {})
                },
                wasi_snapshot_preview1: {
                    fd_write: (fd, iovs_ptr, iovs_len, nwritten_ptr) => {
                        let totalWritten = 0;
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            const chunks = [];
                            for (let i = 0; i < iovs_len; i++) {
                                const bufPtr = view.getUint32(iovs_ptr + i * 8, true);
                                const bufLen = view.getUint32(iovs_ptr + i * 8 + 4, true);
                                if (bufLen > 0) {
                                    const bytes = new Uint8Array(memory.buffer, bufPtr, bufLen);
                                    chunks.push(bytes);
                                    totalWritten += bufLen;
                                }
                            }
                            if (view.byteLength >= nwritten_ptr + 4) {
                                view.setUint32(nwritten_ptr, totalWritten, true);
                            }
                            if (fd === 1) {
                                let str = '';
                                for (const c of chunks) str += textDecoder.decode(c);
                                if (typeof process !== 'undefined' && process.stdout && typeof process.stdout.write === 'function') {
                                    process.stdout.write(str);
                                } else {
                                    console.log(str.endsWith('\n') ? str.slice(0, -1) : str);
                                }
                            } else if (fd === 2) {
                                let str = '';
                                for (const c of chunks) str += textDecoder.decode(c);
                                if (typeof process !== 'undefined' && process.stderr && typeof process.stderr.write === 'function') {
                                    process.stderr.write(str);
                                } else {
                                    console.error(str.endsWith('\n') ? str.slice(0, -1) : str);
                                }
                            } else if (this.fds.has(fd)) {
                                const entry = this.fds.get(fd);
                                const fileObj = entry.vfsFile;
                                let needed = entry.offset;
                                for (const c of chunks) needed += c.length;
                                if (needed > fileObj.content.length) {
                                    const newContent = new Uint8Array(needed);
                                    newContent.set(fileObj.content, 0);
                                    fileObj.content = newContent;
                                }
                                for (const c of chunks) {
                                    fileObj.content.set(c, entry.offset);
                                    entry.offset += c.length;
                                }
                                fileObj.mtime = Date.now();
                            }
                        }
                        return 0;
                    },
                    fd_read: (fd, iovs_ptr, iovs_len, nread_ptr) => {
                        let totalRead = 0;
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            if (this.fds.has(fd)) {
                                const entry = this.fds.get(fd);
                                const fileObj = entry.vfsFile;
                                for (let i = 0; i < iovs_len; i++) {
                                    const bufPtr = view.getUint32(iovs_ptr + i * 8, true);
                                    const bufLen = view.getUint32(iovs_ptr + i * 8 + 4, true);
                                    const available = fileObj.content.length - entry.offset;
                                    if (available <= 0) break;
                                    const toRead = Math.min(bufLen, available);
                                    if (toRead > 0) {
                                        const dest = new Uint8Array(memory.buffer, bufPtr, toRead);
                                        dest.set(fileObj.content.subarray(entry.offset, entry.offset + toRead));
                                        entry.offset += toRead;
                                        totalRead += toRead;
                                    }
                                }
                            }
                            if (view.byteLength >= nread_ptr + 4) {
                                view.setUint32(nread_ptr, totalRead, true);
                            }
                        }
                        return 0;
                    },
                    path_open: (dirfd, dirflags, path_ptr, path_len, oflags, fs_rights_base, fs_rights_inheriting, fdflags, opened_fd_ptr) => {
                        const path = this.readString(path_ptr, path_len);
                        const norm = this.normalizePath(path);
                        let fileObj = this.vfs.get(norm);
                        if (!fileObj) {
                            if ((oflags & 1) !== 0) { // O_CREAT
                                fileObj = { content: new Uint8Array(0), isDirectory: (oflags & 2) !== 0, mtime: Date.now() };
                                this.vfs.set(norm, fileObj);
                            } else {
                                return 44; // ENOENT
                            }
                        }
                        if ((oflags & 8) !== 0) { // O_TRUNC
                            fileObj.content = new Uint8Array(0);
                        }
                        const fd = this.nextFd++;
                        const initialOffset = ((oflags & 16) !== 0) ? fileObj.content.length : 0; // O_APPEND
                        this.fds.set(fd, { path: norm, vfsFile: fileObj, offset: initialOffset, flags: fdflags });
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            view.setUint32(opened_fd_ptr, fd, true);
                        }
                        return 0;
                    },
                    fd_close: (fd) => {
                        this.fds.delete(fd);
                        return 0;
                    },
                    fd_seek: (fd, offset_i64, whence, newoffset_ptr) => {
                        const entry = this.fds.get(fd);
                        if (!entry) return 8; // EBADF
                        const off = Number(offset_i64);
                        if (whence === 0) entry.offset = off; // SEEK_SET
                        else if (whence === 1) entry.offset += off; // SEEK_CUR
                        else if (whence === 2) entry.offset = entry.vfsFile.content.length + off; // SEEK_END
                        if (entry.offset < 0) entry.offset = 0;
                        const memory = this.getMemory();
                        if (memory && memory.buffer && newoffset_ptr > 0) {
                            const view = new DataView(memory.buffer);
                            view.setBigUint64(newoffset_ptr, BigInt(entry.offset), true);
                        }
                        return 0;
                    },
                    fd_filestat_get: (fd, filestat_ptr) => {
                        const entry = this.fds.get(fd);
                        if (!entry) return 8;
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            view.setBigUint64(filestat_ptr, 0n, true); // dev
                            view.setBigUint64(filestat_ptr + 8, BigInt(fd), true); // ino
                            view.setUint8(filestat_ptr + 16, entry.vfsFile.isDirectory ? 3 : 4); // filetype
                            view.setUint32(filestat_ptr + 20, 1, true); // nlink
                            view.setBigUint64(filestat_ptr + 32, BigInt(entry.vfsFile.content.length), true); // size
                            const timeNs = BigInt(entry.vfsFile.mtime || Date.now()) * 1000000n;
                            view.setBigUint64(filestat_ptr + 40, timeNs, true); // atim
                            view.setBigUint64(filestat_ptr + 48, timeNs, true); // mtim
                            view.setBigUint64(filestat_ptr + 56, timeNs, true); // ctim
                        }
                        return 0;
                    },
                    path_filestat_get: (fd, flags, path_ptr, path_len, filestat_ptr) => {
                        const path = this.readString(path_ptr, path_len);
                        const norm = this.normalizePath(path);
                        const fileObj = this.vfs.get(norm);
                        if (!fileObj) return 44; // ENOENT
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            view.setBigUint64(filestat_ptr, 0n, true);
                            view.setBigUint64(filestat_ptr + 8, 1n, true);
                            view.setUint8(filestat_ptr + 16, fileObj.isDirectory ? 3 : 4);
                            view.setUint32(filestat_ptr + 20, 1, true);
                            view.setBigUint64(filestat_ptr + 32, BigInt(fileObj.content.length), true);
                            const timeNs = BigInt(fileObj.mtime || Date.now()) * 1000000n;
                            view.setBigUint64(filestat_ptr + 40, timeNs, true);
                            view.setBigUint64(filestat_ptr + 48, timeNs, true);
                            view.setBigUint64(filestat_ptr + 56, timeNs, true);
                        }
                        return 0;
                    },
                    path_create_directory: (fd, path_ptr, path_len) => {
                        const path = this.readString(path_ptr, path_len);
                        this.mkdir(path);
                        return 0;
                    },
                    path_unlink_file: (fd, path_ptr, path_len) => {
                        const path = this.readString(path_ptr, path_len);
                        const success = this.unlink(path);
                        return success ? 0 : 44;
                    },
                    proc_exit: (rval) => {
                        this.lastExitCode = rval;
                    },
                    args_sizes_get: (argc_ptr, argv_buf_size_ptr) => {
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            view.setUint32(argc_ptr, 0, true);
                            view.setUint32(argv_buf_size_ptr, 0, true);
                        }
                        return 0;
                    },
                    args_get: (argv_ptr, argv_buf_ptr) => 0,
                    environ_sizes_get: (env_count_ptr, env_buf_size_ptr) => {
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            view.setUint32(env_count_ptr, 0, true);
                            view.setUint32(env_buf_size_ptr, 0, true);
                        }
                        return 0;
                    },
                    environ_get: (environ_ptr, environ_buf_ptr) => 0,
                    clock_time_get: (id, precision, time_ptr) => {
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const view = new DataView(memory.buffer);
                            const nowNs = BigInt(Date.now()) * 1000000n;
                            view.setBigUint64(time_ptr, nowNs, true);
                        }
                        return 0;
                    },
                    random_get: (buf_ptr, buf_len) => {
                        const memory = this.getMemory();
                        if (memory && memory.buffer) {
                            const arr = new Uint8Array(memory.buffer, buf_ptr, buf_len);
                            if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
                                crypto.getRandomValues(arr);
                            } else {
                                for (let i = 0; i < buf_len; i++) arr[i] = (Math.random() * 256) | 0;
                            }
                        }
                        return 0;
                    },
                    ...(customImports.wasi_snapshot_preview1 || {})
                },
                js: {
                    ...inlineObj,
                    ...(customImports.js || {})
                },
                ...customImports
            };
        }
    }

    async function instantiateJavaw(wasmSource, customImports = {}, doc = null) {
        const runtime = new JavawRuntime(doc);
        let module;
        let wasmBytes;
        if (wasmSource instanceof WebAssembly.Module) {
            module = wasmSource;
        } else if (wasmSource instanceof ArrayBuffer || wasmSource instanceof Uint8Array) {
            wasmBytes = wasmSource instanceof Uint8Array ? wasmSource.buffer : wasmSource;
            module = await WebAssembly.compile(wasmBytes);
        } else if (typeof WebAssembly.compileStreaming === 'function' && typeof Response !== 'undefined' && wasmSource instanceof Response) {
            module = await WebAssembly.compileStreaming(wasmSource);
        } else if (typeof Response !== 'undefined' && wasmSource instanceof Response) {
            wasmBytes = await wasmSource.arrayBuffer();
            module = await WebAssembly.compile(wasmBytes);
        } else {
            module = await WebAssembly.compile(wasmSource);
        }

        runtime.loadCustomSections(module);
        const importObject = runtime.getImports(customImports);
        const instance = await WebAssembly.instantiate(module, importObject);
        runtime.setInstance(instance);
        return {
            instance,
            runtime,
            module,
            exports: instance.exports
        };
    }

    const javawLib = {
        JavawRuntime,
        instantiateJavaw
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = javawLib;
    }
    if (typeof window !== 'undefined') {
        window.Javaw = javawLib;
        window.JavawRuntime = JavawRuntime;
        window.instantiateJavaw = instantiateJavaw;
    } else if (typeof global !== 'undefined') {
        global.Javaw = javawLib;
        global.JavawRuntime = JavawRuntime;
        global.instantiateJavaw = instantiateJavaw;
    }
})(typeof globalThis !== 'undefined' ? globalThis : this);
