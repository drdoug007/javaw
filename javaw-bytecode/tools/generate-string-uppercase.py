#!/usr/bin/env python3
"""Generate Java 17 String full uppercase expansions and canonical combining classes."""
import argparse,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--check',action='store_true');p.add_argument('--output',type=Path,required=True,help='file to write (under target/)');a=p.parse_args()
source='''import java.util.Locale;
public class UpperData { public static void main(String[] args) {
 if(Runtime.version().feature()!=17)throw new IllegalArgumentException("JDK 17 required");
 for(int cp=0;cp<=0x10ffff;cp++) {
  String s=new String(Character.toChars(cp)),u=s.toUpperCase(Locale.ROOT);
  if(!u.equals(new String(Character.toChars(Character.toUpperCase(cp))))) {
   System.out.print("E,"+cp);for(int i=0;i<u.length();i++)System.out.print(","+(int)u.charAt(i));System.out.println();
  }
  if(u.equals(s) && u!=s)System.out.println("I,"+cp);
  int cc=sun.text.Normalizer.getCombiningClass(cp);if(cc!=0)System.out.println("C,"+cp+","+cc);
 }
}}'''
with tempfile.TemporaryDirectory() as d:
 f=Path(d)/'UpperData.java';f.write_text(source)
 flags=['--add-exports','java.base/sun.text=ALL-UNNAMED']
 subprocess.run([str(a.java_home/'bin/javac'),*flags,str(f)],check=True)
 data=subprocess.check_output([str(a.java_home/'bin/java'),*flags,'-cp',d,'UpperData'],text=True)
exp=[];ranges=[];identities=[]
for line in data.splitlines():
 kind,*v=line.split(',');v=list(map(int,v))
 if kind=='I':identities.append(v[0])
 elif kind=='E':exp.append(v)
 elif ranges and ranges[-1][1]+1==v[0] and ranges[-1][2]==v[1]:ranges[-1][1]=v[0]
 else:ranges.append([v[0],v[0],v[1]])
def unit(c):return '\\%03o'%c if c<256 else '\\u%04x'%c
out='''package javaw.runtime;
/** Generated from Java 17 / Unicode 13. See tools/generate-string-uppercase.py. */
public final class StringUpperData {
 private StringUpperData(){}
'''
ir=[]
for c in identities:
 if ir and ir[-1][1]+1==c:ir[-1][1]=c
 else:ir.append([c,c])
out+=' public static boolean copiesUnchanged(int cp){return '+(' || '.join('(cp>='+str(x)+' && cp<='+str(y)+')' for x,y in ir) or 'false')+';}\n'
out+=' private static final int[] KEYS={'+','.join(str(e[0]) for e in exp)+'};\n'
out+=' private static final String[] VALUES={'+','.join('"'+''.join(map(unit,e[1:]))+'"' for e in exp)+'};\n'
out+=' private static final int[] CLASSES={'+','.join(str(v) for row in ranges for v in row)+'};\n'
out+=''' public static String expansion(int cp){int low=0,high=KEYS.length-1;while(low<=high){int m=(low+high)>>>1;if(cp<KEYS[m])high=m-1;else if(cp>KEYS[m])low=m+1;else return VALUES[m];}return null;}
 public static int combining(int cp){int low=0,high=CLASSES.length/3-1;while(low<=high){int m=(low+high)>>>1,i=m*3;if(cp<CLASSES[i])high=m-1;else if(cp>CLASSES[i+1])low=m+1;else return CLASSES[i+2];}return 0;}
}
'''
target=a.output
target.parent.mkdir(parents=True,exist_ok=True)
if a.check:
 if target.read_text()!=out:raise SystemExit('Uppercase tables differ')
else:target.write_text(out)
print(f'{len(exp)} expansions, {len(ranges)} combining-class ranges')
