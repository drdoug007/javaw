package java.util.regex;
import java.util.*;
public final class Matcher implements MatchResult {
    final Pattern pattern;CharSequence text;Pattern.State state;int first=-1,last=-1;
    Matcher(Pattern pattern,CharSequence text){this.pattern=pattern;this.text=text;}
    public Pattern pattern(){return pattern;}
    public boolean matches(){state=new Pattern.State(text,pattern.flags,pattern.groups);first=last=-1;return pattern.root.match(state,0,p->{if(p!=state.end)return false;first=0;last=p;state.starts[0]=0;state.ends[0]=p;return true;});}

    private void check(){if(first<0)throw new IllegalStateException("No match found");}
    private void groupIndex(int group){if(group<0||group>pattern.groups)throw new IndexOutOfBoundsException("No group "+group);}
    public int start(){return start(0);}public int end(){return end(0);}public String group(){return group(0);}public int groupCount(){return pattern.groups;}
    public int start(int group){check();groupIndex(group);return state.starts[group];}public int end(int group){check();groupIndex(group);return state.ends[group];}
    public String group(int group){int start=start(group);return start<0?null:text.subSequence(start,end(group)).toString();}
}
