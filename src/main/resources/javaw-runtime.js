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
                    if (this.instance && typeof this.instance.exports[handlerName] === 'function') {
                        this.instance.exports[handlerName]();
                    }
                });
            }
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

        getImports(customImports = {}) {
            return {
                dom: {
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
