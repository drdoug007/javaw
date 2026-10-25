#!/usr/bin/env python3
"""Generate Character properties from Java 17 (Unicode 13), independently of the host JDK."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java-home', type=Path, required=True)
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
source = '''
public class PropertyData {
    static int flags(int c) {
        int f=Character.getType(c);
        if(Character.isLowerCase(c))f|=32;
        if(Character.isUpperCase(c))f|=64;
        if(Character.isAlphabetic(c))f|=128;
        if(Character.isJavaIdentifierStart(c))f|=256;
        if(Character.isJavaIdentifierPart(c))f|=512;
        if(Character.isUnicodeIdentifierStart(c))f|=1024;
        if(Character.isUnicodeIdentifierPart(c))f|=2048;
        return f;
    }
    public static void main(String[] args) {
        if(Runtime.version().feature()!=17)throw new IllegalArgumentException("JDK 17 required");
        int bmp=1;
        for(int c=0;c<=65535;c++){char v=(char)c;
            int f=Character.getType(v);if(Character.isDefined(v))f|=32;if(Character.isDigit(v))f|=64;
            if(Character.isLetter(v))f|=128;if(Character.isLetterOrDigit(v))f|=256;
            if(Character.isLowerCase(v))f|=512;if(Character.isUpperCase(v))f|=1024;
            if(Character.isJavaIdentifierStart(v))f|=2048;if(Character.isJavaIdentifierPart(v))f|=4096;
            if(Character.isSpaceChar(v))f|=8192;if(Character.isISOControl(v))f|=16384;
            bmp=31*bmp+f;bmp=31*bmp+Character.getNumericValue(v);bmp=31*bmp+Character.digit(v,36);
        }
        for(int c:new int[]{-1,Integer.MIN_VALUE,Integer.MAX_VALUE,0x110000,0x104a5,0x1d7d8,0x216c,0x00bd,0x1f600}){
            for(int r=0;r<=38;r++)bmp=31*bmp+Character.digit(c,r);
            bmp=31*bmp+(Character.isDefined(c)?1:0)+(Character.isDigit(c)?2:0)+(Character.isLetter(c)?4:0)+(Character.isLetterOrDigit(c)?8:0)+(Character.isSpaceChar(c)?16:0)+(Character.isISOControl(c)?32:0);
        }
        System.err.println("JDK 17 BMP/digit checksum: "+bmp);
        int start=0,previous=flags(0),number=Character.getNumericValue(0),hash=1;
        for(int c=-1;c<=0x110000;c++){hash=31*hash+flags(c);hash=31*hash+Character.getNumericValue(c);}
        System.err.println("JDK 17 properties checksum: "+hash);
        for(int c=1;c<=0x110000;c++){
            int f=c==0x110000?-1:flags(c),n=Character.getNumericValue(c);
            if(f!=previous || n!=number){System.out.println(start+","+(c-1)+","+previous+","+number);start=c;previous=f;number=n;}
        }
    }
}
'''
with tempfile.TemporaryDirectory(prefix='javaw-properties-') as temp:
    root = Path(temp)
    (root / 'PropertyData.java').write_text(source)
    subprocess.run([str(args.java_home / 'bin/javac'), str(root / 'PropertyData.java')], check=True)
    data = subprocess.check_output([str(args.java_home / 'bin/java'), '-cp', temp, 'PropertyData'], text=True)
rows = [tuple(map(int, line.split(','))) for line in data.splitlines()]
def unit(value):
    return ('\\%03o' % value) if value < 256 else ('\\u%04x' % value)
output = '''package javaw.runtime;

/** Generated Java 17 / Unicode 13 Character properties. See tools/generate-unicode-properties.py.
 * Range records pack inclusive start/end, property bits and numeric value as UTF-16 int pairs.
 * Chunks avoid the class-file UTF-8 constant size limit and large initializer methods.
 */
public final class UnicodeProperties {
    private UnicodeProperties(){}
    private static final String[] DATA={
'''
for start in range(0, len(rows), 128):
    chunk=rows[start:start+128]
    for i in range(0,len(chunk),4):
        packed=''.join(unit((v>>shift)&65535) for row in chunk[i:i+4] for v in row for shift in (16,0))
        output+='        "'+packed+('"+' if i+4<len(chunk) else '",')+'\n'
output+='''    };
    private static int number(int row,int column){String data=DATA[row>>>7];int index=(row&127)*8+column*2;return (data.charAt(index)<<16)|data.charAt(index+1);}
    private static int find(int code){
        if(code<0 || code>0x10ffff)return -1;
        int low=0,high=COUNT-1;
        while(low<=high){int mid=(low+high)>>>1;if(code<number(mid,0))high=mid-1;else if(code>number(mid,1))low=mid+1;else return mid;}
        return -1;
    }
    public static int flags(int code){int row=find(code);return row<0?0:number(row,2);}
    public static int numeric(int code){int row=find(code);return row<0?-1:number(row,3);}
}
'''.replace('COUNT',str(len(rows)))
target=Path(__file__).resolve().parents[1]/'src/main/resources/exception-runtime/javaw/runtime/UnicodeProperties.java'
if args.check:
    if target.read_text()!=output:raise SystemExit('Unicode properties differ; regenerate and review')
else:target.write_text(output)
print(f'Java 17 properties: {len(rows)} ranges')
