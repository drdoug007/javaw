#!/usr/bin/env python3
"""Generate the Java 17 Character traversal oracle; newer JDK bounds checks differ."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java-home',type=Path,required=True)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
source='''
public class TraversalData {
    static int probe(int kind,int index,int end){
        char[] text={'a',0xd801,0xdc28,0xd800,'x',0xdc00,0xd800,0xd800,0xdc00};String s=new String(text);
        try{switch(kind){
            case 0:return Character.codePointAt(text,index,end);
            case 1:return Character.codePointBefore(text,index,end);
            case 2:return Character.codePointCount(text,index,end);
            case 3:return Character.codePointCount(s,index,end);
            case 4:return Character.codePointAt(text,index);
            case 5:return Character.codePointBefore(text,index);
            case 6:return Character.codePointAt(s,index);
            case 7:return Character.codePointBefore(s,index);
            case 8:return Character.codePointAt((char[])null,index,end);
            case 9:return Character.codePointBefore((char[])null,index,end);
            case 10:return Character.codePointCount((CharSequence)null,index,end);
            case 11:return Character.codePointAt((char[])null,index);
            case 12:return Character.codePointBefore((char[])null,index);
            default:throw new AssertionError();
        }}catch(NullPointerException e){return -1;}
          catch(ArrayIndexOutOfBoundsException e){return -2;}
          catch(StringIndexOutOfBoundsException e){return -3;}
          catch(IndexOutOfBoundsException e){return -4;}
    }
    public static void main(String[] args){
        if(Runtime.version().feature()!=17)throw new IllegalArgumentException("JDK 17 required");
        for(int kind=0;kind<13;kind++)for(int i=-1;i<=10;i++)for(int end=-1;end<=10;end++)
            System.out.println(kind+" "+i+" "+end+" "+probe(kind,i,end));
    }
}
'''
with tempfile.TemporaryDirectory(prefix='javaw-traversal-') as temp:
    root=Path(temp)
    (root/'TraversalData.java').write_text(source)
    subprocess.run([str(args.java_home/'bin/javac'),str(root/'TraversalData.java')],check=True)
    output=subprocess.check_output([str(args.java_home/'bin/java'),'-cp',temp,'TraversalData'],text=True)
target=Path(__file__).resolve().parents[1]/'src/test/resources/character-traversal-jdk17.txt'
if args.check:
    if target.read_text()!=output:raise SystemExit('Character traversal oracle differs; regenerate and review')
else:
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(output)
print(f'Java 17 Character traversal oracle: {len(output.splitlines())} cases')
