package java.util.regex;
import java.util.*;
/** Selected sequential regex engine; unsupported constructs are diagnosed explicitly. */
public final class Pattern implements java.io.Serializable {
    public static final int UNIX_LINES=1,CASE_INSENSITIVE=2,COMMENTS=4,MULTILINE=8,LITERAL=16,DOTALL=32,UNICODE_CASE=64,CANON_EQ=128,UNICODE_CHARACTER_CLASS=256;
    final String source;final int flags;final Node root;int groups;
    interface Next {boolean at(int position);}
    static final class Node {
        final int kind;Node left,right;String text;java.util.function.IntPredicate predicate;int number,min,max;boolean greedy;List<Instruction> code;int loops;
        Node(int kind){this.kind=kind;}
        boolean match(State state,int position,Next next){if(code==null){code=new ArrayList<>();int[] count={0};compile(this,code,count);code.add(new Instruction(0));loops=count[0];}return execute(code,loops,state,position,next);}
    }
    static final class Instruction {final int kind;Node node;int x,y,id;Instruction(int kind){this.kind=kind;}}
    static final class Frame {final int pc,position;final int[] starts,ends,counts,positions;
        Frame(int pc,int position,State state,int[] counts,int[] positions){this.pc=pc;this.position=position;starts=state.starts.clone();ends=state.ends.clone();this.counts=counts.clone();this.positions=positions.clone();}
    }
    static final class State {final CharSequence text;final int end,flags;final int[] starts,ends;
        State(CharSequence text,int flags,int count){this.text=text;this.end=text.length();this.flags=flags;starts=new int[count+1];ends=new int[count+1];Arrays.fill(starts,-1);Arrays.fill(ends,-1);}
    }
    private Pattern(String source,int flags){this.source=Objects.requireNonNull(source);if((flags&~511)!=0)throw new IllegalArgumentException("Unknown flag 0x"+Integer.toHexString(flags));if((flags&~(LITERAL|CASE_INSENSITIVE|UNICODE_CASE|DOTALL|MULTILINE|UNIX_LINES))!=0)throw unsupported("flags");this.flags=flags;root=parse();}
    public static Pattern compile(String regex){return compile(regex,0);}public static Pattern compile(String regex,int flags){return new Pattern(regex,flags);}
    public String pattern(){return source;}public String toString(){return source;}public int flags(){return flags;}
    public Matcher matcher(CharSequence text){return new Matcher(this,Objects.requireNonNull(text));}
    public static boolean matches(String regex,CharSequence text){return compile(regex).matcher(text).matches();}
    public static String quote(String text){Objects.requireNonNull(text);return "\\Q"+text.replace("\\E","\\E\\\\E\\Q")+"\\E";}
    static UnsupportedOperationException unsupported(String feature){return new UnsupportedOperationException("Unsupported bytecode regular expression feature: "+feature);}
    private Node parse(){if((flags&LITERAL)!=0)return literal(source);Parser parser=new Parser();Node result=parser.expression();if(parser.position!=source.length())throw parser.error("Unmatched closing ')'",parser.position-1);return result;}
    static int codePoint(CharSequence text,int position,int end){char first=text.charAt(position);if(Character.isHighSurrogate(first) && position+1<end && Character.isLowSurrogate(text.charAt(position+1)))return Character.toCodePoint(first,text.charAt(position+1));return first;}
    static Node sequence(Node left,Node right){Node node=new Node(10);node.left=left;node.right=right;return node;}
    static Node atom(java.util.function.IntPredicate predicate){Node node=new Node(1);node.predicate=predicate;return node;}
    static Node alternative(Node left,Node right){Node node=new Node(3);node.left=left;node.right=right;return node;}
    final class Parser {
        int position;boolean quoted;
        PatternSyntaxException error(String message,int index){return new PatternSyntaxException(message,source,index);}
        void quotes(){while(position+1<source.length() && source.charAt(position)=='\\' && ((!quoted && source.charAt(position+1)=='Q') || (quoted && source.charAt(position+1)=='E'))){quoted=!quoted;position+=2;}}
        Node expression(){Node result=sequence();while(position<source.length()&&!quoted&&source.charAt(position)=='|'){position++;Node left=result,right=sequence();result=alternative(left,right);}return result;}
        Node sequence(){Node result=literal("");while(true){quotes();if(position==source.length())return result;char c=source.charAt(position);if(!quoted && (c==')'||c=='|'))return result;result=Pattern.sequence(result,term());}}
        Node term(){Node body=baseTerm();quotes();if(quoted||position==source.length())return body;char c=source.charAt(position);int min,max;
            if(c=='*'){min=0;max=Integer.MAX_VALUE;position++;}else if(c=='+'){min=1;max=Integer.MAX_VALUE;position++;}else if(c=='?'){min=0;max=1;position++;}
            else if(c=='{'){position++;min=number();max=min;if(position<source.length()&&source.charAt(position)==','){position++;max=position<source.length()&&source.charAt(position)=='}'?Integer.MAX_VALUE:number();}if(position==source.length()||source.charAt(position)!='}')throw error("Unclosed counted closure",position);position++;if(max<min)throw error("Illegal repetition range",position-1);}
            else return body;
            boolean greedy=true;if(position<source.length()&&source.charAt(position)=='?'){greedy=false;position++;}else if(position<source.length()&&source.charAt(position)=='+')throw unsupported("possessive quantifier at index "+position);
            return repeat(body,min,max,greedy);
        }
        int number(){int start=position;long value=0;while(position<source.length()&&source.charAt(position)>='0'&&source.charAt(position)<='9'){value=value*10+source.charAt(position++)-'0';if(value>Integer.MAX_VALUE)throw error("Illegal repetition range",position-1);}if(start==position)throw error("Illegal repetition",position);return (int)value;}
        Node baseTerm(){quotes();int offset=position;int c=codePoint(source,position,source.length());position+=Character.charCount(c);
            if(quoted)return literal(new String(Character.toChars(c)));
            if(c=='\\')return escaped();
            if(c=='.')return atom(v->(flags&DOTALL)!=0 || !line(v,flags));
            if(c=='[')return characterClass();
            if(c=='('){boolean capture=true;if(position<source.length()&&source.charAt(position)=='?'){position++;if(position<source.length()&&source.charAt(position)==':'){capture=false;position++;}else throw unsupported("special group at index "+offset);}int number=capture?++groups:0;Node child=expression();if(position==source.length()||source.charAt(position)!=')')throw error("Unclosed group",position);position++;return capture?group(child,number):child;}
            if(c=='^'||c=='$')return anchor(c);
            if(c=='|')throw unsupported("construct at index "+offset);
            if(c=='*'||c=='+'||c=='?')throw error("Dangling meta character '"+(char)c+"'",offset);
            if(c=='{')throw error("Illegal repetition",position);
            return literal(new String(Character.toChars(c)));
        }
        Node escaped(){if(position==source.length())throw error("Unexpected internal error",position);int offset=position;char c=source.charAt(position++);
            if(c=='A'||c=='z'||c=='Z')return anchor(c);
            java.util.function.IntPredicate predefined=predefined(c);if(predefined!=null)return atom(predefined);
            switch(c){case 't':return literal("\t");case 'n':return literal("\n");case 'r':return literal("\r");case 'f':return literal("\f");case 'a':return literal(String.valueOf((char)7));case 'e':return literal(String.valueOf((char)27));
                case 'u':return literal(new String(Character.toChars(unicode())));case 'x':return literal(new String(Character.toChars(hexPoint())));default:
                if(Character.isLetterOrDigit(c)&&"bBcpPkGNRXhHvV0123456789".indexOf(c)<0)throw error("Illegal/unsupported escape sequence",offset);
                if(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9')throw unsupported("escape \\"+c+" at index "+offset);return literal(String.valueOf(c));}
        }
        Node characterClass(){boolean negative=false;if(position<source.length() && source.charAt(position)=='^'){negative=true;position++;}
            List<java.util.function.IntPredicate> parts=new ArrayList<>();boolean first=true;
            while(position<source.length()){
                if(source.charAt(position)==']' && !first){position++;final boolean negate=negative;return atom(v->{boolean found=false;for(java.util.function.IntPredicate part:parts)if(classTest(part,v,flags)){found=true;break;}return found!=negate;});}
                first=false;ClassPart left=classPart();
                if(position<source.length()-1 && source.charAt(position)=='-' && source.charAt(position+1)!=']'){
                    position++;ClassPart right=classPart();if(left.value<0||right.value<0)throw error("Illegal character range",position-1);if(left.value>right.value)throw error("Illegal character range",position-1);
                    final int low=left.value,high=right.value;parts.add(v->v>=low&&v<=high);
                }else parts.add(left.predicate);
            }
            throw error("Unclosed character class",position-1);
        }
        ClassPart classPart(){int start=position;int c=codePoint(source,position,source.length());position+=Character.charCount(c);
            if(c=='[' || c=='&' && position<source.length() && source.charAt(position)=='&')throw unsupported("nested/intersected character class at index "+start);
            if(c=='\\'){
                if(position==source.length())throw error("Unclosed character class",position-1);char escaped=source.charAt(position++);java.util.function.IntPredicate pre=predefined(escaped);if(pre!=null)return new ClassPart(-1,pre);
                switch(escaped){case 't':c=9;break;case 'n':c=10;break;case 'r':c=13;break;case 'f':c=12;break;case 'a':c=7;break;case 'e':c=27;break;case 'u':c=unicode();break;case 'x':c=hexPoint();break;default:if(Character.isLetterOrDigit(escaped)){if("cpPNQhHvV0123456789".indexOf(escaped)<0)throw error("Illegal/unsupported escape sequence",position-1);throw unsupported("class escape "+escaped);}c=escaped;}
            }
            final int point=c;return new ClassPart(c,v->same(point,v,flags));
        }
        int unicode(){int first=hex(4);if(first>=0xd800&&first<=0xdbff&&position+6<=source.length()&&source.charAt(position)=='\\'&&source.charAt(position+1)=='u'){
            int old=position;position+=2;int second=hex(4);if(second>=0xdc00&&second<=0xdfff)return Character.toCodePoint((char)first,(char)second);position=old;
        }return first;}
        int hexPoint(){if(position==source.length()||source.charAt(position)!='{')return hex(2);position++;int start=position;long value=0;
            while(position<source.length()&&source.charAt(position)!='}'){int digit=hexDigit(source.charAt(position++));if(digit<0)throw error("Illegal hexadecimal escape sequence",position-1);value=value*16+digit;if(value>0x10ffff)throw error("Hexadecimal codepoint is too big",position-1);}
            if(position==source.length()||position==start)throw error("Illegal hexadecimal escape sequence",position);position++;if(value>=0xd800&&value<=0xdfff)throw unsupported("surrogate code-point hex escape");return (int)value;
        }
        int hexDigit(char value){return value<128?Character.digit(value,16):-1;}
        int hex(int length){int value=0;for(int i=0;i<length;i++){if(position==source.length())throw error("Illegal hexadecimal escape sequence",position);int digit=hexDigit(source.charAt(position++));if(digit<0)throw error("Illegal hexadecimal escape sequence",position-1);value=(value<<4)|digit;}return value;}
    }
    static Node group(Node body,int number){Node node=new Node(4);node.left=body;node.number=number;return node;}
    static Node repeat(Node body,int min,int max,boolean greedy){Node node=new Node(5);node.left=body;node.min=min;node.max=max;node.greedy=greedy;return node;}
    static void compile(Node node,List<Instruction> code,int[] loops){
        if(node.kind==10){compile(node.left,code,loops);compile(node.right,code,loops);return;}
        if(node.kind==3){Instruction branch=new Instruction(3);code.add(branch);branch.x=code.size();compile(node.left,code,loops);Instruction jump=new Instruction(6);code.add(jump);branch.y=code.size();compile(node.right,code,loops);jump.x=code.size();return;}
        if(node.kind==4){Instruction start=new Instruction(4);start.id=node.number;code.add(start);compile(node.left,code,loops);Instruction end=new Instruction(7);end.id=node.number;code.add(end);return;}
        if(node.kind==5){int id=loops[0]++;Instruction enter=new Instruction(8);enter.id=id;code.add(enter);Instruction choice=new Instruction(5);choice.id=id;choice.node=node;int target=code.size();code.add(choice);choice.x=code.size();compile(node.left,code,loops);Instruction end=new Instruction(9);end.id=id;end.x=target;code.add(end);choice.y=code.size();return;}
        Instruction leaf=new Instruction(node.kind);leaf.node=node;code.add(leaf);
    }
    static boolean execute(List<Instruction> code,int loops,State state,int position,Next next){
        List<Frame> stack=new ArrayList<>();int[] counts=new int[loops],positions=new int[loops];int pc=0,p=position;
        while(true){Instruction instruction=code.get(pc);boolean failed=false;
            switch(instruction.kind){
                case 0:if(next.at(p))return true;failed=true;break;
                case 1:if(p<state.end&&instruction.node.predicate.test(codePoint(state.text,p,state.end))){p+=Character.charCount(codePoint(state.text,p,state.end));pc++;}else failed=true;break;
                case 2:int end=literalEnd(instruction.node.text,state,p);if(end>=0){p=end;pc++;}else failed=true;break;
                case 3:stack.add(new Frame(instruction.y,p,state,counts,positions));pc=instruction.x;break;
                case 4:state.starts[instruction.id]=p;pc++;break;
                case 7:state.ends[instruction.id]=p;pc++;break;
                case 6:pc=instruction.x;break;
                case 8:counts[instruction.id]=0;positions[instruction.id]=-1;pc++;break;
                case 9:counts[instruction.id]++;pc=instruction.x;break;
                case 5:{Node node=instruction.node;int count=counts[instruction.id];boolean exit=count>=node.min,body=count<node.max&&!(count>0&&positions[instruction.id]==p&&exit);positions[instruction.id]=p;
                    if(body&&exit){stack.add(new Frame(node.greedy?instruction.y:instruction.x,p,state,counts,positions));pc=node.greedy?instruction.x:instruction.y;}
                    else if(body)pc=instruction.x;else if(exit)pc=instruction.y;else failed=true;break;}
                case 11:if(anchorAt(instruction.node.number,state,p))pc++;else failed=true;break;
                default:throw new IllegalStateException("Invalid regex instruction");
            }
            if(failed){if(stack.isEmpty())return false;Frame frame=stack.remove(stack.size()-1);pc=frame.pc;p=frame.position;System.arraycopy(frame.starts,0,state.starts,0,state.starts.length);System.arraycopy(frame.ends,0,state.ends,0,state.ends.length);counts=frame.counts;positions=frame.positions;}
        }
    }
    static int literalEnd(String value,State state,int p){int index=0,q=p;while(index<value.length()){if(q==state.end)return -1;int wanted=codePoint(value,index,value.length()),actual=codePoint(state.text,q,state.end);if(!same(wanted,actual,state.flags))return -1;index+=Character.charCount(wanted);q+=Character.charCount(actual);}return q;}
    static final class ClassPart {final int value;final java.util.function.IntPredicate predicate;ClassPart(int value,java.util.function.IntPredicate predicate){this.value=value;this.predicate=predicate;}}
    static java.util.function.IntPredicate predefined(char code){
        switch(code){case 'd':return v->v>='0'&&v<='9';case 'D':return v->!(v>='0'&&v<='9');case 'w':return v->v>='a'&&v<='z'||v>='A'&&v<='Z'||v>='0'&&v<='9'||v=='_';case 'W':return v->!(v>='a'&&v<='z'||v>='A'&&v<='Z'||v>='0'&&v<='9'||v=='_');case 's':return v->v==' '||v>=9&&v<=13;case 'S':return v->!(v==' '||v>=9&&v<=13);default:return null;}
    }
    static Node literal(String value){Node node=new Node(2);node.text=value;return node;}
    static boolean line(int code,int flags){return code==10 || (flags&UNIX_LINES)==0 && (code==13||code==0x85||code==0x2028||code==0x2029);}
    static boolean finalBreak(State s,int p){if(p==s.end)return true;if(p==s.end-1&&line(s.text.charAt(p),s.flags)){return s.text.charAt(p)!=10||p==0||s.text.charAt(p-1)!=13||(s.flags&UNIX_LINES)!=0;}return (s.flags&UNIX_LINES)==0&&p==s.end-2&&s.text.charAt(p)==13&&s.text.charAt(p+1)==10;}
    static Node anchor(int kind){Node node=new Node(11);node.number=kind;return node;}
    static boolean anchorAt(int kind,State s,int p){
        if(kind=='A')return p==0;if(kind=='z')return p==s.end;if(kind=='Z')return finalBreak(s,p);
        if(kind=='^')return p==0 || (s.flags&MULTILINE)!=0&&p<s.end&&line(s.text.charAt(p-1),s.flags)&&!((s.flags&UNIX_LINES)==0&&s.text.charAt(p-1)==13&&s.text.charAt(p)==10);
        return (s.flags&MULTILINE)==0?finalBreak(s,p):p==s.end||line(s.text.charAt(p),s.flags)&&!((s.flags&UNIX_LINES)==0&&s.text.charAt(p)==10&&p>0&&s.text.charAt(p-1)==13);
    }
    static int lower(int value,int flags){if((flags&UNICODE_CASE)!=0)return Character.toLowerCase(Character.toUpperCase(value));return value>='A'&&value<='Z'?value+32:value;}
    static boolean same(int left,int right,int flags){return left==right||(flags&CASE_INSENSITIVE)!=0&&lower(left,flags)==lower(right,flags);}
    static boolean classTest(java.util.function.IntPredicate predicate,int value,int flags){if(predicate.test(value))return true;if((flags&CASE_INSENSITIVE)==0)return false;
        if((flags&UNICODE_CASE)!=0){int upper=Character.toUpperCase(value);return predicate.test(upper)||predicate.test(Character.toLowerCase(upper))||predicate.test(Character.toLowerCase(value));}
        return value>='A'&&value<='Z'&&predicate.test(value+32)||value>='a'&&value<='z'&&predicate.test(value-32);
    }
    public String[] split(CharSequence input){return split(input,0);}
    public String[] split(CharSequence input,int limit){Objects.requireNonNull(input);Matcher matcher=matcher(input);List<String> parts=new ArrayList<>();int end=0;boolean found=false;
        while(matcher.find()){
            if(end==0&&matcher.start()==0&&matcher.end()==0)continue;
            if(limit>0&&parts.size()==limit-1)break;
            parts.add(input.subSequence(end,matcher.start()).toString());end=matcher.end();found=true;
        }
        if(!found)return new String[]{input.toString()};parts.add(input.subSequence(end,input.length()).toString());
        if(limit==0)while(!parts.isEmpty()&&parts.get(parts.size()-1).isEmpty())parts.remove(parts.size()-1);
        return parts.toArray(new String[parts.size()]);
    }
    public java.util.function.Predicate<String> asPredicate(){return value->matcher(value).find();}
    public java.util.function.Predicate<String> asMatchPredicate(){return value->matcher(value).matches();}
}
