package java.time;

import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class Period implements java.io.Serializable {
    public static final Period ZERO=new Period(0,0,0);

    private final int years;
    private final int months;
    private final int days;

    private Period(int years,int months,int days){this.years=years;this.months=months;this.days=days;}

    private static Period create(int years,int months,int days){
        if((years|months|days)==0)return ZERO;
        return new Period(years,months,days);
    }

    public static Period of(int years,int months,int days){return create(years,months,days);}
    public static Period ofYears(int years){return create(years,0,0);}
    public static Period ofMonths(int months){return create(0,months,0);}
    public static Period ofWeeks(int weeks){return create(0,0,Math.multiplyExact(weeks,7));}
    public static Period ofDays(int days){return create(0,0,days);}

    public static Period parse(CharSequence text){
        Objects.requireNonNull(text,"text");
        int length=text.length();
        int i=0;
        int negate=1;
        if(i<length){
            char c=text.charAt(i);
            if(c=='-'){negate=-1;i++;}
            else if(c=='+')i++;
        }
        if(i>=length || (text.charAt(i)!='P' && text.charAt(i)!='p'))return reject(text);
        i++;
        int yearStart=-1,yearEnd=-1,monthStart=-1,monthEnd=-1;
        int weekStart=-1,weekEnd=-1,dayStart=-1,dayEnd=-1;
        int group=-1;
        while(i<length){
            int start=i;
            char c=text.charAt(i);
            if(c=='-' || c=='+')i++;
            if(i>=length || !isDigit(text.charAt(i)))return reject(text);
            while(i<length && isDigit(text.charAt(i)))i++;
            int end=i;
            if(end==length)return reject(text);
            c=text.charAt(end);
            int g;
            if(c=='Y' || c=='y')g=0;
            else if(c=='M' || c=='m')g=1;
            else if(c=='W' || c=='w')g=2;
            else if(c=='D' || c=='d')g=3;
            else return reject(text);
            if(g<=group)return reject(text);
            group=g;
            i=end+1;
            if(g==0){yearStart=start;yearEnd=end;}
            else if(g==1){monthStart=start;monthEnd=end;}
            else if(g==2){weekStart=start;weekEnd=end;}
            else{dayStart=start;dayEnd=end;}
        }
        if(group<0)return reject(text);
        try{
            int years=parseNumber(text,yearStart,yearEnd,negate);
            int months=parseNumber(text,monthStart,monthEnd,negate);
            int weeks=parseNumber(text,weekStart,weekEnd,negate);
            int days=parseNumber(text,dayStart,dayEnd,negate);
            days=Math.addExact(days,Math.multiplyExact(weeks,7));
            return create(years,months,days);
        }catch(NumberFormatException ex){
            throw new DateTimeParseException("Text cannot be parsed to a Period",text,0,ex);
        }
    }

    private static Period reject(CharSequence text){
        throw new DateTimeParseException("Text cannot be parsed to a Period",text,0);
    }

    private static boolean isDigit(char c){return c>='0' && c<='9';}

    /** Group may be absent; a negate overflow is wrapped, like the JDK's parseNumber. */
    private static int parseNumber(CharSequence text,int start,int end,int negate){
        if(start<0)return 0;
        int val=Integer.parseInt(text,start,end,10);
        try{
            return Math.multiplyExact(val,negate);
        }catch(ArithmeticException ex){
            throw new DateTimeParseException("Text cannot be parsed to a Period",text,0,ex);
        }
    }

    public int getYears(){return years;}
    public int getMonths(){return months;}
    public int getDays(){return days;}

    public boolean isZero(){return this==ZERO;}
    public boolean isNegative(){return years<0 || months<0 || days<0;}

    public Period withYears(int years){return years==this.years?this:create(years,this.months,this.days);}
    public Period withMonths(int months){return months==this.months?this:create(this.years,months,this.days);}
    public Period withDays(int days){return days==this.days?this:create(this.years,this.months,days);}

    public Period plusYears(long yearsToAdd){
        return yearsToAdd==0?this:create(Math.toIntExact(Math.addExact(this.years,yearsToAdd)),this.months,this.days);
    }
    public Period plusMonths(long monthsToAdd){
        return monthsToAdd==0?this:create(this.years,Math.toIntExact(Math.addExact(this.months,monthsToAdd)),this.days);
    }
    public Period plusDays(long daysToAdd){
        return daysToAdd==0?this:create(this.years,this.months,Math.toIntExact(Math.addExact(this.days,daysToAdd)));
    }

    public Period minusYears(long yearsToSubtract){
        return yearsToSubtract==Long.MIN_VALUE?plusYears(Long.MAX_VALUE).plusYears(1):plusYears(-yearsToSubtract);
    }
    public Period minusMonths(long monthsToSubtract){
        return monthsToSubtract==Long.MIN_VALUE?plusMonths(Long.MAX_VALUE).plusMonths(1):plusMonths(-monthsToSubtract);
    }
    public Period minusDays(long daysToSubtract){
        return daysToSubtract==Long.MIN_VALUE?plusDays(Long.MAX_VALUE).plusDays(1):plusDays(-daysToSubtract);
    }

    public Period multipliedBy(int scalar){
        if(this==ZERO || scalar==1)return this;
        return create(Math.multiplyExact(years,scalar),Math.multiplyExact(months,scalar),Math.multiplyExact(days,scalar));
    }
    public Period negated(){return multipliedBy(-1);}

    public Period normalized(){
        long total=toTotalMonths();
        long y=total/12;
        int m=(int)(total%12);
        if(y==years && m==months)return this;
        return create(Math.toIntExact(y),m,days);
    }

    public long toTotalMonths(){return years*12L+months;}

    public boolean equals(Object obj){
        if(obj==this)return true;
        if(obj instanceof Period){
            Period other=(Period)obj;
            return other.years==years && other.months==months && other.days==days;
        }
        return false;
    }

    public int hashCode(){return years+Integer.rotateLeft(months,8)+Integer.rotateLeft(days,16);}

    public String toString(){
        if(this==ZERO)return "P0D";
        StringBuilder buf=new StringBuilder(19);
        buf.append('P');
        if(years!=0)buf.append(years).append('Y');
        if(months!=0)buf.append(months).append('M');
        if(days!=0)buf.append(days).append('D');
        return buf.toString();
    }
}
