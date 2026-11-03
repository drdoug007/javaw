const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { startServer } = require('./server.js');
const { createSpringWeb } = require('./spring-web.js');

async function runTests() {
    const server = await startServer(0);
    const base = `http://127.0.0.1:${server.address().port}`;
    async function request(route, method = 'GET', payload) {
        const response = await fetch(base + route, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: payload === undefined ? undefined : JSON.stringify(payload)
        });
        const text = await response.text();
        return { status: response.status, body: text ? JSON.parse(text) : null };
    }
    try {
        const dashboard = await fetch(base + '/');
        assert.equal(dashboard.status, 200);
        assert.match(await dashboard.text(), /Spring API CRUD subset/);
        const seeded = (await request('/api/products')).body;
        assert.deepEqual(seeded, [
            { id: 1, name: 'MacBook Pro M3', price: 1999, stock: 25 },
            { id: 2, name: 'Dell XPS 15', price: 11499, stock: 18 },
            { id: 3, name: 'Sony WH-1000XM5', price: 1399, stock: 50 }
        ]);
        for (const product of seeded) assert.equal((await request('/api/products/' + product.id, 'DELETE')).status, 204);
        assert.deepEqual((await request('/api/products')).body, []);
        const missing = await request('/api/products/99');
        assert.equal(missing.status, 404);
        assert.equal(missing.body.error, 'Product not found with id 99');

        const payload = { name: 'Keyboard "雪" \\ newline\n', price: 129.95, stock: 40 };
        const created = await request('/api/products', 'POST', payload);
        assert.equal(created.status, 201);
        assert.deepEqual(created.body, { id: 4, ...payload });
        const second = await request('/api/products', 'POST', { name: 'Mouse', price: '19.99', stock: 2 });
        assert.deepEqual(second, { status: 201, body: { id: 5, name: 'Mouse', price: 19.99, stock: 2 } });
        assert.deepEqual((await request('/api/products/4')).body, created.body);
        assert.deepEqual((await request('/api/products/')).body, [created.body, second.body]);

        const update = { name: 'Updated 雪', price: 1.25, stock: 7 };
        const updated = await request('/api/products/4', 'PUT', update);
        assert.deepEqual(updated, { status: 200, body: { id: 4, ...update } });
        assert.deepEqual((await request('/api/products/4')).body, updated.body);
        assert.equal((await request('/api/products/99', 'PUT', payload)).status, 404);
        assert.equal((await request('/api/products/99', 'DELETE')).status, 404);
        assert.deepEqual(await request('/api/products/4', 'DELETE'), { status: 204, body: null });
        assert.equal((await request('/api/products/4')).status, 404);
        assert.deepEqual((await request('/api/products')).body, [second.body]);
        const third = await request('/api/products', 'POST', { name: 'Third', price: 0.01, stock: 0 });
        assert.equal(third.body.id, 6);
        assert.equal(third.body.price, 0.01);
        // Remove the tail, then the last remaining row; verify IDs are never reused.
        assert.equal((await request('/api/products/6', 'DELETE')).status, 204);
        assert.equal((await request('/api/products/5', 'DELETE')).status, 204);
        assert.deepEqual((await request('/api/products')).body, []);
        assert.equal((await request('/api/products', 'POST', payload)).body.id, 7);

        // Preserve every decimal digit on the HTTP wire, including numeric JSON input.
        for (const price of ['9007199254740993.12345678901234567890', '0.00000000000000000000000012345']) {
            const response = await fetch(base + '/api/products', {
                method: 'POST', body: '{"name":"Exact","price":' + price + ',"stock":1}'
            });
            assert.equal(response.status, 201);
            const text = await response.text();
            const product = JSON.parse(text);
            const expected = price.startsWith('0.') ? '1.2345E-25' : price;
            assert.ok(text.includes('"price":' + expected), text);
            const read = await fetch(base + '/api/products/' + product.id);
            assert.ok((await read.text()).includes('"price":' + expected));
            await request('/api/products/' + product.id, 'DELETE');
        }

        for (const id of ['abc', '1abc', '1.5', '2147483648']) {
            assert.equal((await request('/api/products/' + id)).status, 400);
        }
        for (const invalid of [{ ...payload, stock: 1.2 }, { ...payload, price: 'bad' }, { ...payload, name: 123 }, []]) {
            assert.equal((await request('/api/products', 'POST', invalid)).status, 400);
        }
        const malformed = await fetch(base + '/api/products', { method: 'POST', body: '{' });
        assert.equal(malformed.status, 400);
        assert.equal((await request('/unknown')).status, 404);
        // Errors must leave the server and repository usable.
        assert.equal((await request('/api/products')).body.length, 1);

        // A fresh Wasm instance has its own database; there is no JavaScript row store.
        const { instance } = await createSpringWeb(fs.readFileSync(path.join(__dirname, 'target', 'generated-wasm', 'SpringBootApp.wasm')));
        const e = instance.exports;
        const ptr = e.ProductController_handleGetAll(e.__javaw_controller_ProductController());
        assert.equal(new DataView(e.memory.buffer).getInt32(ptr, true), 3);
        // Calling main again must not run the same startup bean twice.
        e.main(0);
        const after = e.ProductController_handleGetAll(e.__javaw_controller_ProductController());
        assert.equal(new DataView(e.memory.buffer).getInt32(after, true), 3);
        // Exercise the actual bridge's collection boundary while keeping live repository rows.
        const web = await createSpringWeb(fs.readFileSync(path.join(__dirname, 'target', 'generated-wasm', 'SpringBootApp.wasm')));
        async function invoke(method, route, body) {
            const { Readable } = require('node:stream');
            const req = Readable.from(body === undefined ? [] : [JSON.stringify(body)]); req.method = method;
            let status, text;
            await web.handle(req, { writeHead(code) { status=code; }, end(value) { text=value; } }, new URL(route, 'http://localhost'));
            return { status, body: text ? JSON.parse(text) : null };
        }
        async function cycle() {
            const created = await invoke('POST', '/api/products', { name: 'Temporary 雪', price: '12345678901234567890.123456789', stock: 2 });
            assert.equal(created.status, 201);
            assert.equal((await invoke('GET', '/api/products/' + created.body.id)).status, 200);
            assert.equal((await invoke('PUT', '/api/products/' + created.body.id, { name: 'Updated', price: '0.0001', stock: 3 })).status, 200);
            assert.equal((await invoke('DELETE', '/api/products/' + created.body.id)).status, 204);
            assert.equal((await invoke('GET', '/api/products/' + created.body.id)).status, 404);
        }
        // Warm past the lazy Integer cache (IDs up to 127 are retained intentionally).
        for (let i=0;i<160;i++) await cycle();
        const heap=web.instance.exports;
        const live=heap.__gc_live_bytes(), allocated=heap.__gc_heap_bytes(), pages=heap.memory.buffer.byteLength;
        for (let i=0;i<300;i++) await cycle();
        assert.equal(heap.__gc_live_bytes(),live);
        assert.equal(heap.__gc_heap_bytes(),allocated);
        assert.equal(heap.memory.buffer.byteLength,pages);
        assert.equal((await invoke('GET', '/api/products')).body.length,3);
        console.log('All WebAssembly controller, repository, exact decimal, HTTP and bounded-memory tests passed.');
    } finally {
        await new Promise(resolve => server.close(resolve));
    }
}
runTests().catch(error => { console.error(error); process.exitCode = 1; });
