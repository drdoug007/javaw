/**
 * javaw-runtime.js
 * Lightweight host runtime bridge for Java WebAssembly applications interacting with the DOM.
 */

(function (global) {
    const textDecoder = new TextDecoder('utf-8');

    class JavawRuntime {
        constructor(doc) {
            this.doc = doc || (typeof document !== 'undefined' ? document : null);
            this.handleMap = new Map();
            this.reverseMap = new Map();
            this.nextHandle = 1;
            this.instance = null;
        }

        setInstance(instance) {
            this.instance = instance;
        }

        getMemory() {
            return this.instance ? this.instance.exports.memory : null;
        }

        readString(ptr, len) {
            const memory = this.getMemory();
            if (!memory || len === 0) return '';
            const bytes = new Uint8Array(memory.buffer, ptr, len);
            return textDecoder.decode(bytes);
        }

        getOrCreateHandle(element) {
            if (!element) return 0;
            if (this.reverseMap.has(element)) {
                return this.reverseMap.get(element);
            }
            const handle = this.nextHandle++;
            this.handleMap.set(handle, element);
            this.reverseMap.set(element, handle);
            return handle;
        }

        getElement(handle) {
            return this.handleMap.get(handle) || null;
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

        setAttribute(handle, namePtr, nameLen, valPtr, valLen) {
            const name = this.readString(namePtr, nameLen);
            const value = this.readString(valPtr, valLen);
            const el = this.getElement(handle);
            if (el && typeof el.setAttribute === 'function') {
                el.setAttribute(name, value);
            }
        }

        appendChild(parentHandle, childHandle) {
            const parent = this.getElement(parentHandle);
            const child = this.getElement(childHandle);
            if (parent && child && typeof parent.appendChild === 'function') {
                parent.appendChild(child);
            }
        }

        addEventListener(handle, eventPtr, eventLen, handlerPtr, handlerLen) {
            const eventName = this.readString(eventPtr, eventLen);
            const handlerName = this.readString(handlerPtr, handlerLen);
            const el = this.getElement(handle);
            if (el && typeof el.addEventListener === 'function') {
                el.addEventListener(eventName, (event) => {
                    if (this.instance && typeof this.instance.exports[handlerName] === 'function') {
                        this.instance.exports[handlerName]();
                    }
                });
            }
        }

        getImports(customImports = {}) {
            return {
                dom: {
                    getElementById: (ptr, len) => this.getElementById(ptr, len),
                    createElement: (ptr, len) => this.createElement(ptr, len),
                    getBody: () => this.getBody(),
                    setInnerText: (handle, ptr, len) => this.setInnerText(handle, ptr, len),
                    setAttribute: (handle, np, nl, vp, vl) => this.setAttribute(handle, np, nl, vp, vl),
                    appendChild: (ph, ch) => this.appendChild(ph, ch),
                    addEventListener: (handle, ep, el, hp, hl) => this.addEventListener(handle, ep, el, hp, hl),
                    ...(customImports.dom || {})
                },
                ...customImports
            };
        }
    }

    async function instantiateJavaw(wasmSource, customImports = {}, doc = null) {
        const runtime = new JavawRuntime(doc);
        const importObject = runtime.getImports(customImports);
        let result;
        if (typeof WebAssembly.instantiateStreaming === 'function' && wasmSource instanceof Response) {
            result = await WebAssembly.instantiateStreaming(wasmSource, importObject);
        } else {
            result = await WebAssembly.instantiate(wasmSource, importObject);
        }
        const instance = result.instance || result;
        runtime.setInstance(instance);
        return {
            instance,
            runtime,
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
