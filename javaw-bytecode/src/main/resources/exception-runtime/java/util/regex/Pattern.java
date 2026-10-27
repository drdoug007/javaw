package java.util.regex;
import java.util.*;
/** Selected sequential regex engine; unsupported constructs are diagnosed explicitly. */
public final class Pattern {
    public static final int UNIX_LINES=1,CASE_INSENSITIVE=2,COMMENTS=4,MULTILINE=8,LITERAL=16,DOTALL=32,UNICODE_CASE=64,CANON_EQ=128,UNICODE_CHARACTER_CLASS=256;
    final String source;final int flags;final Node root;int groups;
    interface Next {boolean at(int position);}
    interface Node {boolean match(State state,int position,Next next);}
    static final class State {final CharSequence text;final int end,flags;final int[] starts,ends;
        State(CharSequence text,int flags,int count){this.text=text;this.end=text.length();this.flags=flags;starts=new int[count+1];ends=new int[count+1];Arrays.fill(starts,-1);Arrays.fill(ends,-1);}
    }
    private Pattern(String source,int flags){this.source=Objects.requireNonNull(source);if((flags&~511)!=0)throw new IllegalArgumentException("Unknown flag 0x"+Integer.toHexString(flags));if((flags&~LITERAL)!=0)throw unsupported("flags");this.flags=flags;root=parse();}
    public static Pattern compile(String regex){return compile(regex,0);}public static Pattern compile(String regex,int flags){return new Pattern(regex,flags);}
    public String pattern(){return source;}public String toString(){return source;}public int flags(){return flags;}
    public Matcher matcher(CharSequence text){return new Matcher(this,Objects.requireNonNull(text));}
    public static boolean matches(String regex,CharSequence text){return compile(regex).matcher(text).matches();}
    public static String quote(String text){Objects.requireNonNull(text);return "\\Q"+text.replace("\\E","\\E\\\\E\\Q")+"\\E";}
    static UnsupportedOperationException unsupported(String feature){return new UnsupportedOperationException("Unsupported bytecode regular expression feature: "+feature);}
    private Node parse(){if((flags&LITERAL)!=0)return literal(source);Parser parser=new Parser();Node result=parser.sequence();if(parser.position!=source.length())throw parser.error("Unmatched closing ')'",parser.position-1);return result;}
    static int codePoint(CharSequence text,int position,int end){char first=text.charAt(position);if(Character.isHighSurrogate(first) && position+1<end && Character.isLowSurrogate(text.charAt(position+1)))return Character.toCodePoint(first,text.charAt(position+1));return first;}
    static Node sequence(Node left,Node right){return (s,p,n)->left.match(s,p,q->right.match(s,q,n));}
    static Node atom(java.util.function.IntPredicate predicate){return (s,p,n)->p<s.end && predicate.test(codePoint(s.text,p,s.end)) && n.at(p+Character.charCount(codePoint(s.text,p,s.end)));}
    final class Parser {
        int position;boolean quoted;
        PatternSyntaxException error(String message,int index){return new PatternSyntaxException(message,source,index);}
        void quotes(){while(position+1<source.length() && source.charAt(position)=='\\' && ((!quoted && source.charAt(position+1)=='Q') || (quoted && source.charAt(position+1)=='E'))){quoted=!quoted;position+=2;}}
        Node sequence(){Node result=literal("");while(true){quotes();if(position==source.length())return result;char c=source.charAt(position);if(!quoted && (c==')'||c=='|'))return result;result=Pattern.sequence(result,term());}}
        Node term(){quotes();int offset=position;int c=codePoint(source,position,source.length());position+=Character.charCount(c);
            if(quoted)return literal(new String(Character.toChars(c)));
            if(c=='\\')return escaped();
            if(c=='.')return atom(v->v!='\n' && v!='\r' && v!=0x85 && v!=0x2028 && v!=0x2029);
            if(c=='[' || c=='(' || c=='|' || c=='^' || c=='$')throw unsupported("construct at index "+offset);
            if(c=='*'||c=='+'||c=='?')throw error("Dangling meta character '"+(char)c+"'",offset);
            if(c=='{')throw error("Illegal repetition",position);
            return literal(new String(Character.toChars(c)));
        }
        Node escaped(){if(position==source.length())throw error("Unexpected internal error",position);int offset=position;char c=source.charAt(position++);
            switch(c){case 't':return literal("\t");case 'n':return literal("\n");case 'r':return literal("\r");case 'f':return literal("\f");case 'a':return literal(String.valueOf((char)7));case 'e':return literal(String.valueOf((char)27));
                case 'u':return literal(String.valueOf((char)hex(4)));case 'x':return literal(String.valueOf((char)hex(2)));default:
                if(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9')throw unsupported("escape \\"+c+" at index "+offset);return literal(String.valueOf(c));}
        }
        int hex(int length){int value=0;for(int i=0;i<length;i++){if(position==source.length())throw error("Illegal hexadecimal escape sequence",position);int digit=Character.digit(source.charAt(position++),16);if(digit<0)throw error("Illegal hexadecimal escape sequence",position-1);value=(value<<4)|digit;}return value;}
    }
    static Node literal(final String value){return (s,p,n)->{if(value.length()>s.end-p)return false;for(int i=0;i<value.length();i++)if(s.text.charAt(p+i)!=value.charAt(i))return false;return n.at(p+value.length());};}
}
