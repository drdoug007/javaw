package javaw.runtime;
import java.util.*;
import java.math.*;
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
   s.conversion=format.charAt(at++);s.raw=specifier(s);
   if("sSbBhHcCdoxXeEfgGaAtT%n".indexOf(s.conversion)<0)throw new UnknownFormatConversionException(String.valueOf(s.conversion));
   validate(s);specs.add(s);literal=at;
  }
  if(literal<length){Spec s=new Spec();s.literal=format.substring(literal);specs.add(s);}return specs;
 }
 private static boolean flag(Spec s,char f){return s.flags.indexOf(f)>=0;}
 private static String specifier(Spec s){String text="%";for(int i=0;i<FLAGS.length();i++)if(flag(s,FLAGS.charAt(i)))text+=FLAGS.charAt(i);if(s.index>0 && !flag(s,'<'))text+=s.index+"$";if(s.width>=0)text+=s.width;if(s.precision>=0)text+="."+s.precision;return text+s.conversion;}
 private static char conversion(Spec s){char c=s.conversion;return c>='A' && c<='Z'?(char)(c+32):c;}
 private static String flagsText(Spec s){String text=s.conversion>='A' && s.conversion<='Z'?"^":"";for(int i=0;i<FLAGS.length();i++)if(flag(s,FLAGS.charAt(i)))text+=FLAGS.charAt(i);return text;}
 private static void allowed(Spec s,String allowed){String bad="";for(int i=0;i<FLAGS.length();i++){char c=FLAGS.charAt(i);if(flag(s,c) && allowed.indexOf(c)<0)bad+=c;}if(!bad.isEmpty())throw new FormatFlagsConversionMismatchException(bad,conversion(s));}
 private static void validate(Spec s){
  char c=s.conversion;
  if(c=='n'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);if(s.width>=0)throw new IllegalFormatWidthException(s.width);if(!s.flags.isEmpty())throw new IllegalFormatFlagsException(flagsText(s));return;}
  if(c=='%'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);if(!s.flags.isEmpty() && !s.flags.equals("-"))throw new IllegalFormatFlagsException(flagsText(s));if(flag(s,'-') && s.width<0)throw new MissingFormatWidthException(s.raw);return;}
  if(c=='c' || c=='C'){if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);allowed(s,"-<");if(flag(s,'-') && s.width<0)throw new MissingFormatWidthException(s.raw);return;}
  if(flag(s,'-') && s.width<0)throw new MissingFormatWidthException(s.raw);
  if(c=='s' || c=='S'){allowed(s,"-#<");return;}
  if(c=='b' || c=='B' || c=='h' || c=='H'){allowed(s,"-<");return;}
  if(c=='d' || c=='o' || c=='x' || c=='X'){
   if(s.precision>=0)throw new IllegalFormatPrecisionException(s.precision);
   if(flag(s,'0') && s.width<0)throw new MissingFormatWidthException(s.raw);
   if((flag(s,'+') && flag(s,' ')) || (flag(s,'-') && flag(s,'0')))throw new IllegalFormatFlagsException(flagsText(s));
   allowed(s,c=='d'?"-+ 0,(<":"-#+ 0(<");return;
  }
  if(c=='f' || c=='e' || c=='E' || c=='g' || c=='G'){
   if(flag(s,'0') && s.width<0)throw new MissingFormatWidthException(s.raw);
   if((flag(s,'+') && flag(s,' ')) || (flag(s,'-') && flag(s,'0')))throw new IllegalFormatFlagsException(flagsText(s));
   allowed(s,c=='f'?"-#+ 0,(<":(c=='g' || c=='G')?"-+ 0,(<":"-#+ 0(<");return;
  }
  if(c=='a' || c=='A'){
   if(flag(s,'0') && s.width<0)throw new MissingFormatWidthException(s.raw);
   if((flag(s,'+') && flag(s,' ')) || (flag(s,'-') && flag(s,'0')))throw new IllegalFormatFlagsException(flagsText(s));
   allowed(s,"-#+ 0<");return;
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
   if((c=='s' || c=='S') && flag(s,'#'))throw new FormatFlagsConversionMismatchException("#",conversion(s));
   if(c=='a' || c=='A')text=hexFloat(value,s);
   else if(c=='f' || c=='e' || c=='E' || c=='g' || c=='G')text=decimalFraction(value,s,locale);
   else if(c=='o' || c=='x' || c=='X')text=radix(value,s);
   else if(c=='d')text=decimal(value,s,locale);
   else if(c=='b' || c=='B')text=value==null?"false":value instanceof Boolean?value.toString():"true";
   else if(c=='h' || c=='H')text=value==null?"null":Integer.toHexString(value.hashCode());
   else if(c=='c' || c=='C') {
    if(value==null)text="null";
    else if(value instanceof Character)text=String.valueOf(((Character)value).charValue());
    else if(value instanceof Byte || value instanceof Short || value instanceof Integer){int cp=((Number)value).intValue();if(!Character.isValidCodePoint(cp))throw new IllegalFormatCodePointException(cp);text=new String(Character.toChars(cp));}
    else throw new IllegalFormatConversionException(conversion(s),value.getClass());
   }else text=String.valueOf(value);
   int textPrecision=s.precision;
   if(textPrecision>=0 && (value==null || "sSbBhH".indexOf(c)>=0) && text.length()>textPrecision)text=text.substring(0,textPrecision);
   if(c>='A' && c<='Z')text=text.toUpperCase(c=='A' || ((value instanceof Double || value instanceof Float) && !Double.isFinite(((Number)value).doubleValue()))?Locale.ROOT:locale);
   out.append(pad(text,s));
  }
  return out.toString();
 }
 private static String radix(Object value,Spec s){
  if(value==null)return "null";
  if(value instanceof java.math.BigInteger){
   java.math.BigInteger n=(java.math.BigInteger)value;
   return decorate(n.abs().toString(s.conversion=='o'?8:16),n.signum()<0,flag(s,'#')?(s.conversion=='o'?"0":"0x"):"",s);
  }
  if(!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long))throw new IllegalFormatConversionException(conversion(s),value.getClass());
  allowed(s,"-#0<");long n=((Number)value).longValue();
  if(value instanceof Byte)n&=255L;else if(value instanceof Short)n&=65535L;else if(value instanceof Integer)n&=4294967295L;
  String digits=Long.toUnsignedString(n,s.conversion=='o'?8:16);
  String prefix=flag(s,'#')?(s.conversion=='o'?"0":"0x"):"";
  int zeros=s.width-prefix.length()-digits.length();if(flag(s,'0') && zeros>0)digits="0".repeat(zeros)+digits;
  return prefix+digits;
 }
 private static String hexFloat(Object value,Spec s){
  if(value==null)return "null";
  if(!(value instanceof Double || value instanceof Float))throw new IllegalFormatConversionException(conversion(s),value.getClass());
  double n=((Number)value).doubleValue();long bits=Double.doubleToRawLongBits(n);boolean negative=bits<0;
  long magnitude=bits&Long.MAX_VALUE,fraction=magnitude&0xfffffffffffffL;int field=(int)(magnitude>>>52);
  if(field==2047){if(fraction!=0)return "NaN";return (negative?"-":flag(s,'+')?"+":flag(s,' ')?" ":"")+"Infinity";}
  int precision=s.precision==0?1:s.precision;int exponent=field==0?(magnitude==0?0:-1022):field-1023;
  String digits;
  if(precision>0 && precision<13 && magnitude!=0){
   if(field==0){long scaled=Double.doubleToRawLongBits(Math.scalb(Math.abs(n),54));fraction=scaled&0xfffffffffffffL;exponent=(int)(scaled>>>52)-1023-54;}
   long significand=fraction|0x10000000000000L;int drop=52-4*precision;
   long kept=significand>>>drop,remainder=significand&((1L<<drop)-1),half=1L<<(drop-1);
   if(remainder>half || (remainder==half && (kept&1)!=0))kept++;
   if(kept==(1L<<(4*precision+1))){kept>>>=1;exponent++;}
   String hex=Long.toHexString(kept);digits=hex.substring(0,1)+"."+hex.substring(1);
  }else {
   String hex=Long.toHexString(fraction);hex="0".repeat(13-hex.length())+hex;
   if(precision<0){int end=hex.length();while(end>1 && hex.charAt(end-1)=='0')end--;hex=hex.substring(0,end);}
   else if(precision<13)hex=hex.substring(0,precision);else if(precision>13)hex+="0".repeat(precision-13);
   digits=(field==0?"0":"1")+"."+hex;
  }
  int width=s.width;
  // Java 17 calculates hexadecimal zero padding before appending precision zeros.
  if(flag(s,'0') && precision>0){int end=digits.length(),dot=digits.indexOf('.');while(end>dot+2 && digits.charAt(end-1)=='0')end--;width+=digits.length()-end;}
  return decorate(digits+"p"+exponent,negative,"0x",s,width);
 }
 private static String decimal(Object value,Spec s,Locale locale){
  if(value==null)return "null";
  if(!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger))throw new IllegalFormatConversionException(conversion(s),value.getClass());
  numericLocale(locale);
  String digits=value.toString();boolean negative=digits.charAt(0)=='-';
  if(negative)digits=digits.substring(1);
  if(flag(s,',')){StringBuilder grouped=new StringBuilder();for(int i=0;i<digits.length();i++){if(i>0 && (digits.length()-i)%3==0)grouped.append(',');grouped.append(digits.charAt(i));}digits=grouped.toString();}
  return localize(decorate(digits,negative,"",s),locale);
 }
 private static int numericLocale(Locale locale){
  if(locale.equals(Locale.ROOT) || locale.equals(Locale.ENGLISH) || locale.equals(Locale.US) || locale.equals(Locale.UK))return 0;
  if(locale.equals(Locale.GERMAN) || locale.equals(Locale.GERMANY))return 1;
  if(locale.equals(Locale.FRENCH) || locale.equals(Locale.FRANCE))return 2;
  if(locale.getLanguage().equals("ar") && locale.getVariant().isEmpty() && (locale.getCountry().isEmpty() || locale.getCountry().equals("EG")))return 3;
  throw new UnsupportedOperationException("Numeric format locale");
 }
 private static String localize(String text,Locale locale){
  int style=numericLocale(locale);if(style==0)return text;
  char decimal=style==3?(char)0x066b:',',grouping=style==3?(char)0x066c:style==2?' ':'.';int zero=style==3?0x0660:'0';
  StringBuilder out=new StringBuilder();for(int i=0;i<text.length();i++){char c=text.charAt(i);
   out.append(c=='.'?decimal:c==','?grouping:c>='0' && c<='9'?(char)(zero+c-'0'):c);
  }return out.toString();
 }
 private static String grouped(String digits,Spec s){
  if(!flag(s,','))return digits;
  int end=digits.indexOf('.');if(end<0)end=digits.indexOf('e');if(end<0)end=digits.length();StringBuilder out=new StringBuilder();
  for(int i=0;i<end;i++){if(i>0 && (end-i)%3==0)out.append(',');out.append(digits.charAt(i));}out.append(digits.substring(end));return out.toString();
 }
 private static String decimalFraction(Object value,Spec s,Locale locale){
  if(value==null)return "null";
  if(value instanceof Double || value instanceof Float)return binaryFraction(((Number)value).doubleValue(),s,locale);
  if(!(value instanceof BigDecimal))throw new IllegalFormatConversionException(conversion(s),value.getClass());
  numericLocale(locale);BigDecimal n=(BigDecimal)value;int precision=s.precision<0?6:s.precision;
  String digits;
  if(conversion(s)=='g')digits=general(n.abs(),precision==0?1:precision);
  else if(conversion(s)=='e')digits=scientific(n.abs(),precision,flag(s,'#'));
  else {digits=n.abs().setScale(precision,RoundingMode.HALF_UP).toPlainString();if(precision==0 && flag(s,'#'))digits+=".";}
  return localize(decorate(grouped(digits,s),n.signum()<0,"",s),locale);
 }
 private static String binaryFraction(double value,Spec s,Locale locale){
  if(Double.isNaN(value))return "NaN";
  if(Double.isInfinite(value))return decorate("Infinity",value<0,"",s,-1);
  if(Double.doubleToRawLongBits(value)==Long.MIN_VALUE)throw new UnsupportedOperationException("Binary floating negative zero");
  numericLocale(locale);boolean negative=value<0;int precision=s.precision<0?6:s.precision;
  BigDecimal n=value==0?BigDecimal.ZERO:new BigDecimal(Decimal.formatText(Math.abs(value)));
  String digits=conversion(s)=='g'?general(n,precision==0?1:precision):conversion(s)=='e'?scientific(n,precision,flag(s,'#')):n.setScale(precision,RoundingMode.HALF_UP).toPlainString();
  if(conversion(s)=='f' && precision==0 && flag(s,'#'))digits+=".";
  return localize(decorate(grouped(digits,s),negative,"",s),locale);
 }
 private static String general(BigDecimal n,int precision){
  BigDecimal rounded=n.round(new MathContext(precision,RoundingMode.HALF_UP));
  long exponent=(long)rounded.precision()-1-rounded.scale();
  if((n.signum()==0 && n.scale()!=0) || exponent < -4 || exponent>=precision)return scientific(rounded,precision-1,false);
  return rounded.setScale((int)(precision-exponent-1),RoundingMode.UNNECESSARY).toPlainString();
 }
 private static String scientific(BigDecimal n,int precision,boolean alternate){
  BigDecimal rounded=n.round(new MathContext(Math.addExact(precision,1),RoundingMode.HALF_UP));
  long exponent=(long)rounded.precision()-1-rounded.scale();
  String coefficient=rounded.unscaledValue().toString();
  String fraction=coefficient.substring(1);if(fraction.length()<precision)fraction+="0".repeat(precision-fraction.length());
  String mantissa=coefficient.substring(0,1);if(precision>0 || alternate)mantissa+="."+fraction;
  String power=Long.toString(exponent<0?-exponent:exponent);if(power.length()<2)power="0"+power;
  return mantissa+"e"+(exponent<0?"-":"+")+power;
 }
 private static String decorate(String digits,boolean negative,String radixPrefix,Spec s){return decorate(digits,negative,radixPrefix,s,s.width);}
 private static String decorate(String digits,boolean negative,String radixPrefix,Spec s,int width){
  String prefix=negative?(flag(s,'(')?"(":"-"):flag(s,'+')?"+":flag(s,' ')?" ":"";
  prefix+=radixPrefix;
  String suffix=negative && flag(s,'(')?")":"";
  int zeros=width-prefix.length()-digits.length()-suffix.length();
  if(flag(s,'0') && zeros>0)digits="0".repeat(zeros)+digits;
  return prefix+digits+suffix;
 }
 private static String pad(String text,Spec s){int n=s.width-text.length();if(n<=0)return text;String spaces=" ".repeat(n);return flag(s,'-')?text+spaces:spaces+text;}
}
