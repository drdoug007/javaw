# Vendored Node test dependencies

Test-only JavaScript libraries, committed so the offline build (`mvn -o`) can run DOM tests without network access. Nothing here is shipped in javaw artifacts or loaded by `javaw-runtime.js`.

| Package | Version | License | Why |
| --- | --- | --- | --- |
| `happy-dom` | 21.0.4 | MIT | DOM with custom elements, shadow DOM and constructable stylesheets for the web components tests ([docs/web-components.md](../../docs/web-components.md#validation-strategy)) |
| `entities` | 7.0.1 | BSD-2-Clause | happy-dom dependency |
| `whatwg-mimetype` | 3.0.0 | MIT | happy-dom dependency |
| `ws` | 8.23.0 | MIT | happy-dom dependency |
| `buffer-image-size` | 0.6.4 | MIT | happy-dom dependency |

Each package keeps its upstream `LICENSE` file under `node_modules/<package>/`. `package.json` pins the top-level version. `package-lock.json` records the exact resolved tree, including the types-only packages that were removed.

## Using it from a test

happy-dom is an ES module. Load it with a dynamic import of the absolute path:

```js
const { Window } = await import(require('node:url').pathToFileURL(vendorDir + '/node_modules/happy-dom/lib/index.js'));
const window = new Window();
```

## Updating

Run `./update.sh`. It needs network access, which tests never do. It installs `package.json` into an empty temporary directory with lifecycle scripts disabled. It then removes files that are not needed at runtime: TypeScript sources, source maps, declaration files, and the types-only `@types/*` and `undici-types` packages. Finally it replaces `node_modules/`. Review the diff and the licences before committing an update.

## Known happy-dom deviations

- **Upgrade without initial attribute reactions.** In 21.0.4, an element that is already in the document when `customElements.define` upgrades it gets no `attributeChangedCallback` for its existing observed attributes. Per the spec, those reactions run before `connectedCallback`. Elements that are parsed or created after `define` behave correctly. Tests that check upgrade-time attribute reactions must not rely on happy-dom alone; the opt-in Chromium smoke test covers them.
