'use strict';
// Persistent Node host for the bytecode tests (NodeHost.java). Each request runs one test script in a fresh worker
// thread, so a test JVM pays Node's process start-up once instead of once per script. A worker has its own V8 isolate,
// globals and module cache, and process.argv and process.exit behave as they do in a process.
//
// Protocol: one request per stdin line, tab-separated "id timeoutMs stackSizeMb script arg...", where the script and
// each argument are base64-encoded UTF-8; one reply per stdout line, "id code output" with the output base64-encoded.
// Script output never reaches this process's stdout, which carries only replies.
const {Worker} = require('node:worker_threads');
const readline = require('node:readline');

function reply(message) {
    process.stdout.write(`${message.id}\t${message.code}\t${Buffer.from(message.output, 'utf8').toString('base64')}\n`);
}

function parse(line) {
    const [id, timeoutMs, stackSizeMb, script, ...args] = line.split('\t');
    const text = value => Buffer.from(value, 'base64').toString('utf8');
    return {id, timeoutMs: Number(timeoutMs), stackSizeMb: Number(stackSizeMb), script: text(script), args: args.map(text)};
}

function run(request) {
    const output = [];
    let worker;
    try {
        worker = new Worker(request.script, {
            argv: request.args,
            stdout: true,
            stderr: true,
            // The default worker stack is about four times the main thread's; match the main thread's recursion depth.
            resourceLimits: {stackSizeMb: request.stackSizeMb},
        });
    } catch (error) {
        reply({id: request.id, code: 1, output: String(error && error.stack || error)});
        return;
    }
    // Reply only after the exit code and both output streams are complete.
    let pending = 3, code = 1, failure = false, timedOut = false;
    const finish = () => {
        if (--pending === 0) reply({id: request.id, code: timedOut ? 124 : failure && code === 0 ? 1 : code, output: output.join('')});
    };
    for (const stream of [worker.stdout, worker.stderr]) {
        stream.setEncoding('utf8');
        stream.on('data', text => output.push(text));
        stream.on('end', finish);
    }
    const timer = setTimeout(() => {
        timedOut = true;
        output.push(`\nTimed out after ${request.timeoutMs} ms\n`);
        worker.terminate();
    }, request.timeoutMs);
    // A process prints an uncaught exception to stderr and exits with code 1; a worker reports it here instead.
    worker.on('error', error => {
        failure = true;
        output.push((error && error.stack || String(error)) + '\n');
    });
    worker.on('exit', exitCode => {
        clearTimeout(timer);
        code = exitCode;
        finish();
    });
}

readline.createInterface({input: process.stdin}).on('line', line => run(parse(line))).on('close', () => process.exit(0));
