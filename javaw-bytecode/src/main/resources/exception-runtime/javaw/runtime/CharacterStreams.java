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
}
