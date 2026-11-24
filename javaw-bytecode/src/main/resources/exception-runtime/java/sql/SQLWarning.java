package java.sql;

public class SQLWarning extends SQLException {
    public SQLWarning() { super(); }
    public SQLWarning(String reason) { super(reason); }
    public SQLWarning(String reason, String SQLState) { super(reason, SQLState); }
    public SQLWarning(String reason, String SQLState, int vendorCode) { super(reason, SQLState, vendorCode); }
    public SQLWarning(Throwable cause) { super(cause); }
    public SQLWarning(String reason, Throwable cause) { super(reason, cause); }
    public SQLWarning(String reason, String SQLState, Throwable cause) { super(reason, SQLState, cause); }
    public SQLWarning(String reason, String SQLState, int vendorCode, Throwable cause) { super(reason, SQLState, vendorCode, cause); }

    public SQLWarning getNextWarning() {
        try {
            return (SQLWarning) getNextException();
        }
        catch (ClassCastException ex) {
            throw new Error("SQLWarning chain holds value that is not a SQLWarning");
        }
    }
    public void setNextWarning(SQLWarning w) { setNextException(w); }
}
