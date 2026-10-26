package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class DecimalSupportTest {
    @Test void executesArbitraryPrecisionArithmetic() throws Exception {
        var compilation=JavacToWasm.compile("App.java", """
            class App {
                public static int test() {
                    BigInteger huge=new BigInteger("123456789012345678901234567890");
                    if(!huge.multiply(huge).toString().equals("15241578753238836750495351562536198787501905199875019052100")) return -1;
                    if(!huge.divide(new BigInteger("97")).toString().equals("1272750402189130710322005854")) return -2;
                    if(!BigInteger.valueOf(-9223372036854775808L).toString().equals("-9223372036854775808")) return -3;
                    BigDecimal a=new BigDecimal("9007199254740993.12345678901234567890");
                    if(!a.add(new BigDecimal("0.00000000000000000001")).toPlainString().equals("9007199254740993.12345678901234567891")) return -4;
                    if(!new BigDecimal("1.20").multiply(new BigDecimal("3.0")).toString().equals("3.600")) return -5;
                    if(!new BigDecimal("1").divide(new BigDecimal("8")).toString().equals("0.125")) return -6;
                    if(!new BigDecimal("2.345").setScale(2,RoundingMode.HALF_EVEN).toString().equals("2.34")) return -7;
                    if(!new BigDecimal("-2.345").setScale(2,RoundingMode.FLOOR).toString().equals("-2.35")) return -8;
                    if(!new BigDecimal("12300").stripTrailingZeros().toString().equals("1.23E+4")) return -9;
                    if(new BigDecimal("1.0").equals(new BigDecimal("1.00")) || new BigDecimal("1.0").compareTo(new BigDecimal("1.00"))!=0) return -10;
                    if(!BigDecimal.valueOf(0.1).toString().equals("0.1")) return -11;
                    try { new BigDecimal("1").divide(new BigDecimal("3")); return -12; } catch(ArithmeticException ex) {}
                    try { new BigDecimal("1.1").intValueExact(); return -13; } catch(ArithmeticException ex) {}
                    try { new BigDecimal("1..2"); return -14; } catch(NumberFormatException ex) {}
                    return 42;
                }
            }
            """,false);
        byte[] wasm=compilation.getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
    @Test void matchesJdkRoundingAndBinaryDoubleConstruction() throws Exception {
        StringBuilder source=new StringBuilder("class App { public static int test() {");
        int code=1;
        for(String value:java.util.List.of("2.345","2.355","-2.345","-2.355","0.001","0","99999999999999999999.995")) {
            for(java.math.RoundingMode mode:java.math.RoundingMode.values()) {
                try {
                    String expected=new java.math.BigDecimal(value).setScale(2,mode).toString();
                    source.append("if(!new BigDecimal(\"").append(value).append("\").setScale(2,RoundingMode.").append(mode)
                        .append(").toString().equals(\"").append(expected).append("\")) return -").append(code).append(';');
                } catch(ArithmeticException expected) {
                    source.append("try { new BigDecimal(\"").append(value).append("\").setScale(2,RoundingMode.").append(mode)
                        .append("); return -").append(code).append("; } catch(ArithmeticException e) {}");
                }
                code++;
            }
        }
        for(double value:new double[]{0.1,-1.25,0.0,1e20}) {
            source.append("if(!new BigDecimal(").append(value).append(").toString().equals(\"")
                .append(new java.math.BigDecimal(value)).append("\")) return -").append(code++).append(';');
        }
        source.append("return 42; } }");
        byte[] wasm=JavacToWasm.compile("App.java",source.toString(),false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

}
