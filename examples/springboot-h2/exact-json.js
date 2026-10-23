// Preserve JSON number lexemes until the Wasm parameter type is known.
const decimalPattern = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?$/;
class ExactNumber {
    constructor(text) {
        if (!decimalPattern.test(text)) throw new SyntaxError('Invalid JSON number');
        this.text = text;
    }
}
function parseExactJSON(text) {
    JSON.parse(text); // Validate syntax before preserving numeric tokens.
    const tokens = text.match(/"(?:[^"\\\u0000-\u001f]|\\(?:["\\/bfnrt]|u[\da-fA-F]{4}))*"|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?|true|false|null|[{}\[\],:]/g);
    let index = 0;
    function read() {
        const token = tokens[index++];
        if (token === '[') {
            const result = [];
            if (tokens[index] !== ']') do { result.push(read()); } while (tokens[index++] === ',');
            else index++;
            return result;
        }
        if (token === '{') {
            const entries = [];
            if (tokens[index] !== '}') do {
                const key = JSON.parse(tokens[index++]); index++; entries.push([key, read()]);
            } while (tokens[index++] === ',');
            else index++;
            return Object.fromEntries(entries);
        }
        if (token[0] === '"' || token === 'true' || token === 'false' || token === 'null') return JSON.parse(token);
        return new ExactNumber(token);
    }
    return read();
}
function stringifyExactJSON(value) {
    if (value instanceof ExactNumber) return value.text;
    if (Array.isArray(value)) return '[' + value.map(stringifyExactJSON).join(',') + ']';
    if (value !== null && typeof value === 'object') {
        return '{' + Object.entries(value).map(([key, item]) => JSON.stringify(key) + ':' + stringifyExactJSON(item)).join(',') + '}';
    }
    return JSON.stringify(value);
}
module.exports = { ExactNumber, parseExactJSON, stringifyExactJSON };
