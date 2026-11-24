package java.time;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class Duration implements Comparable<Duration>,java.io.Serializable {
    private static final long NANOS_PER_SECOND=1_000_000_000L;
    private static final long NANOS_PER_MILLI=1_000_000;
    private static final long SECONDS_PER_MINUTE=60;
    private static final long SECONDS_PER_HOUR=3600;
    private static final long SECONDS_PER_DAY=86400;

    public static final Duration ZERO=new Duration(0,0);

    private final long seconds;
    private final int nanos;

    private Duration(long seconds,int nanos){this.seconds=seconds;this.nanos=nanos;}

    private static Duration create(long seconds,int nanoAdjustment){
        if((seconds|nanoAdjustment)==0)return ZERO;
        long totalNanos=nanoAdjustment;
        long totalSecs=Math.addExact(seconds,Math.floorDiv(totalNanos,NANOS_PER_SECOND));
        int adjustedNanos=(int)Math.floorMod(totalNanos,NANOS_PER_SECOND);
        return new Duration(totalSecs,adjustedNanos);
    }

    private static Duration create(BigDecimal secs){
        BigInteger n=secs.movePointRight(9).toBigIntegerExact();
        BigInteger[] qr=n.divideAndRemainder(BigInteger.valueOf(1_000_000_000L));
        if(qr[0].bitLength()>63)throw new ArithmeticException("Exceeds capacity of Duration: "+n);
        return ofSeconds(qr[0].longValue(),qr[1].intValue());
    }

    public static Duration ofDays(long days){return create(Math.multiplyExact(days,SECONDS_PER_DAY),0);}
    public static Duration ofHours(long hours){return create(Math.multiplyExact(hours,SECONDS_PER_HOUR),0);}
    public static Duration ofMinutes(long minutes){return create(Math.multiplyExact(minutes,SECONDS_PER_MINUTE),0);}
    public static Duration ofSeconds(long seconds){return create(seconds,0);}

    public static Duration ofSeconds(long seconds,long nanoAdjustment){
        long secs=Math.addExact(seconds,Math.floorDiv(nanoAdjustment,NANOS_PER_SECOND));
        int nos=(int)Math.floorMod(nanoAdjustment,NANOS_PER_SECOND);
        return create(secs,nos);
    }

    public static Duration ofMillis(long millis){
        long secs=millis/1000;
        long mos=millis%1000;
        if(mos<0){mos+=1000;secs--;}
        return create(secs,(int)(mos*NANOS_PER_MILLI));
    }

    public static Duration ofNanos(long nanos){
        long secs=nanos/NANOS_PER_SECOND;
        long n=nanos%NANOS_PER_SECOND;
        if(n<0){n+=NANOS_PER_SECOND;secs--;}
        return create(secs,(int)n);
    }

    public static Duration parse(CharSequence text){
        Objects.requireNonNull(text,"text");
        int length=text.length();
        int i=0;
        boolean negate=false;
        if(i<length && (text.charAt(i)=='+' || text.charAt(i)=='-')){negate=text.charAt(i)=='-';i++;}
        if(i>=length || (text.charAt(i)!='P' && text.charAt(i)!='p'))return reject(text);
        i++;
        int dayStart=-1,dayEnd=-1,hourStart=-1,hourEnd=-1,minuteStart=-1,minuteEnd=-1;
        int secondStart=-1,secondEnd=-1,fractionStart=-1,fractionEnd=-1,tStart=-1;
        if(i<length && (isDigit(text.charAt(i)) || text.charAt(i)=='+' || text.charAt(i)=='-')){
            int start=i;i++;
            while(i<length && isDigit(text.charAt(i)))i++;
            if(i==start+1 && !isDigit(text.charAt(start)))return reject(text);
            if(i>=length || (text.charAt(i)!='D' && text.charAt(i)!='d'))return reject(text);
            dayStart=start;dayEnd=i;i++;
        }
        if(i<length && (text.charAt(i)=='T' || text.charAt(i)=='t')){
            tStart=i;i++;
            if(i<length && (isDigit(text.charAt(i)) || text.charAt(i)=='+' || text.charAt(i)=='-')){
                int start=i;int j=i+1;
                while(j<length && isDigit(text.charAt(j)))j++;
                if(hasDigits(text,start,j) && j<length && (text.charAt(j)=='H' || text.charAt(j)=='h')){hourStart=start;hourEnd=j;i=j+1;}
            }
            if(i<length && (isDigit(text.charAt(i)) || text.charAt(i)=='+' || text.charAt(i)=='-')){
                int start=i;int j=i+1;
                while(j<length && isDigit(text.charAt(j)))j++;
                if(hasDigits(text,start,j) && j<length && (text.charAt(j)=='M' || text.charAt(j)=='m')){minuteStart=start;minuteEnd=j;i=j+1;}
            }
            if(i<length){
                char c=text.charAt(i);
                if(isDigit(c) || c=='+' || c=='-'){
                    secondStart=i;i++;
                    while(i<length && isDigit(text.charAt(i)))i++;
                    if(!hasDigits(text,secondStart,i))return reject(text);
                    if(i<length && (text.charAt(i)=='.' || text.charAt(i)==',')){
                        secondEnd=i;
                        fractionStart=i+1;
                        i++;
                        while(i<length && isDigit(text.charAt(i)))i++;
                        fractionEnd=i;
                        if(fractionEnd-fractionStart>9)return reject(text);
                    }else secondEnd=i;
                    if(i>=length || (text.charAt(i)!='S' && text.charAt(i)!='s'))return reject(text);
                    i++;
                }else if(c=='.' || c==',')return reject(text);
            }
        }
        if(i!=length)return reject(text);
        if(tStart>=0 && hourStart<0 && minuteStart<0 && secondStart<0 && text.charAt(tStart)=='T')return reject(text);
        if(dayStart<0 && hourStart<0 && minuteStart<0 && secondStart<0)return reject(text);
        long daysAsSecs=parseNumber(text,dayStart,dayEnd,SECONDS_PER_DAY,"days");
        long hoursAsSecs=parseNumber(text,hourStart,hourEnd,SECONDS_PER_HOUR,"hours");
        long minsAsSecs=parseNumber(text,minuteStart,minuteEnd,SECONDS_PER_MINUTE,"minutes");
        long seconds=parseNumber(text,secondStart,secondEnd,1,"seconds");
        boolean negativeSecs=secondStart>=0 && text.charAt(secondStart)=='-';
        int nanos=parseFraction(text,fractionStart,fractionEnd,negativeSecs?-1:1);
        try{
            long total=Math.addExact(daysAsSecs,Math.addExact(hoursAsSecs,Math.addExact(minsAsSecs,seconds)));
            return negate?ofSeconds(total,nanos).negated():ofSeconds(total,nanos);
        }catch(ArithmeticException ex){
            throw new DateTimeParseException("Text cannot be parsed to a Duration: overflow",text,0,ex);
        }
    }

    private static Duration reject(CharSequence text){
        throw new DateTimeParseException("Text cannot be parsed to a Duration",text,0);
    }

    private static boolean isDigit(char c){return c>='0' && c<='9';}

    /** A number group is [-+]?[0-9]+ as in the JDK pattern: a lone sign is not a number. */
    private static boolean hasDigits(CharSequence text,int start,int end){
        return end>start+1 || (end==start+1 && isDigit(text.charAt(start)));
    }

    private static long parseNumber(CharSequence text,int start,int end,long multiplier,String errorText){
        if(start<0)return 0;
        try{
            return Math.multiplyExact(Long.parseLong(text,start,end,10),multiplier);
        }catch(NumberFormatException ex){
            throw new DateTimeParseException("Text cannot be parsed to a Duration: "+errorText,text,0,ex);
        }catch(ArithmeticException ex){
            throw new DateTimeParseException("Text cannot be parsed to a Duration: "+errorText,text,0,ex);
        }
    }

    private static int parseFraction(CharSequence text,int start,int end,int negate){
        if(start<0 || start>=end)return 0;
        int value=Integer.parseInt(text,start,end,10);
        for(int digits=end-start;digits<9;digits++)value*=10;
        return value*negate;
    }

    public boolean isZero(){return (seconds|nanos)==0;}
    public boolean isNegative(){return seconds<0;}
    public long getSeconds(){return seconds;}
    public int getNano(){return nanos;}

    public Duration plus(Duration duration){return plus(duration.seconds,duration.nanos);}
    public Duration plusDays(long daysToAdd){return plus(Math.multiplyExact(daysToAdd,SECONDS_PER_DAY),0);}
    public Duration plusHours(long hoursToAdd){return plus(Math.multiplyExact(hoursToAdd,SECONDS_PER_HOUR),0);}
    public Duration plusMinutes(long minutesToAdd){return plus(Math.multiplyExact(minutesToAdd,SECONDS_PER_MINUTE),0);}
    public Duration plusSeconds(long secondsToAdd){return plus(secondsToAdd,0);}
    public Duration plusMillis(long millisToAdd){return plus(millisToAdd/1000,(millisToAdd%1000)*NANOS_PER_MILLI);}
    public Duration plusNanos(long nanosToAdd){return plus(0,nanosToAdd);}

    private Duration plus(long secondsToAdd,long nanosToAdd){
        if((secondsToAdd|nanosToAdd)==0)return this;
        long epochSec=Math.addExact(this.seconds,secondsToAdd);
        epochSec=Math.addExact(epochSec,nanosToAdd/NANOS_PER_SECOND);
        nanosToAdd=nanosToAdd%NANOS_PER_SECOND;
        return ofSeconds(epochSec,nanos+(int)nanosToAdd);
    }

    public Duration minus(Duration duration){
        long s=duration.seconds;
        int n=duration.nanos;
        if(s==Long.MIN_VALUE)return plus(Long.MAX_VALUE,-n).plus(1,0);
        return plus(-s,-n);
    }
    public Duration minusDays(long daysToSubtract){
        return daysToSubtract==Long.MIN_VALUE ? plusDays(Long.MAX_VALUE).plusDays(1) : plusDays(-daysToSubtract);
    }
    public Duration minusHours(long hoursToSubtract){
        return hoursToSubtract==Long.MIN_VALUE ? plusHours(Long.MAX_VALUE).plusHours(1) : plusHours(-hoursToSubtract);
    }
    public Duration minusMinutes(long minutesToSubtract){
        return minutesToSubtract==Long.MIN_VALUE ? plusMinutes(Long.MAX_VALUE).plusMinutes(1) : plusMinutes(-minutesToSubtract);
    }
    public Duration minusSeconds(long secondsToSubtract){
        return secondsToSubtract==Long.MIN_VALUE ? plusSeconds(Long.MAX_VALUE).plusSeconds(1) : plusSeconds(-secondsToSubtract);
    }
    public Duration minusMillis(long millisToSubtract){
        return millisToSubtract==Long.MIN_VALUE ? plusMillis(Long.MAX_VALUE).plusMillis(1) : plusMillis(-millisToSubtract);
    }
    public Duration minusNanos(long nanosToSubtract){
        return nanosToSubtract==Long.MIN_VALUE ? plusNanos(Long.MAX_VALUE).plusNanos(1) : plusNanos(-nanosToSubtract);
    }

    public Duration multipliedBy(long multiplicand){
        if(multiplicand==0)return ZERO;
        if(multiplicand==1)return this;
        BigDecimal secs=toBigDecimalSeconds();
        BigDecimal result=secs.multiply(BigDecimal.valueOf(multiplicand));
        return create(result);
    }

    public Duration dividedBy(long divisor){
        if(divisor==0)throw new ArithmeticException("Cannot divide by zero");
        if(divisor==1)return this;
        BigDecimal secs=toBigDecimalSeconds();
        BigDecimal result=secs.divide(BigDecimal.valueOf(divisor),RoundingMode.DOWN);
        return create(result);
    }

    public Duration negated(){return multipliedBy(-1);}

    public Duration abs(){return isNegative()?negated():this;}

    private BigDecimal toBigDecimalSeconds(){
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos,9));
    }

    public long toDays(){return seconds/SECONDS_PER_DAY;}
    public long toHours(){return seconds/SECONDS_PER_HOUR;}
    public long toMinutes(){return seconds/SECONDS_PER_MINUTE;}
    public long toSeconds(){return seconds;}

    public long toMillis(){
        long s=seconds,n=nanos;
        if(s<0){s=s+1;n=n-NANOS_PER_SECOND;}
        return Math.addExact(Math.multiplyExact(s,1000),n/1_000_000);
    }

    public long toNanos(){
        long s=seconds,n=nanos;
        if(s<0){s=s+1;n=n-NANOS_PER_SECOND;}
        return Math.addExact(Math.multiplyExact(s,NANOS_PER_SECOND),n);
    }

    public int compareTo(Duration other){
        int cmp=Long.compare(this.seconds,other.seconds);
        if(cmp==0)cmp=this.nanos-other.nanos;
        return cmp;
    }

    public boolean equals(Object obj){
        if(obj==this)return true;
        if(obj instanceof Duration){
            Duration other=(Duration)obj;
            return other.seconds==this.seconds && other.nanos==this.nanos;
        }
        return false;
    }

    public int hashCode(){return (int)(seconds^(seconds>>>32))+(51*nanos);}

    public String toString(){
        if(this==ZERO)return "PT0S";
        long effectiveTotalSecs=seconds;
        if(seconds<0 && nanos>0)effectiveTotalSecs++;
        long hours=effectiveTotalSecs/3600;
        int minutes=(int)((effectiveTotalSecs%3600)/60);
        int secs=(int)(effectiveTotalSecs%60);
        StringBuilder buf=new StringBuilder(24);
        buf.append("PT");
        if(hours!=0)buf.append(hours).append('H');
        if(minutes!=0)buf.append(minutes).append('M');
        if(secs==0 && nanos==0 && buf.length()>2)return buf.toString();
        if(seconds<0 && nanos>0){if(secs==0)buf.append("-0");else buf.append(secs);}
        else buf.append(secs);
        if(nanos>0){
            int pos=buf.length();
            if(seconds<0)buf.append(2*1_000_000_000L-nanos);else buf.append(nanos+1_000_000_000L);
            while(buf.charAt(buf.length()-1)=='0')buf.setLength(buf.length()-1);
            buf.setCharAt(pos,'.');
        }
        buf.append('S');
        return buf.toString();
    }
}
