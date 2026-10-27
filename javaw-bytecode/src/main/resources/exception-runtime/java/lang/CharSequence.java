package java.lang;
public interface CharSequence {
    int length();char charAt(int index);CharSequence subSequence(int start,int end);String toString();
    default boolean isEmpty(){return length()==0;}

    default java.util.stream.IntStream chars(){return javaw.runtime.CharacterStreams.chars(this);}
    default java.util.stream.IntStream codePoints(){return javaw.runtime.CharacterStreams.codePoints(this);}

}
