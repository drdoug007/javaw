package java.time.format;

import java.time.DateTimeException;

public class DateTimeParseException extends DateTimeException {
    private final String parsedString;
    private final int errorIndex;

    public DateTimeParseException(String message,CharSequence parsedData,int errorIndex){
        this(message,parsedData,errorIndex,null);
    }
    public DateTimeParseException(String message,CharSequence parsedData,int errorIndex,Throwable cause){
        super(message,cause);
        this.parsedString=parsedData.toString();
        this.errorIndex=errorIndex;
    }
    public String getParsedString(){return parsedString;}
    public int getErrorIndex(){return errorIndex;}
}
