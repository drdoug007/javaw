package java.sql;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SQLException extends Exception implements Iterable<Throwable> {
    private String SQLState;
    private int vendorCode;
    private SQLException next;

    public SQLException() { super(); }
    public SQLException(String reason) { super(reason); }
    public SQLException(String reason, String SQLState) { super(reason); this.SQLState = SQLState; }
    public SQLException(String reason, String SQLState, int vendorCode) { super(reason); this.SQLState = SQLState; this.vendorCode = vendorCode; }
    public SQLException(Throwable cause) { super(cause); }
    public SQLException(String reason, Throwable cause) { super(reason, cause); }
    public SQLException(String reason, String SQLState, Throwable cause) { super(reason, cause); this.SQLState = SQLState; }
    public SQLException(String reason, String SQLState, int vendorCode, Throwable cause) { super(reason, cause); this.SQLState = SQLState; this.vendorCode = vendorCode; }

    public String getSQLState() { return SQLState; }
    public int getErrorCode() { return vendorCode; }
    public SQLException getNextException() { return next; }
    public void setNextException(SQLException ex) {
        SQLException e = this;
        while (e.next != null) e = e.next;
        e.next = ex;
    }

    public Iterator<Throwable> iterator() {
        return new Iterator<Throwable>() {
            SQLException firstException = SQLException.this;
            SQLException nextException = firstException.getNextException();
            Throwable cause = firstException.getCause();
            public boolean hasNext() { return firstException != null || nextException != null || cause != null; }
            public Throwable next() {
                if (firstException != null) { Throwable r = firstException; firstException = null; return r; }
                if (cause != null) { Throwable r = cause; cause = cause.getCause(); return r; }
                if (nextException != null) {
                    Throwable r = nextException;
                    cause = nextException.getCause();
                    nextException = nextException.getNextException();
                    return r;
                }
                throw new NoSuchElementException();
            }
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }
}
