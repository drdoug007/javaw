package java.util.regex;
import java.util.*;
public final class Matcher {
    final Pattern pattern;CharSequence text;Pattern.State state;int first=-1,last=-1;
    Matcher(Pattern pattern,CharSequence text){this.pattern=pattern;this.text=text;}
    public Pattern pattern(){return pattern;}
    public boolean matches(){state=new Pattern.State(text,pattern.flags,pattern.groups);first=last=-1;return pattern.root.match(state,0,p->{if(p!=state.end)return false;first=0;last=p;state.starts[0]=0;state.ends[0]=p;return true;});}
}
