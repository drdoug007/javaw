package javaw.runtime;
import java.util.*;
import java.util.stream.*;
/** Terminal-time binding for non-interfering UTF-16 sequences. */
public final class CharacterStreams {
    private CharacterStreams(){}
    public static IntStream chars(final CharSequence text){
        Objects.requireNonNull(text);return IntPipeline.source(()->new Iterator<Integer>(){
            final int end=text.length();int index;
            public boolean hasNext(){return index<end;}
            public Integer next(){if(!hasNext())throw new NoSuchElementException();return Integer.valueOf(text.charAt(index++));}
        });
    }

    public static IntStream codePoints(final CharSequence text){
        Objects.requireNonNull(text);return IntPipeline.source(()->new Iterator<Integer>(){
            final int end=text.length();int index;
            public boolean hasNext(){return index<end;}
            public Integer next(){
                if(!hasNext())throw new NoSuchElementException();int first=text.charAt(index++);
                if(first>=0xd800 && first<=0xdbff && index<end){int second=text.charAt(index);if(second>=0xdc00 && second<=0xdfff){index++;return Integer.valueOf(0x10000+((first-0xd800)<<10)+(second-0xdc00));}}
                return Integer.valueOf(first);
            }
        });
    }

}
