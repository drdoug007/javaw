package java.util;
public class BitSet implements Cloneable, java.io.Serializable {
 private long[] words;
 private int wordsInUse;
 private transient boolean sizeIsSticky;

 public BitSet(){words=new long[1];}
 public BitSet(int nbits){
     if(nbits<0)throw new NegativeArraySizeException("nbits < 0: "+nbits);
     words=new long[wordIndex(nbits-1)+1]; // ceil(nbits/64) without overflowing near Integer.MAX_VALUE
     sizeIsSticky=true;
 }
 private BitSet(long[] words){this.words=words;this.wordsInUse=words.length;}

 private static int wordIndex(int bit){return bit>>6;}
 private void ensureWords(int wordIndex){
     if(words.length<=wordIndex){words=Arrays.copyOf(words,Math.max(2*words.length,wordIndex+1));sizeIsSticky=false;}
 }
 private void recalculateWordsInUse(){
     while(wordsInUse>0&&words[wordsInUse-1]==0)wordsInUse--;
 }
 private void checkRange(int fromIndex,int toIndex){
     if(fromIndex<0)throw new IndexOutOfBoundsException("fromIndex < 0: "+fromIndex);
     if(toIndex<0)throw new IndexOutOfBoundsException("toIndex < 0: "+toIndex);
     if(fromIndex>toIndex)throw new IndexOutOfBoundsException("fromIndex: "+fromIndex+" > toIndex: "+toIndex);
 }

 public boolean get(int bitIndex){
     if(bitIndex<0)throw new IndexOutOfBoundsException("bitIndex < 0: "+bitIndex);
     int w=wordIndex(bitIndex);
     return w<wordsInUse&&(words[w]&(1L<<bitIndex))!=0;
 }
 public void set(int bitIndex){
     if(bitIndex<0)throw new IndexOutOfBoundsException("bitIndex < 0: "+bitIndex);
     int w=wordIndex(bitIndex);
     ensureWords(w);
     words[w]|=1L<<bitIndex;
     if(w+1>wordsInUse)wordsInUse=w+1;
 }
 public void set(int bitIndex,boolean value){
     if(value)set(bitIndex);else clear(bitIndex);
 }
 public void set(int fromIndex,int toIndex){
     checkRange(fromIndex,toIndex);
     if(fromIndex==toIndex)return;
     int startWord=wordIndex(fromIndex);
     int endWord=wordIndex(toIndex-1);
     ensureWords(endWord);
     if(startWord==endWord)words[startWord]|=(-1L<<fromIndex)&(-1L>>>-toIndex);
     else{
         words[startWord]|=-1L<<fromIndex;
         for(int w=startWord+1;w<endWord;w++)words[w]=-1L;
         words[endWord]|=-1L>>>-toIndex;
     }
     if(endWord+1>wordsInUse)wordsInUse=endWord+1;
 }
 public void clear(int bitIndex){
     if(bitIndex<0)throw new IndexOutOfBoundsException("bitIndex < 0: "+bitIndex);
     int w=wordIndex(bitIndex);
     if(w>=wordsInUse)return;
     words[w]&=~(1L<<bitIndex);
     recalculateWordsInUse();
 }
 public void clear(int fromIndex,int toIndex){
     checkRange(fromIndex,toIndex);
     if(fromIndex==toIndex)return;
     int startWord=wordIndex(fromIndex);
     if(startWord>=wordsInUse)return;
     if(wordIndex(toIndex-1)>=wordsInUse)toIndex=length();
     int endWord=wordIndex(toIndex-1);
     if(startWord==endWord)words[startWord]&=~((-1L<<fromIndex)&(-1L>>>-toIndex));
     else{
         words[startWord]&=~(-1L<<fromIndex);
         for(int w=startWord+1;w<endWord;w++)words[w]=0L;
         words[endWord]&=~(-1L>>>-toIndex);
     }
     recalculateWordsInUse();
 }
 public void clear(){
     for(int i=0;i<words.length;i++)words[i]=0L;
     wordsInUse=0;
 }
 public void flip(int bitIndex){
     if(bitIndex<0)throw new IndexOutOfBoundsException("bitIndex < 0: "+bitIndex);
     if(get(bitIndex))clear(bitIndex);else set(bitIndex);
 }

 public int nextSetBit(int fromIndex){
     if(fromIndex<0)throw new IndexOutOfBoundsException("fromIndex < 0: "+fromIndex);
     int w=wordIndex(fromIndex);
     if(w>=wordsInUse)return -1;
     long word=words[w]&(-1L<<fromIndex);
     while(word==0){
         w++;
         if(w>=wordsInUse)return -1;
         word=words[w];
     }
     return (w<<6)+Long.numberOfTrailingZeros(word);
 }
 public int nextClearBit(int fromIndex){
     if(fromIndex<0)throw new IndexOutOfBoundsException("fromIndex < 0: "+fromIndex);
     int w=wordIndex(fromIndex);
     if(w>=wordsInUse)return fromIndex;
     long word=~words[w]&(-1L<<fromIndex);
     while(word==0){
         w++;
         if(w>=wordsInUse)return wordsInUse<<6;
         word=~words[w];
     }
     return (w<<6)+Long.numberOfTrailingZeros(word);
 }
 public int previousSetBit(int fromIndex){
     if(fromIndex<-1)throw new IndexOutOfBoundsException("fromIndex < -1: "+fromIndex);
     if(fromIndex==-1)return -1;
     int w=wordIndex(fromIndex);
     if(w>=wordsInUse)return length()-1;
     long word=words[w]&(-1L>>>-(fromIndex+1));
     while(word==0){
         w--;
         if(w<0)return -1;
         word=words[w];
     }
     return (w+1)*64-1-Long.numberOfLeadingZeros(word);
 }
 public int previousClearBit(int fromIndex){
     if(fromIndex<-1)throw new IndexOutOfBoundsException("fromIndex < -1: "+fromIndex);
     if(fromIndex==-1)return -1;
     int w=wordIndex(fromIndex);
     if(w>=wordsInUse)return fromIndex;
     long word=~words[w]&(-1L>>>-(fromIndex+1));
     while(word==0){
         w--;
         if(w<0)return -1;
         word=~words[w];
     }
     return (w+1)*64-1-Long.numberOfLeadingZeros(word);
 }

 public void and(BitSet set){
     if(set==this)return;
     while(wordsInUse>set.wordsInUse)words[--wordsInUse]=0;
     for(int i=0;i<wordsInUse;i++)words[i]&=set.words[i];
     recalculateWordsInUse();
 }
 public void or(BitSet set){
     if(set==this)return;
     int common=Math.min(wordsInUse,set.wordsInUse);
     if(set.wordsInUse>wordsInUse){ensureWords(set.wordsInUse-1);wordsInUse=set.wordsInUse;}
     for(int i=0;i<common;i++)words[i]|=set.words[i];
     if(common<set.wordsInUse)System.arraycopy(set.words,common,words,common,set.wordsInUse-common);
 }
 public void xor(BitSet set){
     int common=Math.min(wordsInUse,set.wordsInUse);
     if(set.wordsInUse>wordsInUse){ensureWords(set.wordsInUse-1);wordsInUse=set.wordsInUse;}
     for(int i=0;i<common;i++)words[i]^=set.words[i];
     if(common<set.wordsInUse)System.arraycopy(set.words,common,words,common,set.wordsInUse-common);
     recalculateWordsInUse();
 }
 public void andNot(BitSet set){
     for(int i=0;i<Math.min(wordsInUse,set.wordsInUse);i++)words[i]&=~set.words[i];
     recalculateWordsInUse();
 }
 public boolean intersects(BitSet set){
     for(int i=0;i<Math.min(wordsInUse,set.wordsInUse);i++)if((words[i]&set.words[i])!=0)return true;
     return false;
 }

 public int length(){
     return wordsInUse==0?0:wordsInUse*64-Long.numberOfLeadingZeros(words[wordsInUse-1]);
 }
 public int size(){return words.length<<6;}
 public boolean isEmpty(){return wordsInUse==0;}
 public int cardinality(){
     int count=0;
     for(int i=0;i<wordsInUse;i++)count+=Long.bitCount(words[i]);
     return count;
 }
 public String toString(){
     StringBuilder out=new StringBuilder();
     out.append('{');
     boolean first=true;
     for(int i=nextSetBit(0);i>=0;){
         if(!first)out.append(", ");
         first=false;
         out.append(i);
         if(++i<0)break; // bit Integer.MAX_VALUE was the last possible one
         i=nextSetBit(i);
     }
     out.append('}');
     return out.toString();
 }

 public boolean equals(Object obj){
     if(!(obj instanceof BitSet))return false;
     if(obj==this)return true;
     BitSet other=(BitSet)obj;
     if(other.wordsInUse!=wordsInUse)return false;
     for(int i=0;i<wordsInUse;i++)if(other.words[i]!=words[i])return false;
     return true;
 }
 public int hashCode(){
     long h=1234;
     for(int i=wordsInUse;--i>=0;)h^=words[i]*(i+1);
     return (int)((h>>32)^h);
 }
 public Object clone(){
     if(!sizeIsSticky&&wordsInUse!=words.length)words=Arrays.copyOf(words,wordsInUse);
     try{
         BitSet copy=(BitSet)super.clone();
         copy.words=words.clone();
         return copy;
     }catch(CloneNotSupportedException e){throw new InternalError(e);}
 }

 public long[] toLongArray(){
     return Arrays.copyOf(words,wordsInUse);
 }
 public byte[] toByteArray(){
     byte[] bytes=new byte[(length()+7)/8];
     for(int k=0;k<bytes.length;k++)bytes[k]=(byte)(words[k>>3]>>>(8*(k&7)));
     return bytes;
 }
 public static BitSet valueOf(long[] longs){
     int n=longs.length;
     while(n>0&&longs[n-1]==0)n--;
     return new BitSet(Arrays.copyOf(longs,n));
 }
 public static BitSet valueOf(byte[] bytes){
     int n=bytes.length;
     while(n>0&&bytes[n-1]==0)n--;
     long[] words=new long[(n+7)/8];
     for(int k=0;k<n;k++)words[k>>3]|=(bytes[k]&0xffL)<<(8*(k&7));
     return new BitSet(words);
 }
}
