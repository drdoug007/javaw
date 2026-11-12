#!/usr/bin/env python3
"""Verify the library lock and inventory the real runtime dependency closure."""
import argparse
import collections
import csv
import json
from pathlib import Path
import os
import subprocess
import sys


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--online', action='store_true', help='allow Maven downloads')
    baseline = parser.add_mutually_exclusive_group()
    baseline.add_argument('--check-baseline', action='store_true', help='fail if the committed inventory summary has drifted')
    baseline.add_argument('--update-baseline', action='store_true', help='write a reviewed inventory summary to the committed baseline')
    args = parser.parse_args()
    module = Path(__file__).resolve().parent
    root = module.parent
    subprocess.run([sys.executable, str(module / 'verify-lock.py')] + (['--online'] if args.online else []), check=True)
    offline = [] if args.online else ['-o']
    with (module / 'target' / 'audit-build.log').open('w') as log:
        # One reactor invocation, so reactor modules (javaw-core, javaw-unicode-data) resolve to their target/classes
        # instead of whatever is installed in ~/.m2. Each module writes the classpath file; javaw-bytecode's is last.
        classpath = root / 'javaw-bytecode' / 'target' / 'audit-classpath.txt'
        subprocess.run(['mvn'] + offline + ['-pl', 'javaw-bytecode', '-am', '-DskipTests', 'compile',
                       'org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath',
                       f'-Dmdep.outputFile={classpath}', '-Dmdep.includeScope=runtime'],
                       cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)
    output = module / 'target' / 'audit'
    subprocess.run(['java', '-Xmx1g', '-cp', str(root / 'javaw-bytecode' / 'target' / 'classes')
                    + os.pathsep + classpath.read_text().strip(),
                    'one.dastec.javaw.bytecode.DependencyAudit', str(module / 'target' / 'audit-manifest.tsv'),
                    str(output), '17', 'org/h2/Driver', 'org/springframework/boot/SpringApplication',
                    'org/hibernate/cfg/Configuration'], cwd=root, check=True)
    def counts(filename, key, category=None):
        result = collections.Counter()
        with (output / filename).open() as stream:
            for row in csv.DictReader(stream, delimiter='\t'):
                if category is None or row['category'] == category:
                    result[row[key]] += int(row['count'])
        return dict(sorted(result.items()))
    with (output / 'summary.tsv').open() as stream:
        summary = {row['metric']: row['value'] for row in csv.DictReader(stream, delimiter='\t')}
    report = {'schemaVersion': 1, 'summary': summary,
              'unsupportedOpcodeOccurrences': counts('unsupported-opcodes.tsv', 'name'),
              'normalizedOpcodeOccurrences': counts('normalized-opcodes.tsv', 'name'),
              'requirementOccurrences': counts('requirements.tsv', 'category'),
              'bootstrapOccurrences': counts('requirements.tsv', 'symbol', 'invokedynamic-bootstrap')}
    encoded = json.dumps(report, indent=2, sort_keys=True) + '\n'
    (output / 'summary.json').write_text(encoded)
    committed = module / 'audit-baseline.json'
    if args.check_baseline and committed.read_text() != encoded:
        raise ValueError('audit summary drifted; review the reports before --update-baseline')
    if args.update_baseline:
        committed.write_text(encoded)
    print((output / 'summary.tsv').read_text())
    print(f'Full inventory: {output}')


if __name__ == '__main__':
    main()
