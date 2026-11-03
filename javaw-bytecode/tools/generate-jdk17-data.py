#!/usr/bin/env python3
"""Generate the Java 17 / Unicode 13 data the bundled runtime and its tests need, under target/.

The build runs this in the generate-resources phase. The tables are derived from a JDK 17 installation, never
committed: runtime sources go to --main-resources (exception-runtime/javaw/runtime/*.java), the test oracle to
--test-resources. Outputs are regenerated only when a generator script or the JDK 17 installation changes.
"""
import argparse
import concurrent.futures
import hashlib
import os
from pathlib import Path
import subprocess
import sys

TOOLS = Path(__file__).resolve().parent
# (generator, resource root, path below that root)
GENERATED = [
    ('generate-unicode-case.py', 'main', 'exception-runtime/javaw/runtime/UnicodeCase.java'),
    ('generate-unicode-properties.py', 'main', 'exception-runtime/javaw/runtime/UnicodeProperties.java'),
    ('generate-string-uppercase.py', 'main', 'exception-runtime/javaw/runtime/StringUpperData.java'),
    ('generate-string-lowercase.py', 'main', 'exception-runtime/javaw/runtime/StringLowerData.java'),
    ('generate-character-traversal.py', 'test', 'character-traversal-jdk17.txt'),
]


def jdk17(explicit):
    """The first JDK 17 among the explicit path, CI/Docker variables and the platform default."""
    candidates = [explicit] if explicit else []
    candidates += [os.environ.get(name) for name in ('JAVA_HOME_17', 'JAVA_HOME_17_X64', 'JAVA_HOME_17_ARM64')]
    if sys.platform == 'darwin':
        try:
            candidates.append(subprocess.check_output(['/usr/libexec/java_home', '-v', '17'], text=True,
                                                      stderr=subprocess.DEVNULL).strip())
        except (OSError, subprocess.CalledProcessError):
            pass
    for candidate in candidates:
        if not candidate:
            continue
        home = Path(candidate)
        java = home / 'bin' / 'java'
        if not java.is_file():
            continue
        version = subprocess.run([str(java), '-version'], capture_output=True, text=True).stderr
        if '"17' in version:
            return home, version
    raise SystemExit('A JDK 17 installation is required to generate the Unicode tables: pass -Djavaw.jdk17.home=<path> '
                     'or set JAVA_HOME_17 (checked: ' + ', '.join(c for c in candidates if c) + ')')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', default='', help='JDK 17 installation (default: discover)')
    parser.add_argument('--main-resources', type=Path, required=True)
    parser.add_argument('--test-resources', type=Path, required=True)
    args = parser.parse_args()
    home, version = jdk17(args.java_home)
    roots = {'main': args.main_resources, 'test': args.test_resources}
    stamp_text = hashlib.sha256('\n'.join([str(home), version] + [
        hashlib.sha256((TOOLS / script).read_bytes()).hexdigest() for script in [Path(__file__).name] + [g[0] for g in GENERATED]
    ]).encode()).hexdigest()
    stamp = args.main_resources.parent / '.jdk17-data.stamp'  # outside the resource root, so not packaged
    outputs = [roots[root] / path for _, root, path in GENERATED]
    if stamp.is_file() and stamp.read_text() == stamp_text and all(output.is_file() for output in outputs):
        print('Java 17 data tables are up to date')
        return

    def run(entry):
        script, root, path = entry
        result = subprocess.run([sys.executable, str(TOOLS / script), '--java-home', str(home), '--output', str(roots[root] / path)],
                                capture_output=True, text=True)
        if result.returncode != 0:
            raise SystemExit(f'{script} failed:\n{result.stdout}{result.stderr}')
        return f'{script}: {result.stdout.strip()}'

    with concurrent.futures.ThreadPoolExecutor() as pool:
        for line in pool.map(run, GENERATED):
            print(line)
    stamp.parent.mkdir(parents=True, exist_ok=True)
    stamp.write_text(stamp_text)


if __name__ == '__main__':
    main()
