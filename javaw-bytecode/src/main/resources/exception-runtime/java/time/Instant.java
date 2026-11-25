package java.time;

import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class Instant implements Comparable<Instant>,java.io.Serializable {
    private static final long MIN_SECOND=-31557014167219200L;
    private static final long MAX_SECOND=31556889864403199L;
    private static final long SECONDS_PER_10000_YEARS=146097L*25*86400;
    private static final long SECONDS_0000_TO_1970=((146097L*5)-(30L*365+7))*86400;

    public static final Instant EPOCH=new Instant(0,0);
    public static final Instant MIN=ofEpochSecond(MIN_SECOND,0);
    public static final Instant MAX=ofEpochSecond(MAX_SECOND,999_999_999);

    private final long seconds;
    private final int nanos;

    private Instant(long epochSecond,int nanos){this.seconds=epochSecond;this.nanos=nanos;}

    private static Instant create(long seconds,int nanoOfSecond){
        if((seconds|nanoOfSecond)==0)return EPOCH;
        if(seconds<MIN_SECOND || seconds>MAX_SECOND)throw new DateTimeException("Instant exceeds minimum or maximum instant");
        return new Instant(seconds,nanoOfSecond);
    }

    public static Instant ofEpochSecond(long epochSecond){return create(epochSecond,0);}

    public static Instant ofEpochSecond(long epochSecond,long nanoAdjustment){
        long secs=Math.addExact(epochSecond,Math.floorDiv(nanoAdjustment,1_000_000_000L));
        int nos=(int)Math.floorMod(nanoAdjustment,1_000_000_000L);
        return create(secs,nos);
    }

    public static Instant ofEpochMilli(long epochMilli){
        return create(Math.floorDiv(epochMilli,1000),(int)Math.floorMod(epochMilli,1000)*1_000_000);
    }

    public long getEpochSecond(){return seconds;}
    public int getNano(){return nanos;}

    public Instant plusSeconds(long secondsToAdd){return plus(secondsToAdd,0);}
    public Instant plusMillis(long millisToAdd){return plus(millisToAdd/1000,(millisToAdd%1000)*1_000_000);}
    public Instant plusNanos(long nanosToAdd){return plus(0,nanosToAdd);}

    private Instant plus(long secondsToAdd,long nanosToAdd){
        if((secondsToAdd|nanosToAdd)==0)return this;
        long epochSec=Math.addExact(this.seconds,secondsToAdd);
        epochSec=Math.addExact(epochSec,nanosToAdd/1_000_000_000L);
        nanosToAdd=nanosToAdd%1_000_000_000L;
        return ofEpochSecond(epochSec,nanos+(int)nanosToAdd);
    }

    public Instant minusSeconds(long secondsToSubtract){
        return secondsToSubtract==Long.MIN_VALUE?plusSeconds(Long.MAX_VALUE).plusSeconds(1):plusSeconds(-secondsToSubtract);
    }
    public Instant minusMillis(long millisToSubtract){
        return millisToSubtract==Long.MIN_VALUE?plusMillis(Long.MAX_VALUE).plusMillis(1):plusMillis(-millisToSubtract);
    }
    public Instant minusNanos(long nanosToSubtract){
        return nanosToSubtract==Long.MIN_VALUE?plusNanos(Long.MAX_VALUE).plusNanos(1):plusNanos(-nanosToSubtract);
    }

    public long toEpochMilli(){
        if(seconds<0 && nanos>0){
            long millis=Math.multiplyExact(seconds+1,1000);
            long adj=nanos/1_000_000-1000;
            return Math.addExact(millis,adj);
        }
        long millis=Math.multiplyExact(seconds,1000);
        return Math.addExact(millis,nanos/1_000_000);
    }

    public int compareTo(Instant other){
        int cmp=Long.compare(this.seconds,other.seconds);
        if(cmp==0)cmp=this.nanos-other.nanos;
        return cmp;
    }
    public boolean isAfter(Instant other){return compareTo(other)>0;}
    public boolean isBefore(Instant other){return compareTo(other)<0;}

    public boolean equals(Object other){
        if(other==this)return true;
        if(other instanceof Instant){
            Instant that=(Instant)other;
            return that.seconds==this.seconds && that.nanos==this.nanos;
        }
        return false;
    }
    public int hashCode(){return (int)(seconds^(seconds>>>32))+(51*nanos);}

    public String toString(){
        StringBuilder buf=new StringBuilder();
        if(seconds>=-SECONDS_0000_TO_1970){
            long zeroSecs=seconds-SECONDS_PER_10000_YEARS+SECONDS_0000_TO_1970;
            long hi=Math.floorDiv(zeroSecs,SECONDS_PER_10000_YEARS)+1;
            long lo=Math.floorMod(zeroSecs,SECONDS_PER_10000_YEARS);
            if(hi>0)buf.append('+').append(hi);
            appendDateTime(buf,lo-SECONDS_0000_TO_1970);
        }else{
            long zeroSecs=seconds+SECONDS_0000_TO_1970;
            long hi=zeroSecs/SECONDS_PER_10000_YEARS;
            long lo=zeroSecs%SECONDS_PER_10000_YEARS;
            int pos=buf.length();
            int year=appendDateTime(buf,lo-SECONDS_0000_TO_1970);
            if(hi<0){
                if(year==-10_000)buf.replace(pos,pos+2,Long.toString(hi-1));
                else if(lo==0)buf.insert(pos,Long.toString(hi));
                else buf.insert(pos+1,Long.toString(Math.abs(hi)));
            }
        }
        if(nanos>0){
            buf.append('.');
            int n=nanos,div=100_000_000;
            for(int i=0;n>0 || i%3!=0;i++){
                int digit=n/div;
                buf.append((char)('0'+digit));
                n-=digit*div;
                div/=10;
            }
        }
        buf.append('Z');
        return buf.toString();
    }

    /** Appends yyyy-MM-ddTHH:mm:ss to buf (LocalDate/LocalTime toString style, seconds always) and returns the year. */
    private static int appendDateTime(StringBuilder buf,long epochSecond){
        long epochDay=Math.floorDiv(epochSecond,86400);
        int secsOfDay=(int)Math.floorMod(epochSecond,86400);
        long zeroDay=epochDay+719528-60;
        long adjust=0;
        if(zeroDay<0){
            long cycles=(zeroDay+1)/146097-1;
            adjust=cycles*400;
            zeroDay+=-cycles*146097;
        }
        long yearEst=(400*zeroDay+591)/146097;
        long doyEst=zeroDay-(365*yearEst+yearEst/4-yearEst/100+yearEst/400);
        if(doyEst<0){
            yearEst--;
            doyEst=zeroDay-(365*yearEst+yearEst/4-yearEst/100+yearEst/400);
        }
        yearEst+=adjust;
        int marchDoy0=(int)doyEst;
        int marchMonth0=(marchDoy0*5+2)/153;
        int month=(marchMonth0+2)%12+1;
        int dom=marchDoy0-(marchMonth0*306+5)/10+1;
        int year=(int)(yearEst+marchMonth0/10);
        // Like LocalDate.toString: four digits for |year|<1000, '+' for year>9999.
        // deleteCharAt is offset by the era prefix already in buf.
        int start=buf.length();
        int absYear=year<0?-year:year;
        if(absYear<1000){
            if(year<0)buf.append(year-10000).deleteCharAt(start+1);
            else buf.append(year+10000).deleteCharAt(start);
        }else{
            if(year>9999)buf.append('+');
            buf.append(year);
        }
        buf.append(month<10?"-0":"-").append(month).append(dom<10?"-0":"-").append(dom).append('T');
        int hour=secsOfDay/3600;
        int minute=(secsOfDay/60)%60;
        int second=secsOfDay%60;
        buf.append(hour<10?"0":"").append(hour).append(minute<10?":0":":").append(minute).append(second<10?":0":":").append(second);
        return year;
    }

    /** Parses ISO_INSTANT text (yyyy-MM-ddTHH:mm:ss.SSSSSSSSS, optional offset Z or +-HH:MM[:ss]). */
    public static Instant parse(CharSequence text){
        Objects.requireNonNull(text,"text");
        int len=text.length(),pos=0,start=0,sign=0;
        if(pos<len){
            char c=text.charAt(pos);
            if(c=='+'){sign=1;pos++;}
            else if(c=='-'){sign=-1;pos++;}
        }
        int digitsStart=pos;
        long year=0;int count=0;
        while(pos<len && count<10 && isDigit(text.charAt(pos))){year=year*10+text.charAt(pos)-'0';pos++;count++;}
        if(count<4)throw parseError(text,digitsStart);
        if(sign==-1){
            if(year==0)throw parseError(text,start);
            year=-year;
        }else if(sign==1){
            if(count<=4)throw parseError(text,start);
        }else if(count>4){
            throw parseError(text,start);
        }
        if(pos>=len || text.charAt(pos)!='-')throw parseError(text,pos);
        pos++;
        int month=twoDigits(text,pos);
        if(month<0)throw parseError(text,pos);
        pos+=2;
        if(pos>=len || text.charAt(pos)!='-')throw parseError(text,pos);
        pos++;
        int day=twoDigits(text,pos);
        if(day<0)throw parseError(text,pos);
        pos+=2;
        if(pos>=len || text.charAt(pos)!='T' && text.charAt(pos)!='t')throw parseError(text,pos);
        pos++;
        int hour=twoDigits(text,pos);
        if(hour<0)throw parseError(text,pos);
        pos+=2;
        if(pos>=len || text.charAt(pos)!=':')throw parseError(text,pos);
        pos++;
        int minute=twoDigits(text,pos);
        if(minute<0)throw parseError(text,pos);
        pos+=2;
        if(pos>=len || text.charAt(pos)!=':')throw parseError(text,pos);
        pos++;
        int second=twoDigits(text,pos);
        if(second<0)throw parseError(text,pos);
        pos+=2;
        int nano=0;
        if(pos<len && text.charAt(pos)=='.'){
            pos++;
            long n=0;int digits=0;
            while(pos<len && digits<9 && isDigit(text.charAt(pos))){n=n*10+text.charAt(pos)-'0';pos++;digits++;}
            for(;digits<9;digits++)n*=10;
            nano=(int)n;
        }
        int offStart=pos,offsetSecs=0;
        if(pos>=len)throw parseError(text,pos);
        char oc=text.charAt(pos);
        if(oc=='Z'||oc=='z'){
            pos++;
        }else if(oc=='+'||oc=='-'){
            int oSign=oc=='+'?1:-1;
            pos++;
            int oh=offsetGroup(text,pos);
            if(oh<0)throw parseError(text,offStart);
            pos+=2;
            if(pos>=len || text.charAt(pos)!=':')throw parseError(text,offStart);
            pos++;
            int om=offsetGroup(text,pos);
            if(om<0)throw parseError(text,offStart);
            pos+=2;
            int oss=0;
            if(pos<len && text.charAt(pos)==':'){
                int s=offsetGroup(text,pos+1);
                if(s>=0){pos+=3;oss=s;}
            }
            if(oh>23){
                String message="Value out of range: Hour[0-23], Minute[0-59], Second[0-59]";
                throw resolveError(text,message,new DateTimeException(message));
            }
            offsetSecs=oSign*(oh*3600+om*60+oss);
        }else{
            throw parseError(text,offStart);
        }
        int days=0;
        if(hour==24 && minute==0 && second==0 && nano==0){hour=0;days=1;}
        else if(hour==23 && minute==59 && second==60){second=59;}
        long y=year%10_000;
        if(month<1 || month>12 || day<1 || day>lengthOfMonth(y,month) || hour>23 || minute>59 || second>59
                || offsetSecs<-64800 || offsetSecs>64800)throw parseError(text,0);
        long epochSecond=(epochDay(year,month,day)+days)*86400L+hour*3600+minute*60+second-offsetSecs;
        if(pos<len)throw unparsedError(text,pos);
        if(epochSecond<MIN_SECOND || epochSecond>MAX_SECOND){
            String message="Unable to obtain Instant from TemporalAccessor: {InstantSeconds="+epochSecond+"}";
            throw resolveError(text,message,new DateTimeException(message));
        }
        return create(epochSecond,nano);
    }

    private static boolean isDigit(char c){return c>='0' && c<='9';}

    /** Two ASCII digits at pos, or -1 when either char is missing or not '0'..'9'. No range check. */
    private static int twoDigits(CharSequence text,int pos){
        int len=text.length();
        if(pos>=len || pos+1>=len)return -1;
        char a=text.charAt(pos),b=text.charAt(pos+1);
        if(!isDigit(a) || !isDigit(b))return -1;
        return (a-'0')*10+(b-'0');
    }

    /** Two ASCII digits at pos with value <= 59 (one offset group), or -1. */
    private static int offsetGroup(CharSequence text,int pos){
        int value=twoDigits(text,pos);
        return value<=59?value:-1;
    }

    private static DateTimeParseException parseError(CharSequence text,int index){
        return new DateTimeParseException("Text '"+abbreviate(text)+"' could not be parsed at index "+index,text,index);
    }

    private static DateTimeParseException unparsedError(CharSequence text,int index){
        return new DateTimeParseException("Text '"+abbreviate(text)+"' could not be parsed, unparsed text found at index "+index,text,index);
    }

    private static DateTimeParseException resolveError(CharSequence text,String message,RuntimeException cause){
        return new DateTimeParseException("Text '"+abbreviate(text)+"' could not be parsed: "+message,text,0,cause);
    }

    private static String abbreviate(CharSequence text){
        return text.length()>64?text.subSequence(0,64).toString()+"...":text.toString();
    }

    private static boolean isLeap(long year){return (year&3)==0 && (year%100!=0 || year%400==0);}

    private static int lengthOfMonth(long year,int month){
        if(month==2)return isLeap(year)?29:28;
        return month==4 || month==6 || month==9 || month==11?30:31;
    }

    /** Proleptic Gregorian day count from 1970-01-01 for the full (signed) year, as LocalDate.toEpochDay. */
    private static long epochDay(long year,int month,int day){
        long y=year,total=365*y;
        if(y>=0)total+=(y+3)/4-(y+99)/100+(y+399)/400;
        else total-=y/-4-y/-100+y/-400;
        total+=(367L*month-362)/12;
        total+=day-1;
        if(month>2){total--;if(!isLeap(year))total--;}
        return total-719528;
    }
}
