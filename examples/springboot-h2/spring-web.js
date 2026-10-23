// HTTP marshalling only. Controllers, repository rows and generated IDs live in Wasm.
const { instantiateJavaw } = require('./javaw-runtime.js');

async function createSpringWeb(wasmBytes) {
    const module = await WebAssembly.compile(wasmBytes);
    const sections = WebAssembly.Module.customSections(module, 'javaw_spring_web');
    if (sections.length !== 1) throw new Error('Missing javaw_spring_web metadata; rebuild the module with the current compiler');
    const metadata = JSON.parse(new TextDecoder().decode(sections[0]));
    const { instance } = await instantiateJavaw(module);
    const exports = instance.exports;
    exports.main(0);
    const view = () => new DataView(exports.memory.buffer);
    const readString = ptr => {
        if (!ptr) return null;
        const bytes = new Uint8Array(exports.memory.buffer);
        let end = ptr;
        while (end < bytes.length && bytes[end] !== 0) end++;
        return new TextDecoder().decode(bytes.slice(ptr, end));
    };
    const writeString = text => {
        const bytes = new TextEncoder().encode(text);
        const ptr = exports.__str_alloc(bytes.length);
        new Uint8Array(exports.memory.buffer).set(bytes, ptr);
        new Uint8Array(exports.memory.buffer)[ptr + bytes.length] = 0;
        return ptr;
    };
    const decode = (ptr, type) => {
        if (type.endsWith('[]')) {
            if (!ptr) return [];
            const length = view().getInt32(ptr, true);
            return Array.from({ length }, (_, i) => decode(view().getInt32(ptr + 4 + i * 4, true), type.slice(0, -2)));
        }
        if (!ptr) return null;
        const fields = metadata.entities[type];
        if (!fields) throw new Error('Unsupported response type: ' + type);
        return Object.fromEntries(fields.map(field => {
            const address = ptr + field.offset;
            let value;
            switch (field.type) {
                case 'String': value = readString(view().getInt32(address, true)); break;
                case 'BigDecimal': {
                    const text = readString(view().getInt32(address, true));
                    value = text === null ? null : Number(text);
                    break;
                }
                case 'int': case 'Integer': value = view().getInt32(address, true); break;
                default: throw new Error('Unsupported entity field type: ' + field.type);
            }
            return [field.name, value];
        }));
    };
    const badRequest = message => Object.assign(new Error(message), { status: 400 });
    const encode = (value, parameter) => {
        switch (parameter.type) {
            case 'String':
                if (value === undefined || value === null) return 0;
                if (typeof value !== 'string') throw badRequest(parameter.name + ' must be a string');
                if (value.includes('\0')) throw badRequest(parameter.name + ' cannot contain NUL');
                return writeString(value);
            case 'int': case 'Integer': {
                const number = parameter.path && /^-?\d+$/.test(value) ? Number(value) : value;
                if (!Number.isInteger(number) || number < -2147483648 || number > 2147483647)
                    throw badRequest(parameter.name + ' must be a 32-bit integer');
                return number;
            }
            case 'BigDecimal':
                if (value === null || value === undefined) return 0;
                if ((typeof value !== 'number' && typeof value !== 'string')
                    || !/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(String(value)) || !Number.isFinite(Number(value)))
                    throw badRequest(parameter.name + ' must be a finite decimal');
                return writeString(String(value));
            default: throw new Error('Unsupported parameter type: ' + parameter.type);
        }
    };
    const routes = metadata.routes.map(route => ({
        ...route,
        segments: route.path.split('/').filter(Boolean),
        receiver: exports['__javaw_controller_' + route.controller]()
    }));
    async function handle(req, res, url) {
        const send = (status, body) => {
            res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(body === undefined ? undefined : JSON.stringify(body));
        };
        try {
            const segments = url.pathname.split('/').filter(Boolean).map(decodeURIComponent);
            const route = routes.find(r => r.verb === req.method && r.segments.length === segments.length
                && r.segments.every((part, i) => part.startsWith('{') || part === segments[i]));
            if (!route) return send(404, { error: 'Endpoint not found' });
            const pathValues = Object.fromEntries(route.segments.flatMap((part, i) => part.startsWith('{') ? [[part.slice(1, -1), segments[i]]] : []));
            let payload = {};
            if (req.method === 'POST' || req.method === 'PUT') {
                let body = '';
                for await (const chunk of req) {
                    body += chunk;
                    if (body.length > 1024 * 1024) throw badRequest('Request body too large');
                }
                try { payload = JSON.parse(body || '{}'); } catch { throw badRequest('Invalid JSON payload'); }
                if (payload === null || Array.isArray(payload) || typeof payload !== 'object') throw badRequest('JSON object required');
            }
            const args = route.params.map(p => encode(p.path ? pathValues[p.name] : payload[p.name], p));
            const result = exports[route.export](route.receiver, ...args);
            if (route.result === 'void') return send(204);
            send(req.method === 'POST' ? 201 : 200, decode(result, route.result));
        } catch (error) {
            if (error instanceof WebAssembly.Exception && error.is(exports.Exception)) {
                const ptr = error.getArg(exports.Exception, 0);
                const status = view().getInt32(ptr + metadata.exception.status, true);
                if (view().getInt32(ptr, true) === metadata.exception.vtable && status >= 400 && status <= 599) {
                    return send(status, { error: readString(view().getInt32(ptr + metadata.exception.reason, true)) });
                }
            }
            if (error instanceof URIError) return send(400, { error: 'Invalid URL encoding' });
            if (!error.status) console.error(error);
            send(error.status || 500, { error: error.status ? error.message : 'Internal server error' });
        }
    }
    return { handle, instance, metadata };
}
module.exports = { createSpringWeb };
