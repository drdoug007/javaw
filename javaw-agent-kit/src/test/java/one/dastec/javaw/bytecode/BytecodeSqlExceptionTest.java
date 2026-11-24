package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T57: the java.sql exception family. Expected values come from the JVM; see tasks/T57-sql-exception.md. */
class BytecodeSqlExceptionTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.sql.*;
        import java.util.Iterator;
        import java.util.NoSuchElementException;

        /** Logs the overridable calls the iterator and getNextWarning make. */
        class Spy extends SQLException {
            final StringBuilder log=new StringBuilder();
            Spy(String m,Throwable c){super(m,c);}
            public SQLException getNextException(){log.append("next;");return super.getNextException();}
            public Throwable getCause(){log.append("cause;");return super.getCause();}
        }
        class SpyWarning extends SQLWarning {
            final StringBuilder log=new StringBuilder();
            SpyWarning(String m){super(m);}
            public SQLException getNextException(){log.append("next;");return super.getNextException();}
        }
        /** Like H2's JdbcSQLException: a vendor subclass that formats its own message. */
        class VendorException extends SQLSyntaxErrorException {
            VendorException(String m,String state,int code,Throwable cause){super(m,state,code);initCause(cause);}
            public String toString(){return "Vendor["+getSQLState()+"/"+getErrorCode()+"] "+getMessage();}
        }
        public class Fixture {
            static String d(Throwable t){
                if(t==null)return "-";
                String s=t.getClass().getName()+"{"+t.getMessage();
                if(t instanceof SQLException){SQLException e=(SQLException)t;s+="|"+e.getSQLState()+"|"+e.getErrorCode();}
                Throwable c=t.getCause();
                return s+"|"+(c==null?"-":c.getMessage())+"}";
            }
            static String names(Iterable<Throwable> chain){
                StringBuilder out=new StringBuilder();
                for(Throwable t:chain)out.append(t.getMessage()).append(' ');
                return out.toString().trim();
            }

            public static String constructors(){
                IllegalStateException c=new IllegalStateException("io");
                SQLException[] all={new SQLException(),new SQLException("r"),new SQLException("r","S1"),
                    new SQLException("r","S1",7),new SQLException(c),new SQLException("r",c),
                    new SQLException("r","S1",c),new SQLException("r","S1",-3,c),new SQLException((Throwable)null),
                    new SQLException(null,null,0,null)};
                StringBuilder out=new StringBuilder();
                for(SQLException e:all)out.append(d(e)).append(';');
                out.append(new SQLException("r","S1",7)).append(';').append(new SQLException()).append(';');
                out.append(new SQLException(c)).append(';');
                out.append((Object)all[0] instanceof Exception).append(',').append((Object)all[0] instanceof RuntimeException).append(',');
                out.append((Object)all[0] instanceof Iterable);
                return out.toString();
            }
            public static String nextChain(){
                SQLException a=new SQLException("a"),b=new SQLException("b"),c=new SQLException("c");
                StringBuilder out=new StringBuilder();
                out.append(a.getNextException()).append(';');
                a.setNextException(b);a.setNextException(c);
                out.append(a.getNextException().getMessage()).append(',').append(b.getNextException().getMessage()).append(',').append(c.getNextException()).append(';');
                a.setNextException(null);
                out.append(c.getNextException()).append(';');
                SQLException d=new SQLException("d");d.setNextException(new SQLException("e"));
                b.setNextException(d);
                out.append(c.getNextException().getMessage()).append(',').append(c.getNextException().getNextException().getMessage()).append(';');
                int n=0;for(SQLException e=a;e!=null;e=e.getNextException())n++;
                out.append(n);
                return out.toString();
            }
            public static String iteration(){
                SQLException root=new SQLException("root",new IllegalArgumentException("c1",new IllegalStateException("c2")));
                SQLException second=new SQLException("second",new SQLException("c3"));
                root.setNextException(second);
                root.setNextException(new SQLWarning("warn"));
                StringBuilder out=new StringBuilder();
                out.append(names(root)).append(';');
                out.append(names(second)).append(';');
                out.append(names(new SQLException("alone"))).append(';');
                Iterator<Throwable> it=new SQLException("one").iterator();
                out.append(it.hasNext()).append(',').append(it.next().getMessage()).append(',').append(it.hasNext()).append(',');
                try{it.next();out.append("no");}catch(NoSuchElementException e){out.append("nse:").append(e.getMessage());}
                out.append(',');
                Iterator<Throwable> r=root.iterator();
                try{r.remove();out.append("no");}catch(UnsupportedOperationException e){out.append("uoe:").append(e.getMessage());}
                out.append(',').append(r.next().getMessage());
                return out.toString();
            }
            public static String iteratorSnapshot(){
                StringBuilder out=new StringBuilder();
                SQLException a=new SQLException("a");
                Iterator<Throwable> early=a.iterator();
                a.setNextException(new SQLException("b"));
                a.setNextException(new SQLException("c"));
                while(early.hasNext())out.append(early.next().getMessage());
                out.append(';');
                Iterator<Throwable> late=a.iterator();
                out.append(late.next().getMessage()).append(late.next().getMessage());
                a.setNextException(new SQLException("d"));
                while(late.hasNext())out.append(late.next().getMessage());
                out.append(';');
                SQLException x=new SQLException("x");
                Iterator<Throwable> nc=x.iterator();
                x.initCause(new IllegalStateException("late-cause"));
                while(nc.hasNext())out.append(nc.next().getMessage()).append(' ');
                out.append(';').append(names(x));
                return out.toString();
            }
            public static String overridableCalls(){
                StringBuilder out=new StringBuilder();
                Spy s=new Spy("s",new IllegalStateException("k"));
                s.setNextException(new SQLException("n",new IllegalStateException("m")));
                out.append(s.log).append('|');s.log.setLength(0);
                Iterator<Throwable> it=s.iterator();
                out.append(s.log).append('|');s.log.setLength(0);
                while(it.hasNext())out.append(it.next().getMessage());
                out.append('|').append(s.log).append('|');s.log.setLength(0);
                SpyWarning w=new SpyWarning("w");
                w.setNextWarning(new SQLWarning("w2"));
                out.append(w.log).append('|');
                out.append(w.getNextWarning().getMessage()).append(w.log);
                return out.toString();
            }
            public static String warnings(){
                StringBuilder out=new StringBuilder();
                IllegalStateException c=new IllegalStateException("io");
                SQLWarning[] all={new SQLWarning(),new SQLWarning("r"),new SQLWarning("r","01000"),
                    new SQLWarning("r","01000",5),new SQLWarning(c),new SQLWarning("r",c),
                    new SQLWarning("r","01000",c),new SQLWarning("r","01000",9,c)};
                for(SQLWarning w:all)out.append(d(w)).append(';');
                SQLWarning w1=new SQLWarning("w1"),w2=new SQLWarning("w2","01004"),w3=new SQLWarning("w3");
                out.append(w1.getNextWarning()).append(';');
                w1.setNextWarning(w2);w1.setNextWarning(w3);
                for(SQLWarning w=w1;w!=null;w=w.getNextWarning())
                    out.append(w.getMessage()).append('/').append(w.getSQLState()).append('/').append(w.getErrorCode()).append(' ');
                out.append(';').append(w1.getNextException()==w2).append(',').append(names(w1)).append(';');
                SQLWarning bad=new SQLWarning("bad");
                bad.setNextException(new SQLException("plain"));
                try{bad.getNextWarning();out.append("no");}
                catch(Error e){out.append(e.getClass().getName()).append(':').append(e.getMessage()).append(':').append(e.getCause());}
                out.append(';').append((Object)w1 instanceof SQLException).append(',').append(w1);
                return out.toString();
            }
            static SQLException[] family(String m,String s,int v,Throwable c){
                return new SQLException[]{new SQLNonTransientException(m,s,v,c),new SQLTransientException(m,s,v,c),
                    new SQLDataException(m,s,v,c),new SQLFeatureNotSupportedException(m,s,v,c),
                    new SQLIntegrityConstraintViolationException(m,s,v,c),new SQLInvalidAuthorizationSpecException(m,s,v,c),
                    new SQLNonTransientConnectionException(m,s,v,c),new SQLSyntaxErrorException(m,s,v,c),
                    new SQLTimeoutException(m,s,v,c),new SQLTransactionRollbackException(m,s,v,c),
                    new SQLTransientConnectionException(m,s,v,c)};
            }
            public static String subclassHierarchy(){
                StringBuilder out=new StringBuilder();
                for(SQLException e:family("m","42000",17,new IllegalStateException("k"))){
                    out.append(e).append('|').append(e.getSQLState()).append('|').append(e.getErrorCode()).append('|')
                       .append(e.getCause().getMessage()).append('|')
                       .append(e instanceof SQLNonTransientException).append(e instanceof SQLTransientException)
                       .append(e instanceof SQLWarning).append(';');
                }
                return out.toString();
            }
            public static String subclassConstructors(){
                StringBuilder out=new StringBuilder();
                Throwable c=new IllegalArgumentException("arg");
                out.append(d(new SQLNonTransientException())).append(d(new SQLNonTransientException("a"))).append(d(new SQLNonTransientException("a","s")))
                   .append(d(new SQLNonTransientException("a","s",1))).append(d(new SQLNonTransientException(c))).append(d(new SQLNonTransientException("a",c)))
                   .append(d(new SQLNonTransientException("a","s",c))).append(';');
                out.append(d(new SQLTransientException())).append(d(new SQLTransientException("a"))).append(d(new SQLTransientException("a","s")))
                   .append(d(new SQLTransientException("a","s",1))).append(d(new SQLTransientException(c))).append(d(new SQLTransientException("a",c)))
                   .append(d(new SQLTransientException("a","s",c))).append(';');
                out.append(d(new SQLDataException())).append(d(new SQLDataException("a"))).append(d(new SQLDataException("a","s")))
                   .append(d(new SQLDataException("a","s",2))).append(d(new SQLDataException(c))).append(d(new SQLDataException("a",c)))
                   .append(d(new SQLDataException("a","s",c))).append(';');
                out.append(d(new SQLFeatureNotSupportedException())).append(d(new SQLFeatureNotSupportedException("a"))).append(d(new SQLFeatureNotSupportedException("a","s")))
                   .append(d(new SQLFeatureNotSupportedException("a","s",3))).append(d(new SQLFeatureNotSupportedException(c))).append(d(new SQLFeatureNotSupportedException("a",c)))
                   .append(d(new SQLFeatureNotSupportedException("a","s",c))).append(';');
                out.append(d(new SQLIntegrityConstraintViolationException())).append(d(new SQLIntegrityConstraintViolationException("a"))).append(d(new SQLIntegrityConstraintViolationException("a","s")))
                   .append(d(new SQLIntegrityConstraintViolationException("a","s",4))).append(d(new SQLIntegrityConstraintViolationException(c))).append(d(new SQLIntegrityConstraintViolationException("a",c)))
                   .append(d(new SQLIntegrityConstraintViolationException("a","s",c))).append(';');
                out.append(d(new SQLInvalidAuthorizationSpecException())).append(d(new SQLInvalidAuthorizationSpecException("a"))).append(d(new SQLInvalidAuthorizationSpecException("a","s")))
                   .append(d(new SQLInvalidAuthorizationSpecException("a","s",5))).append(d(new SQLInvalidAuthorizationSpecException(c))).append(d(new SQLInvalidAuthorizationSpecException("a",c)))
                   .append(d(new SQLInvalidAuthorizationSpecException("a","s",c))).append(';');
                out.append(d(new SQLNonTransientConnectionException())).append(d(new SQLNonTransientConnectionException("a"))).append(d(new SQLNonTransientConnectionException("a","s")))
                   .append(d(new SQLNonTransientConnectionException("a","s",6))).append(d(new SQLNonTransientConnectionException(c))).append(d(new SQLNonTransientConnectionException("a",c)))
                   .append(d(new SQLNonTransientConnectionException("a","s",c))).append(';');
                out.append(d(new SQLSyntaxErrorException())).append(d(new SQLSyntaxErrorException("a"))).append(d(new SQLSyntaxErrorException("a","s")))
                   .append(d(new SQLSyntaxErrorException("a","s",7))).append(d(new SQLSyntaxErrorException(c))).append(d(new SQLSyntaxErrorException("a",c)))
                   .append(d(new SQLSyntaxErrorException("a","s",c))).append(';');
                out.append(d(new SQLTimeoutException())).append(d(new SQLTimeoutException("a"))).append(d(new SQLTimeoutException("a","s")))
                   .append(d(new SQLTimeoutException("a","s",8))).append(d(new SQLTimeoutException(c))).append(d(new SQLTimeoutException("a",c)))
                   .append(d(new SQLTimeoutException("a","s",c))).append(';');
                out.append(d(new SQLTransactionRollbackException())).append(d(new SQLTransactionRollbackException("a"))).append(d(new SQLTransactionRollbackException("a","s")))
                   .append(d(new SQLTransactionRollbackException("a","s",9))).append(d(new SQLTransactionRollbackException(c))).append(d(new SQLTransactionRollbackException("a",c)))
                   .append(d(new SQLTransactionRollbackException("a","s",c))).append(';');
                out.append(d(new SQLTransientConnectionException())).append(d(new SQLTransientConnectionException("a"))).append(d(new SQLTransientConnectionException("a","s")))
                   .append(d(new SQLTransientConnectionException("a","s",10))).append(d(new SQLTransientConnectionException(c))).append(d(new SQLTransientConnectionException("a",c)))
                   .append(d(new SQLTransientConnectionException("a","s",c)));
                return out.toString();
            }
            static void fail(int kind) throws SQLException {
                if(kind==0)throw new SQLSyntaxErrorException("syntax","42000",42102);
                if(kind==1)throw new SQLTimeoutException("slow","HYT00",57014);
                if(kind==2)throw new SQLFeatureNotSupportedException();
                if(kind==3)throw new VendorException("dup","23505",23505,new IllegalStateException("root"));
                throw new SQLException("plain","HY000",1);
            }
            public static String libraryPatterns(){
                StringBuilder out=new StringBuilder();
                for(int k=0;k<5;k++){
                    try{fail(k);}
                    catch(SQLTransientException e){out.append("transient:").append(e.getMessage()).append(';');}
                    catch(SQLNonTransientException e){out.append("nontransient:").append(e).append(';');}
                    catch(SQLException e){out.append("sql:").append(e.getErrorCode()).append(';');}
                }
                // HikariPool.createTimeoutException
                SQLException original=new SQLException("refused","08001");
                SQLTransientConnectionException timeout=new SQLTransientConnectionException("pool - Connection is not available","08001",original);
                timeout.setNextException(original);
                out.append(d(timeout)).append(',').append(d(timeout.getNextException())).append(',').append(names(timeout)).append(';');
                // Spring SQLErrorCodeSQLExceptionTranslator: dig for a SQL state through getNextException
                SQLException outer=new SQLException("outer");
                outer.setNextException(new SQLException("inner","23000",1062));
                SQLException cur=outer;String state=cur.getSQLState();
                while(state==null&&cur.getNextException()!=null){cur=cur.getNextException();state=cur.getSQLState();}
                out.append(state).append('/').append(cur.getErrorCode()).append(';');
                SQLException main=new SQLException("main");
                main.addSuppressed(new SQLException("close"));
                out.append(main.getSuppressed().length).append(main.getSuppressed()[0].getMessage());
                return out.toString();
            }
        }
        """;

    @Test void sqlExceptionBasics() throws Exception {
        matchesJvm(FIXTURE, "constructors", "nextChain");
    }

    @Test void chainIteration() throws Exception {
        matchesJvm(FIXTURE, "iteration", "iteratorSnapshot", "overridableCalls");
    }

    @Test void warnings() throws Exception {
        matchesJvm(FIXTURE, "warnings");
    }

    @Test void subclasses() throws Exception {
        matchesJvm(FIXTURE, "subclassHierarchy", "subclassConstructors", "libraryPatterns");
    }

    @Test void leavesOutBatchClientInfoAndDriverManager() {
        rejectsUnusedCall("java/sql/BatchUpdateException", "<init>", "(Ljava/lang/String;Ljava/lang/String;I[I)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/sql/SQLClientInfoException", "<init>", "(Ljava/lang/String;Ljava/util/Map;)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/sql/SQLRecoverableException", "<init>", "(Ljava/lang/String;)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/sql/DriverManager", "getLogWriter", "()Ljava/io/PrintWriter;", INVOKESTATIC, false);
    }
}
