#!/usr/bin/env python3
"""Generate simple Unicode case tables from the Java 17 Character contract."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java-home', type=Path, required=True, help='JDK 17 installation')
parser.add_argument('--check', action='store_true')
parser.add_argument('--output',type=Path,required=True,help='file to write (under target/)')
args = parser.parse_args()
source = '''
public class CaseData {
    public static void main(String[] args) {
        if (Runtime.version().feature() != 17) throw new IllegalArgumentException("JDK 17 required");
        int upper=1,lower=1,chars=1;
        for(int cp=-1;cp<=0x110000;cp++){upper=31*upper+Character.toUpperCase(cp);lower=31*lower+Character.toLowerCase(cp);}
        for(int cp=0;cp<=65535;cp++){chars=31*chars+Character.toUpperCase((char)cp);chars=31*chars+Character.toLowerCase((char)cp);}
        System.err.println("JDK 17 whole-domain checksums: "+upper+" "+lower+" "+chars);
        for (int kind = 0; kind < 2; kind++) {
            int start = -1, end = -1, delta = 0;
            for (int cp = 0; cp <= 0x110000; cp++) {
                int mapped = cp == 0x110000 ? cp : kind == 0 ? Character.toUpperCase(cp) : Character.toLowerCase(cp);
                int next = mapped - cp;
                if (start >= 0 && (cp != end + 1 || next != delta)) {
                    System.out.println(kind + "," + start + "," + end + "," + delta);
                    start = -1;
                }
                if (next != 0) { if (start < 0) { start = cp; delta = next; } end = cp; }
            }
        }
    }
}
'''
with tempfile.TemporaryDirectory(prefix='javaw-case-') as temp:
    root = Path(temp)
    (root / 'CaseData.java').write_text(source)
    subprocess.run([str(args.java_home / 'bin/javac'), str(root / 'CaseData.java')], check=True)
    data = subprocess.check_output([str(args.java_home / 'bin/java'), '-cp', temp, 'CaseData'], text=True)
tables = [[], []]
for line in data.splitlines():
    kind, start, end, delta = map(int, line.split(','))
    tables[kind].append((start, end, delta))
def unit(value):
    return ('\\%03o' % value) if value < 256 else ('\\u%04x' % value)

output = '''package javaw.runtime;

/** Generated Java 17 / Unicode 13 simple case mappings. Do not edit by hand.
 * Regenerate with tools/generate-unicode-case.py and a JDK 17 installation.
 * Each triple is an inclusive code-point range and its signed mapping delta.
 * Ints are packed as two UTF-16 units to avoid a large array-initializer method.
 */
public final class UnicodeCase {
    private UnicodeCase(){}
'''
for name, rows in zip(('UPPER', 'LOWER'), tables):
    chunks = []
    for row in rows:
        chunks.append(''.join(unit((value >> shift) & 65535) for value in row for shift in (16, 0)))
    output += f'    private static final String {name}=\n' + '\n'.join(
        '        "' + ''.join(chunks[i:i+8]) + ('";' if i+8>=len(chunks) else '" +')
        for i in range(0, len(chunks), 8)) + '\n'
output += '''    private static int number(String data,int index){return (data.charAt(index)<<16)|data.charAt(index+1);}
    private static int convert(int code,String ranges){
        int low=0,high=ranges.length()/6-1;
        while(low<=high){int middle=(low+high)>>>1,index=middle*6;
            if(code<number(ranges,index))high=middle-1;
            else if(code>number(ranges,index+2))low=middle+1;
            else return code+number(ranges,index+4);
        }return code;
    }
    public static int upper(int code){return convert(code,UPPER);}
    public static int lower(int code){return convert(code,LOWER);}
}
'''
target=args.output
target.parent.mkdir(parents=True,exist_ok=True)
if args.check:
    if target.read_text() != output:
        raise SystemExit('Unicode case tables differ; regenerate and review')
else:
    target.write_text(output)
print(f'Java 17 case tables: {len(tables[0])} upper and {len(tables[1])} lower ranges')
