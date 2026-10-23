const http = require('http');
const fs = require('fs');
const path = require('path');

const wasmPath = path.join(__dirname, 'SpringBootApp.wasm');
if (!fs.existsSync(wasmPath)) {
    console.error('Error: SpringBootApp.wasm not found at ' + wasmPath);
    console.error('Please run "mvn compile" in this directory or root first to compile the WebAssembly module.');
    process.exit(1);
}

const wasmBytes = fs.readFileSync(wasmPath);

async function startServer(customPort) {
    const { createSpringWeb } = require('./spring-web.js');
    const web = await createSpringWeb(wasmBytes);
    const PORT = customPort ?? process.env.PORT ?? 8080;
    const server = http.createServer((req, res) => {
        const url = new URL(req.url, `http://${req.headers.host}`);
        const pathname = url.pathname;
        const method = req.method;

        // CORS Headers
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

        if (method === 'OPTIONS') {
            res.writeHead(204);
            res.end();
            return;
        }

        // Web UI Dashboard / Documentation at root "/"
        if (pathname === '/' || pathname === '/index.html') {
            res.setHeader('Content-Type', 'text/html; charset=utf-8');
            res.writeHead(200);
            res.end(`<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Spring API subset on WebAssembly (javaw)</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; }
        .container { max-width: 900px; margin: 0 auto; }
        h1 { color: #38bdf8; margin-bottom: 8px; }
        .card { background: #1e293b; border-radius: 8px; padding: 20px; margin-bottom: 20px; border: 1px solid #334155; }
        button { background: #0284c7; color: white; border: none; padding: 8px 16px; border-radius: 6px; cursor: pointer; font-weight: 600; margin-right: 8px; margin-bottom: 8px; }
        button:hover { background: #0369a1; }
        button.delete { background: #ef4444; }
        button.delete:hover { background: #dc2626; }
        pre { background: #020617; color: #a5f3fc; padding: 12px; border-radius: 6px; overflow-x: auto; font-size: 14px; }
        input { background: #0f172a; border: 1px solid #475569; color: white; padding: 8px 12px; border-radius: 6px; margin-right: 8px; margin-bottom: 8px; }
        .badge { display: inline-block; background: #0369a1; color: #e0f2fe; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: bold; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Spring API CRUD subset (Node.js + WebAssembly)</h1>
        <p>This application executes constructor-injected controllers and an in-memory repository compiled to WebAssembly via <span class="badge">javaw</span> on Node.js.</p>
        
        <div class="card">
            <h3>REST Endpoints</h3>
            <div>
                <button onclick="loadProducts()">GET /api/products (Fetch All)</button>
                <button onclick="fetchProduct(1)">GET /api/products/1</button>
            </div>
            <div style="margin-top: 12px;">
                <input id="prodName" placeholder="Product Name" value="Keychron Q1 Pro">
                <input id="prodPrice" type="number" placeholder="Price" value="199">
                <input id="prodStock" type="number" placeholder="Stock" value="45">
                <button onclick="createProduct()">POST /api/products</button>
            </div>
            <div style="margin-top: 12px;">
                <input id="updateId" type="number" placeholder="ID" value="1" style="width: 60px;">
                <input id="updateName" placeholder="New Name" value="MacBook Pro M3 Max">
                <input id="updatePrice" type="number" placeholder="Price" value="2499">
                <input id="updateStock" type="number" placeholder="Stock" value="12">
                <button onclick="updateProduct()">PUT /api/products/:id</button>
                <button class="delete" onclick="deleteProduct()">DELETE /api/products/:id</button>
            </div>
        </div>

        <div class="card">
            <h3>Live Response</h3>
            <pre id="output">Click a button above to invoke WebAssembly CRUD endpoints...</pre>
        </div>
    </div>

    <script>
        async function loadProducts() {
            const res = await fetch('/api/products');
            const data = res.status === 204 ? { success: true } : await res.json();
            document.getElementById('output').innerText = JSON.stringify(data, null, 2);
        }
        async function fetchProduct(id) {
            const res = await fetch('/api/products/' + id);
            const data = res.status === 204 ? { success: true } : await res.json();
            document.getElementById('output').innerText = JSON.stringify(data, null, 2);
        }
        async function createProduct() {
            const body = {
                name: document.getElementById('prodName').value,
                price: parseInt(document.getElementById('prodPrice').value, 10),
                stock: parseInt(document.getElementById('prodStock').value, 10)
            };
            const res = await fetch('/api/products', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const data = res.status === 204 ? { success: true } : await res.json();
            document.getElementById('output').innerText = JSON.stringify(data, null, 2);
        }
        async function updateProduct() {
            const id = document.getElementById('updateId').value;
            const body = {
                name: document.getElementById('updateName').value,
                price: parseInt(document.getElementById('updatePrice').value, 10),
                stock: parseInt(document.getElementById('updateStock').value, 10)
            };
            const res = await fetch('/api/products/' + id, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const data = res.status === 204 ? { success: true } : await res.json();
            document.getElementById('output').innerText = JSON.stringify(data, null, 2);
        }
        async function deleteProduct() {
            const id = document.getElementById('updateId').value;
            const res = await fetch('/api/products/' + id, { method: 'DELETE' });
            const data = res.status === 204 ? { success: true } : await res.json();
            document.getElementById('output').innerText = JSON.stringify(data, null, 2);
        }
        loadProducts();
    </script>
</body>
</html>`);
            return;
        }

        web.handle(req, res, url);
    });

    return new Promise((resolve, reject) => {
        server.once('error', reject);
        server.listen(PORT, () => {
            const port = server.address().port;
            console.log(`\n======================================================`);
            console.log(` Spring API subset on WebAssembly`);
            console.log(` Web UI:   http://localhost:${port}/`);
            console.log(` REST API: http://localhost:${port}/api/products`);
            console.log(` Runtime:  Node.js + WebAssembly (javaw engine)`);
            console.log(`======================================================\n`);
            resolve(server);
        });
    });
}

if (require.main === module) {
    startServer().catch(error => { console.error(error); process.exitCode = 1; });
}

module.exports = { startServer };
