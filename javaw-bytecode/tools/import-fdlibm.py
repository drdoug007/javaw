#!/usr/bin/env python3
"""Reproduce the selected fdlibm sources from the pinned OpenJDK source archive."""
import argparse
import hashlib
from pathlib import Path
import re
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('src_zip', type=Path)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    module = Path(__file__).resolve().parents[1]
    resources = module / 'src/main/resources/exception-runtime'
    notice = (resources / 'legal/fdlibm/NOTICE').read_text()
    expected_hash = re.search(r'Source SHA-256: ([0-9a-f]{64})', notice).group(1)
    with zipfile.ZipFile(args.src_zip) as archive:
        raw = archive.read('java.base/java/lang/FdLibm.java')
    if hashlib.sha256(raw).hexdigest() != expected_hash:
        raise ValueError('Source differs from the pinned OpenJDK 21.0.2 fdlibm source')
    source = raw.decode('utf-8')
    # Mask comments and literals so braces inside them cannot affect extraction.
    masked = re.sub(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\x27(?:\\.|[^\x27\\])*\x27',
                    lambda match: ' ' * len(match[0]), source)
    result = source[:source.index('    /** sin(x)')].replace('Double.POSITIVE_INFINITY', '(1.0/0.0)')
    result = result.replace('package java.lang;',
        '// Javaw adaptation, 2026-10-26: selected algorithms; inline unavailable Double\n'
        '// constants; route internal StrictMath calls through portable Math adapters.\n'
        '// The original GPLv2 with Classpath exception continues to apply.\n'
        'package java.lang;')
    for name in ['Exp', 'Log', 'Log10', 'Pow', 'Sin', 'Cos', 'RemPio2', 'KernelRemPio2', 'Tan', 'Asin', 'Acos', 'Atan', 'Atan2']:
        start = source.index('    static final class ' + name + ' ')
        cursor = masked.index('{', start) + 1
        depth = 1
        while depth:
            if masked[cursor] == '{':
                depth += 1
            elif masked[cursor] == '}':
                depth -= 1
            cursor += 1
        result += source[start:cursor].replace('Double.MAX_VALUE', '0x1.fffffffffffffp1023').replace('StrictMath.', 'Math.') + '\n'
    result += '}\n'
    destination = resources / 'java/lang/FdLibm.java'
    if args.check:
        if destination.read_text() != result:
            raise ValueError('Bundled fdlibm source differs from the reproducible selection')
        print('Pinned fdlibm selection matches')
    else:
        destination.write_text(result)


if __name__ == '__main__':
    main()
