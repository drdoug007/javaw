#!/usr/bin/env python3
"""Extract reproducible Java 17 ROOT word-boundary data for String lowercase contexts."""
import argparse,subprocess,tempfile,zipfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--java-home',type=Path,required=True);p.add_argument('--check',action='store_true');a=p.parse_args()
source='''import java.text.*;import java.util.*;import java.lang.reflect.*;
public class LowerData {
 static Object field(Object b,String name)throws Exception{Field f=b.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(b);}
 public static void main(String[] args)throws Exception{
  if(Runtime.version().feature()!=17)throw new IllegalArgumentException("JDK 17 required");
  BreakIterator b=BreakIterator.getWordInstance(Locale.ROOT);
  Method category=b.getClass().getDeclaredMethod("lookupCategory",int.class);category.setAccessible(true);
  System.out.println("N,"+field(b,"numCategories"));
  for(short state:(short[])field(b,"stateTable"))System.out.println("S,"+state);
  for(short state:(short[])field(b,"backwardsStateTable"))System.out.println("B,"+state);
  boolean[] end=(boolean[])field(b,"endStates"),look=(boolean[])field(b,"lookaheadStates");
  for(int i=0;i<end.length;i++)System.out.println("F,"+((end[i]?1:0)|(look[i]?2:0)));
  for(String lang:new String[]{"en","de","fr","it","ja","zh","ko","tr","az","lt","ar","ru","el","lo"}){
   BreakIterator other=BreakIterator.getWordInstance(new Locale(lang));
   if(other.getClass()!=b.getClass() || !field(other,"checksum").equals(field(b,"checksum")))throw new IllegalArgumentException("Different word data: "+lang);
  }
  int start=0,previous=(int)category.invoke(b,0);
  for(int cp=1;cp<=0x110000;cp++){
   int next=cp==0x110000?-2:(int)category.invoke(b,cp);
   if(next!=previous){System.out.println("C,"+start+","+(cp-1)+","+previous);start=cp;previous=next;}
  }
 }}'''
with tempfile.TemporaryDirectory() as d:
 f=Path(d)/'LowerData.java';f.write_text(source)
 subprocess.run([str(a.java_home/'bin/javac'),str(f)],check=True)
 data=subprocess.check_output([str(a.java_home/'bin/java'),'--add-opens','java.base/sun.text=ALL-UNNAMED','-Djava.locale.providers=CLDR','-cp',d,'LowerData'],text=True)
rows=[];states=[];flags=[];back=[]
for line in data.splitlines():
 kind,*v=line.split(',');v=list(map(int,v))
 if kind=='N':categories=v[0]
 elif kind=='S':states+=v
 elif kind=='F':flags+=v
 elif kind=='B':back+=v
 else:rows.append(v)
with zipfile.ZipFile(a.java_home/'lib/src.zip') as z:rules=z.read('java.base/sun/text/resources/BreakIteratorRules.java').decode()
header=rules[:rules.index('package ')]
def unit(c):return '\\%03o'%c if c<256 else '\\u%04x'%c
def packed(values):return ''.join(unit((v>>shift)&65535) for v in values for shift in (16,0))
out=header+'''package javaw.runtime;
/** Generated Java 17 ROOT word-break tables. See tools/generate-string-lowercase.py
 * and legal/word-break for upstream source, license and reproduction notes.
 * Each category range packs inclusive start, end and category as UTF-16 int pairs.
 */
public final class StringLowerData {
 private StringLowerData(){}
'''
out+=' private static final String STATES="'+''.join(unit(v) for v in states)+'";\n'
out+=' private static final String BACK="'+''.join(unit(v) for v in back)+'";\n'
out+=' private static final String FLAGS="'+''.join(unit(v) for v in flags)+'";\n'
out+=' private static final String[] RANGES={\n'
for i in range(0,len(rows),128):out+='  "'+packed(v for row in rows[i:i+128] for v in row)+'",\n'
out+=''' };
 private static int number(int row,int column){String s=RANGES[row>>>7];int i=(row&127)*6+column*2;return(s.charAt(i)<<16)|s.charAt(i+1);}
 public static int category(int cp){int low=0,high=COUNT-1;while(low<=high){int m=(low+high)>>>1;if(cp<number(m,0))high=m-1;else if(cp>number(m,1))low=m+1;else return number(m,2);}return -1;}
 public static int state(int state,int category){return STATES.charAt(state*CATEGORIES+category);}
 public static int backward(int state,int category){return BACK.charAt(state*CATEGORIES+category);}
 public static boolean end(int state){return(FLAGS.charAt(state)&1)!=0;}
 public static boolean look(int state){return(FLAGS.charAt(state)&2)!=0;}
}
'''.replace('COUNT',str(len(rows))).replace('CATEGORIES',str(categories))
target=Path(__file__).resolve().parents[1]/'src/main/resources/exception-runtime/javaw/runtime/StringLowerData.java'
if a.check:
 if target.read_text()!=out:raise SystemExit('Lowercase word tables differ; regenerate and review')
else:target.write_text(out)
print(f'{len(rows)} category ranges, {len(states)} transitions, {len(flags)} states')
