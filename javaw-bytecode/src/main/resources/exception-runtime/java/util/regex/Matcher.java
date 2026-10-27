package java.util.regex;
import java.util.*;
public final class Matcher implements MatchResult {
    final Pattern pattern;CharSequence text;Pattern.State state;int first=-1,last=0,appendPosition;boolean anchoredFailure;
    Matcher(Pattern pattern,CharSequence text){this.pattern=pattern;this.text=text;}
    public Pattern pattern(){return pattern;}
    private boolean at(int start,boolean entire){state=new Pattern.State(text,pattern.flags,pattern.groups);first=-1;
        boolean found=pattern.root.match(state,start,p->{if(entire&&p!=state.end)return false;first=start;last=p;state.starts[0]=start;state.ends[0]=p;return true;});
        return found;
    }
    public boolean matches(){boolean found=at(0,true);anchoredFailure=!found;return found;}
    public boolean lookingAt(){boolean found=at(0,false);anchoredFailure=!found;return found;}
    private boolean search(int from){for(int p=from;p<=text.length();p++)if(at(p,false))return true;first=-1;return false;}
    public boolean find(){if(anchoredFailure)throw Pattern.unsupported("find after failed anchored match; call reset()");int from=last+(last==first?1:0);if(from>text.length()){Arrays.fill(state.starts,-1);Arrays.fill(state.ends,-1);return false;}return search(from);}
    public boolean find(int start){if(start<0||start>text.length())throw new IndexOutOfBoundsException("Illegal start index");reset();return search(start);}
    public Matcher reset(){first=-1;last=appendPosition=0;state=null;anchoredFailure=false;return this;}
    public Matcher reset(CharSequence text){reset();this.text=Objects.requireNonNull(text);return this;}
    private void check(){if(first<0)throw new IllegalStateException("No match found");}
    private void groupIndex(int group){if(group<0||group>pattern.groups)throw new IndexOutOfBoundsException("No group "+group);}
    public int start(){check();return first;}public int end(){check();return last;}public String group(){return group(0);}public int groupCount(){return pattern.groups;}
    public int start(int group){check();groupIndex(group);return state.starts[group];}public int end(int group){check();groupIndex(group);return state.ends[group];}
    public String group(int group){int start=start(group);return start<0?null:text.subSequence(start,end(group)).toString();}
}
