#!/usr/bin/env python3
"""Summarize per-test wall and bytecode-compile times recorded by BytecodeTestTimings.

The bytecode test build writes one TSV per test class to javaw-bytecode/target/test-timings
(class, test, outcome, wall ms, compile ms, compiles). Each class also has a "(class)" row
covering the whole class; time outside its tests is that row minus the tests.
"""
import argparse
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent


def load(directory):
    tests, classes = [], {}
    for path in sorted(directory.glob('*.tsv')):
        for line in path.read_text(encoding='utf-8').splitlines():
            if not line:
                continue
            type_name, test, outcome, wall, compile_ms, compiles = line.split('\t')
            row = {'class': type_name.rsplit('.', 1)[-1], 'test': test, 'outcome': outcome,
                   'wall': int(wall), 'compile': int(compile_ms), 'compiles': int(compiles)}
            if test == '(class)':
                classes[row['class']] = row
            else:
                tests.append(row)
    for name, total in classes.items():
        inner = [row for row in tests if row['class'] == name]
        total['outside'] = total['wall'] - sum(row['wall'] for row in inner)
        total['outsideCompile'] = total['compile'] - sum(row['compile'] for row in inner)
        total['tests'] = len(inner)
    return tests, classes


def seconds(ms):
    return f'{ms / 1000:.1f}'


def report(tests, classes, top):
    wall = sum(row['wall'] for row in classes.values())
    compile_ms = sum(row['compile'] for row in classes.values())
    compiles = sum(row['compiles'] for row in classes.values())
    lines = ['## Bytecode test timings', '',
             f'{len(tests)} tests in {len(classes)} classes: {seconds(wall)} s of test-class time, '
             f'{seconds(compile_ms)} s ({100 * compile_ms / max(wall, 1):.0f}%) in {compiles} bytecode compilations '
             f'(mean {compile_ms / max(compiles, 1) / 1000:.2f} s). Class times add up across forks.', '',
             f'### Slowest {top} test classes', '',
             '| Class | Tests | Wall s | Compile s | Compiles | Outside tests s |', '| --- | ---: | ---: | ---: | ---: | ---: |']
    for row in sorted(classes.values(), key=lambda row: -row['wall'])[:top]:
        lines.append(f"| {row['class']} | {row['tests']} | {seconds(row['wall'])} | {seconds(row['compile'])} | "
                     f"{row['compiles']} | {seconds(row['outside'])} |")
    lines += ['', f'### Slowest {top} tests by compile time', '',
              '| Test | Wall s | Compile s | Compiles |', '| --- | ---: | ---: | ---: |']
    for row in sorted(tests, key=lambda row: -row['compile'])[:top]:
        lines.append(f"| {row['class']}.{row['test']} | {seconds(row['wall'])} | {seconds(row['compile'])} | {row['compiles']} |")
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--dir', type=pathlib.Path, default=ROOT / 'target' / 'test-timings')
    parser.add_argument('--top', type=int, default=15)
    parser.add_argument('--markdown', type=pathlib.Path, help='append the report (e.g. $GITHUB_STEP_SUMMARY)')
    parser.add_argument('--json', type=pathlib.Path, help='write every row as JSON')
    parser.add_argument('--warn-compile-seconds', type=float,
                        help='emit a GitHub warning for each test whose compile time exceeds this')
    args = parser.parse_args()
    tests, classes = load(args.dir)
    if not classes:
        # Not an error: a failed build may stop before any class finishes, and that failure is reported already.
        print(f'no timing files in {args.dir}; run the bytecode tests first', file=sys.stderr)
        return
    text = report(tests, classes, args.top)
    print(text)
    if args.markdown:
        with args.markdown.open('a', encoding='utf-8') as out:
            out.write(text)
    if args.json:
        args.json.write_text(json.dumps({'tests': tests, 'classes': list(classes.values())}, indent=2) + '\n', encoding='utf-8')
    if args.warn_compile_seconds is not None:
        for row in tests:
            if row['compile'] > args.warn_compile_seconds * 1000:
                print(f"::warning title=Slow bytecode compile::{row['class']}.{row['test']} spent "
                      f"{seconds(row['compile'])} s in {row['compiles']} compilations")


if __name__ == '__main__':
    main()
