package java.lang;
public interface Appendable {
    Appendable append(CharSequence text) throws java.io.IOException;
    Appendable append(CharSequence text,int start,int end) throws java.io.IOException;
    Appendable append(char ch) throws java.io.IOException;
}
