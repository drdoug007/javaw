#!/usr/bin/env python3
"""Resolve the pinned target, verify its exact runtime closure, and emit an audit manifest."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--online', action='store_true', help='allow Maven to download missing artifacts')
    args = parser.parse_args()
    module = Path(__file__).resolve().parent
    output = module / 'target'
    output.mkdir(exist_ok=True)
    listing = output / 'resolved-dependencies.txt'
    listing.unlink(missing_ok=True)
    command = ['mvn'] + ([] if args.online else ['-o']) + [
        '-pl', 'runtime-target', 'org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list',
        '-DincludeScope=runtime', '-DoutputAbsoluteArtifactFilename=true', f'-DoutputFile={listing}']
    with (output / 'resolve.log').open('w') as log:
        subprocess.run(command, cwd=module.parent, stdout=log, stderr=subprocess.STDOUT, check=True)
    text = re.sub(r'\x1b\[[0-9;]*m', '', listing.read_text())
    actual = {}
    pattern = r'^\s+([^: ]+):([^: ]+):jar:([^: ]+):(compile|runtime):(.+?\.jar)(?: -- module.*)?$'
    for line in text.splitlines():
        match = re.match(pattern, line)
        if match:
            group, artifact, version, _, path = match.groups()
            coordinate = f'{group}:{artifact}:{version}'
            if coordinate in actual:
                raise ValueError(f'duplicate resolved artifact: {coordinate}')
            actual[coordinate] = Path(path)
        elif ':jar:' in line:
            raise ValueError(f'unrecognized dependency entry: {line}')
    lock = json.loads((module / 'dependencies.lock.json').read_text())
    if lock['schemaVersion'] != 1:
        raise ValueError('unsupported lock schema')
    expected = {f"{item['groupId']}:{item['artifactId']}:{item['version']}": item['sha256']
                for item in lock['artifacts']}
    if len(expected) != len(lock['artifacts']) or not expected:
        raise ValueError('empty or duplicate lock entries')
    if actual.keys() != expected.keys():
        raise ValueError(f'closure differs: missing={sorted(expected.keys()-actual.keys())}, '
                         f'extra={sorted(actual.keys()-expected.keys())}')
    for coordinate, path in actual.items():
        with path.open('rb') as stream:
            hasher = hashlib.sha256()
            for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                hasher.update(chunk)
            digest = hasher.hexdigest()
        if digest != expected[coordinate]:
            raise ValueError(f'SHA-256 mismatch for {coordinate}')
    # Absolute local paths are generated only; the committed lock is machine independent.
    manifest = output / 'audit-manifest.tsv'
    manifest.write_text(''.join(f'{coordinate}\t{actual[coordinate]}\n' for coordinate in sorted(actual)))
    print(f'Verified {len(actual)} pinned runtime JARs; manifest: {manifest}')


if __name__ == '__main__':
    main()
