package java.util;

/** Selected language/country/variant identities; the isolated default is ROOT. */
public final class Locale implements Cloneable,java.io.Serializable {
    private final String language,country,variant;
    public static final Locale ROOT=new Locale("","");
    public static final Locale ENGLISH=new Locale("en"),FRENCH=new Locale("fr"),GERMAN=new Locale("de"),ITALIAN=new Locale("it"),JAPANESE=new Locale("ja"),KOREAN=new Locale("ko"),CHINESE=new Locale("zh");
    public static final Locale US=new Locale("en","US"),UK=new Locale("en","GB"),CANADA=new Locale("en","CA"),CANADA_FRENCH=new Locale("fr","CA"),FRANCE=new Locale("fr","FR"),GERMANY=new Locale("de","DE"),ITALY=new Locale("it","IT"),JAPAN=new Locale("ja","JP"),KOREA=new Locale("ko","KR"),CHINA=new Locale("zh","CN"),TAIWAN=new Locale("zh","TW");
    public static final Locale SIMPLIFIED_CHINESE=CHINA,PRC=CHINA,TRADITIONAL_CHINESE=TAIWAN;
    public Locale(String language){this(language,"");}
    public Locale(String language,String country){this(language,country,"");}
    public Locale(String language,String country,String variant){
        this.language=modern(ascii(language,false));this.country=ascii(country,true);
        if(variant==null)throw new NullPointerException();this.variant=variant;
        if((this.language.equals("ja") && this.country.equals("JP") && variant.equals("JP")) || (this.language.equals("th") && this.country.equals("TH") && variant.equals("TH")))throw new UnsupportedOperationException("Legacy locale extensions are unsupported");
    }
    private static String ascii(String text,boolean upper){char[] chars=text.toCharArray();for(int i=0;i<chars.length;i++){
        char c=chars[i];if(upper && c>='a' && c<='z')chars[i]=(char)(c-32);else if(!upper && c>='A' && c<='Z')chars[i]=(char)(c+32);}return new String(chars);}
    private static String modern(String language){return language.equals("iw")?"he":language.equals("ji")?"yi":language.equals("in")?"id":language;}
    public String getLanguage(){return language;}
    public String getCountry(){return country;}
    public String getVariant(){return variant;}
    public static Locale getDefault(){return ROOT;}
    public Object clone(){return new Locale(language,country,variant);}
    public boolean equals(Object other){return other instanceof Locale && language.equals(((Locale)other).language) && country.equals(((Locale)other).country) && variant.equals(((Locale)other).variant);}
    public int hashCode(){return ((language.hashCode()*31)*31+country.hashCode())*31+variant.hashCode();}
    public String toString(){String text=language;if(!country.isEmpty() || (!language.isEmpty() && !variant.isEmpty()))text+="_"+country;
        if(!variant.isEmpty() && (!language.isEmpty() || !country.isEmpty()))text+="_"+variant;return text;}
}
