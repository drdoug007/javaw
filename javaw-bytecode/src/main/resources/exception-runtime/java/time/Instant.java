package java.time;

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
}
