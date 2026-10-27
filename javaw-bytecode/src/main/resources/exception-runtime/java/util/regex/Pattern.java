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
    private Node parse(){
        StringBuilder literal=new StringBuilder();boolean quoted=(flags&LITERAL)!=0;
        for(int i=0;i<source.length();i++){char c=source.charAt(i);
            if((flags&LITERAL)==0 && c=='\\' && i+1<source.length()){
                char following=source.charAt(i+1);if(!quoted && following=='Q'){quoted=true;i++;continue;}if(quoted && following=='E'){quoted=false;i++;continue;}
                if(!quoted && following=='\\'){literal.append('\\');i++;continue;}
            }
            if(!quoted && ".[](){}*+?|^$\\".indexOf(c)>=0)throw unsupported("pattern syntax");literal.append(c);
        }
        return literal(literal.toString());
    }
    static Node literal(final String value){return (s,p,n)->{if(value.length()>s.end-p)return false;for(int i=0;i<value.length();i++)if(s.text.charAt(p+i)!=value.charAt(i))return false;return n.at(p+value.length());};}
}
