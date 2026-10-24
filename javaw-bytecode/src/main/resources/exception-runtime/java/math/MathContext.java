package java.math;

/** Immutable precision and rounding policy for bundled decimal arithmetic. */
public final class MathContext implements java.io.Serializable {
    private final int precision;
    private final RoundingMode roundingMode;
    public static final MathContext UNLIMITED=new MathContext(0,RoundingMode.HALF_UP);
    public static final MathContext DECIMAL32=new MathContext(7,RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL64=new MathContext(16,RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL128=new MathContext(34,RoundingMode.HALF_EVEN);
    public MathContext(int precision){this(precision,RoundingMode.HALF_UP);}
    public MathContext(int precision,RoundingMode mode){
        if(precision<0)throw new IllegalArgumentException();
        if(mode==null)throw new NullPointerException();
        this.precision=precision;roundingMode=mode;
    }
    public MathContext(String text){
        if(text==null)throw new NullPointerException();
        int digits;RoundingMode mode;
        try{
            if(!text.startsWith("precision="))throw new IllegalArgumentException();
            int space=text.indexOf(' ');
            digits=Integer.parseInt(text.substring(10,space));
            if(!text.startsWith("roundingMode=",space+1))throw new IllegalArgumentException();
            String name=text.substring(space+14);mode=null;
            for(RoundingMode candidate:RoundingMode.values())if(candidate.name().equals(name))mode=candidate;
            if(mode==null)throw new IllegalArgumentException();
        }catch(RuntimeException failure){throw new IllegalArgumentException();}
        if(digits<0)throw new IllegalArgumentException();
        precision=digits;roundingMode=mode;
    }
    public int getPrecision(){return precision;}
    public RoundingMode getRoundingMode(){return roundingMode;}
    public boolean equals(Object other){return other instanceof MathContext && precision==((MathContext)other).precision && roundingMode==((MathContext)other).roundingMode;}
    public int hashCode(){return precision+59*roundingMode.hashCode();}
    public String toString(){return "precision="+precision+" roundingMode="+roundingMode.toString();}
}
