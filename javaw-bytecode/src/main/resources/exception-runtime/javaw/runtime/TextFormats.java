package javaw.runtime;
import java.util.*;
/** Selected String formatting, without a public Formatter/provider implementation. */
public final class TextFormats {
 private TextFormats(){}
 private static final String FLAGS="-#+ 0,(<";
 private static final class Spec {
  String literal,raw,flags="";int index=-1,width=-1,precision=-1;char conversion;
 }
 private static boolean digit(char c){return c>='0' && c<='9';}
 private static int number(String format,int start,int end,int kind){
  int value=0;for(int i=start;i<end;i++){int d=format.charAt(i)-'0';if(value>(Integer.MAX_VALUE-d)/10){if(kind==0)throw new IllegalFormatArgumentIndexException(Integer.MIN_VALUE);if(kind==1)throw new IllegalFormatWidthException(Integer.MIN_VALUE);throw new IllegalFormatPrecisionException(Integer.MIN_VALUE);}value=value*10+d;}return value;
 }
 private static ArrayList<Spec> parse(String format){
  ArrayList<Spec> specs=new ArrayList<>();int length=format.length(),literal=0;
  for(int at=0;at<length;){
   if(format.charAt(at)!='%'){at++;continue;}
   if(literal<at){Spec s=new Spec();s.literal=format.substring(literal,at);specs.add(s);}
   int start=at++;Spec s=new Spec();int n=at;while(at<length && digit(format.charAt(at)))at++;
   if(at>n && at<length && format.charAt(at)=='$'){s.index=number(format,n,at,0);if(s.index==0)throw new IllegalFormatArgumentIndexException(0);at++;}else at=n;
   while(at<length && FLAGS.indexOf(format.charAt(at))>=0){char flag=format.charAt(at++);if(s.flags.indexOf(flag)>=0)throw new DuplicateFormatFlagsException(String.valueOf(flag));s.flags+=flag;}
   n=at;while(at<length && digit(format.charAt(at)))at++;if(at>n)s.width=number(format,n,at,1);
   if(at<length && format.charAt(at)=='.'){n=++at;while(at<length && digit(format.charAt(at)))at++;if(n==at)throw new UnknownFormatConversionException(".");s.precision=number(format,n,at,2);}
   if(at==length)throw new UnknownFormatConversionException(String.valueOf(format.charAt(start+1<length?start+1:start)));
   s.conversion=format.charAt(at++);s.raw=format.substring(start,at);
   if("sSbBhHcCdoxXeEfgGaAtT%n".indexOf(s.conversion)<0)throw new UnknownFormatConversionException(String.valueOf(s.conversion));
   validate(s);specs.add(s);literal=at;
  }
  if(literal<length){Spec s=new Spec();s.literal=format.substring(literal);specs.add(s);}return specs;
 }
 private static boolean flag(Spec s,char f){return s.flags.indexOf(f)>=0;}
 private static void allowed(Spec s,String allowed){String bad="";for(int i=0;i<s.flags.length();i++){char c=s.flags.charAt(i);if(allowed.indexOf(c)<0)bad+=c;}if(!bad.isEmpty())throw new FormatFlagsConversionMismatchException(bad,s.conversion);}
 private static void validate(Spec s){
  char c=s.conversion;
  if(c=='n'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);if(s.width>=0)throw new IllegalFormatWidthException(s.width);if(!s.flags.isEmpty())throw new IllegalFormatFlagsException(s.flags);return;}
  if(c=='%'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);if(!s.flags.isEmpty() && !s.flags.equals("-"))throw new IllegalFormatFlagsException(s.flags);if(flag(s,'-') && s.width<0)throw new MissingFormatWidthException(s.raw);return;}
  if(flag(s,'-') && s.width<0)throw new MissingFormatWidthException(s.raw);
  if(c=='s' || c=='S'){allowed(s,"-#<");return;}
  if(c=='b' || c=='B' || c=='h' || c=='H'){allowed(s,"-<");return;}
  if(c=='c' || c=='C'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);allowed(s,"-<");return;}
  if(c=='d'){
   if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);
   if(flag(s,'0') && s.width<0)throw new MissingFormatWidthException(s.raw);
   if((flag(s,'+') && flag(s,' ')) || (flag(s,'-') && flag(s,'0')))throw new IllegalFormatFlagsException(s.flags);
   allowed(s,"-+ 0,(<");return;
  }
  throw new UnsupportedOperationException("Formatting conversion: "+c);
 }
 public static String format(Locale locale,String format,Object[] args){
  if(locale==null)locale=Locale.ROOT;
  ArrayList<Spec> specs=parse(format);StringBuilder out=new StringBuilder();int ordinary=0,last=-1;
  for(Spec s:specs){
   if(s.literal!=null){out.append(s.literal);continue;}
   char c=s.conversion;if(c=='n'){out.append('\n');continue;}if(c=='%'){out.append(pad("%",s));continue;}
   int index;if(flag(s,'<')){index=last;}else if(s.index>0)index=s.index-1;else index=ordinary++;
   if(index<0 || (args!=null && index>=args.length))throw new MissingFormatArgumentException(s.raw);last=index;
   Object value=args==null?null:args[index];String text;
   if(flag(s,'#'))throw new FormatFlagsConversionMismatchException("#",c);
   if(c=='d')text=decimal(value,s,locale);
   else if(c=='b' || c=='B')text=value==null?"false":value instanceof Boolean?value.toString():"true";
   else if(c=='h' || c=='H')text=value==null?"null":Integer.toHexString(value.hashCode());
   else if(c=='c' || c=='C') {
    if(value==null)text="null";
    else if(value instanceof Character)text=String.valueOf(((Character)value).charValue());
    else if(value instanceof Byte || value instanceof Short || value instanceof Integer){int cp=((Number)value).intValue();if(!Character.isValidCodePoint(cp))throw new IllegalFormatCodePointException(cp);text=new String(Character.toChars(cp));}
    else throw new IllegalFormatConversionException(c,value.getClass());
   }else text=String.valueOf(value);
   if(s.precision>=0 && text.length()>s.precision)text=text.substring(0,s.precision);
   if(c>='A' && c<='Z')text=text.toUpperCase(locale);
   out.append(pad(text,s));
  }
  return out.toString();
 }
 private static String decimal(Object value,Spec s,Locale locale){
  if(value==null)return "null";
  if(value instanceof java.math.BigInteger)throw new UnsupportedOperationException("BigInteger formatting");
  if(!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long))throw new IllegalFormatConversionException(s.conversion,value.getClass());
  String language=locale.getLanguage(),country=locale.getCountry();
  if(!language.isEmpty() && !(language.equals("en") && (country.isEmpty() || country.equals("US") || country.equals("GB"))))throw new UnsupportedOperationException("Numeric format locale");
  long n=((Number)value).longValue();String digits=Long.toString(n);boolean negative=n<0;
  if(negative)digits=digits.substring(1);
  if(flag(s,',')){StringBuilder grouped=new StringBuilder();for(int i=0;i<digits.length();i++){if(i>0 && (digits.length()-i)%3==0)grouped.append(',');grouped.append(digits.charAt(i));}digits=grouped.toString();}
  String prefix=negative?(flag(s,'(')?"(":"-"):flag(s,'+')?"+":flag(s,' ')?" ":"";
  String suffix=negative && flag(s,'(')?")":"";
  int zeros=s.width-prefix.length()-digits.length()-suffix.length();
  if(flag(s,'0') && zeros>0)digits="0".repeat(zeros)+digits;
  return prefix+digits+suffix;
 }
 private static String pad(String text,Spec s){int n=s.width-text.length();if(n<=0)return text;String spaces=" ".repeat(n);return flag(s,'-')?text+spaces:spaces+text;}
}
