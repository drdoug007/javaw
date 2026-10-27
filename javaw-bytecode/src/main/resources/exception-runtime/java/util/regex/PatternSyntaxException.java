package java.util.regex;
/** Selected Java 17 regex diagnostic shape. */
public class PatternSyntaxException extends IllegalArgumentException {
    private final String description,pattern;private final int index;
    public PatternSyntaxException(String description,String pattern,int index){this.description=description;this.pattern=pattern;this.index=index;}
    public String getDescription(){return description;}public String getPattern(){return pattern;}public int getIndex(){return index;}
    public String getMessage(){StringBuilder out=new StringBuilder().append(description);if(index>=0)out.append(" near index ").append(index);out.append('\n').append(pattern);if(index>=0){out.append('\n');for(int i=0;i<index;i++)out.append(' ');out.append('^');}return out.toString();}
}
