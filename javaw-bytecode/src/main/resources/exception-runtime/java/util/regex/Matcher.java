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

    public static String quoteReplacement(String text){Objects.requireNonNull(text);if(text.indexOf('\\')<0&&text.indexOf('$')<0)return text;StringBuilder out=new StringBuilder();for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='\\'||c=='$')out.append('\\');out.append(c);}return out.toString();}
    private String replacement(String value){Objects.requireNonNull(value);StringBuilder out=new StringBuilder();for(int i=0;i<value.length();i++){char c=value.charAt(i);
        if(c=='\\'){if(++i==value.length())throw new IllegalArgumentException("character to be escaped is missing");out.append(value.charAt(i));}
        else if(c=='$'){if(++i==value.length())throw new IllegalArgumentException("Illegal group reference: group index is missing");c=value.charAt(i);if(c=='{')throw Pattern.unsupported("named replacement group");if(c<'0'||c>'9')throw new IllegalArgumentException("Illegal group reference");int number=c-'0';groupIndex(number);
            while(i+1<value.length()&&value.charAt(i+1)>='0'&&value.charAt(i+1)<='9'){int candidate=number*10+value.charAt(i+1)-'0';if(candidate>pattern.groups)break;number=candidate;i++;}String capture=group(number);if(capture!=null)out.append(capture);
        }else out.append(c);
    }return out.toString();}
    public Matcher appendReplacement(StringBuilder destination,String replacement){check();String value=replacement(replacement);destination.append(text,appendPosition,first).append(value);appendPosition=last;return this;}
    public Matcher appendReplacement(StringBuffer destination,String replacement){check();String value=replacement(replacement);destination.append(text,appendPosition,first).append(value);appendPosition=last;return this;}
    public StringBuilder appendTail(StringBuilder destination){return destination.append(text,appendPosition,text.length());}
    public StringBuffer appendTail(StringBuffer destination){return destination.append(text,appendPosition,text.length());}
    public String replaceAll(String replacement){reset();if(!find())return text.toString();StringBuilder result=new StringBuilder();do{appendReplacement(result,replacement);}while(find());return appendTail(result).toString();}
    public String replaceFirst(String replacement){Objects.requireNonNull(replacement);reset();if(!find())return text.toString();StringBuilder result=new StringBuilder();appendReplacement(result,replacement);return appendTail(result).toString();}
}
