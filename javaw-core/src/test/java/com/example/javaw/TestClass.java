package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;
import com.example.javaw.dom.CanvasRenderingContext2D;
import com.example.javaw.dom.WebGLRenderingContext;
import com.example.javaw.dom.Window;
import com.example.javaw.dom.JSObject;
import com.example.javaw.dom.JSValue;
import com.example.javaw.dom.JSNumber;
import com.example.javaw.dom.JSBoolean;
import com.example.javaw.dom.JSString;
import com.example.javaw.dom.JSBigInt;
import com.example.javaw.dom.JSSymbol;
import com.example.javaw.dom.JSUndefined;
import com.example.javaw.dom.JSError;
import com.example.javaw.dom.JSNull;
import com.example.javaw.wasi.WASI;
import com.example.javaw.wasi.WASIFile;
import com.example.javaw.wasi.WASITerminal;
import com.example.javaw.terminal.Terminal;
import com.example.javaw.simd.Float32x4;
import com.example.javaw.simd.Int32x4;
import com.example.javaw.memory.Memory;
import com.example.javaw.js.JS;
import java.util.Arrays;

@JS.Code("globalThis.__bundled_test_var = 777; function bundledCalc(n) { return n * 3; }")
@JS.Resource(value = "body { background-color: #f0f0f0; }", path = "theme.css")
@JS.Resource(value = "{\"theme\":\"dark\",\"port\":8080}", path = "config.json")
public class TestClass {
    public static int asyncCounter = 0;
    public static String asyncFetchedData = "";

    public void onTimerTick() {
        asyncCounter += 10;
    }

    public void onFrameUpdate(double timestamp) {
        asyncCounter += 5;
    }

    public void onFetchSuccess(String data) {
        asyncFetchedData = data;
    }

    public int testScheduleTimer(int delay) {
        return Window.setTimeout("onTimerTick", delay);
    }

    public int testScheduleInterval(int delay) {
        return Window.setInterval("onTimerTick", delay);
    }

    public int testRequestAnimationFrame() {
        return Window.requestAnimationFrame("onFrameUpdate");
    }

    public void testFetchData(String url) {
        Window.fetch(url, "onFetchSuccess");
    }

    public boolean testConfirm(String message) {
        return Window.confirm(message);
    }

    public double testMathHardware(double a, double b) {
        double f = Math.floor(a);
        double c = Math.ceil(a);
        double s = Math.sqrt(b);
        double ab = Math.abs(a);
        double mn = Math.min(a, b);
        double mx = Math.max(a, b);
        double r = Math.rint(a);
        double cp = Math.copySign(a, b);
        return f + c + s + ab + mn + mx + r + cp;
    }

    public double testMathTranscendental(double x) {
        double s = Math.sin(x);
        double c = Math.cos(x);
        double t = Math.tan(x);
        double e = Math.exp(x);
        double l = Math.log(x);
        double p = Math.pow(x, 2.0);
        double h = Math.hypot(x, x);
        return s + c + t + e + l + p + h;
    }

    public double testMathConstants() {
        return Math.PI + Math.E;
    }

    public int testIntegerWrappers(String s, int val) {
        int parsed = Integer.parseInt(s);
        int parsedHex = Integer.parseInt("1A", 16);
        String str = Integer.toString(val);
        String hex = Integer.toHexString(val);
        String bin = Integer.toBinaryString(val);
        String oct = Integer.toOctalString(val);
        int bc = Integer.bitCount(val);
        int clz = Integer.numberOfLeadingZeros(val);
        int ctz = Integer.numberOfTrailingZeros(val);
        int revBytes = Integer.reverseBytes(val);
        int rotl = Integer.rotateLeft(val, 2);
        int rotr = Integer.rotateRight(val, 2);
        int cmp = Integer.compare(val, parsed);
        int sgn = Integer.signum(val);
        return parsed + parsedHex + bc + clz + ctz + cmp + sgn;
    }

    public long testLongWrappers(String s, long val) {
        long parsed = Long.parseLong(s);
        String str = Long.toString(val);
        String hex = Long.toHexString(val);
        String bin = Long.toBinaryString(val);
        int bc = Long.bitCount(val);
        int clz = Long.numberOfLeadingZeros(val);
        int ctz = Long.numberOfTrailingZeros(val);
        long revBytes = Long.reverseBytes(val);
        int cmp = Long.compare(val, parsed);
        int sgn = Long.signum(val);
        return parsed + (long)bc + (long)clz + (long)ctz + (long)cmp + (long)sgn;
    }

    public double testDoubleWrappers(String s, double val) {
        double parsed = Double.parseDouble(s);
        String str = Double.toString(val);
        boolean nan = Double.isNaN(val);
        boolean inf = Double.isInfinite(val);
        boolean fin = Double.isFinite(val);
        int cmp = Double.compare(val, parsed);
        long bits = Double.doubleToLongBits(val);
        double fromBits = Double.longBitsToDouble(bits);
        return parsed + fromBits + (double)cmp + (fin ? 1.0 : 0.0);
    }

    public float testFloatWrappers(String s, float val) {
        float parsed = Float.parseFloat(s);
        String str = Float.toString(val);
        boolean nan = Float.isNaN(val);
        boolean inf = Float.isInfinite(val);
        boolean fin = Float.isFinite(val);
        int bits = Float.floatToIntBits(val);
        float fromBits = Float.intBitsToFloat(bits);
        return parsed + fromBits + (fin ? 1.0f : 0.0f);
    }

    public boolean testBooleanWrappers(String s, boolean val) {
        boolean parsed = Boolean.parseBoolean(s);
        String str = Boolean.toString(val);
        int cmp = Boolean.compare(val, parsed);
        boolean land = Boolean.logicalAnd(val, parsed);
        boolean lor = Boolean.logicalOr(val, parsed);
        boolean lxor = Boolean.logicalXor(val, parsed);
        return parsed && lor;
    }

    public int testCharacterWrappers(char c) {
        boolean isD = Character.isDigit(c);
        boolean isL = Character.isLetter(c);
        boolean isLD = Character.isLetterOrDigit(c);
        boolean isW = Character.isWhitespace(c);
        boolean isU = Character.isUpperCase(c);
        boolean isLow = Character.isLowerCase(c);
        char up = Character.toUpperCase(c);
        char low = Character.toLowerCase(c);
        int d = Character.digit('f', 16);
        char fd = Character.forDigit(15, 16);
        return (isL ? 1 : 0) + (isU ? 2 : 0) + d + (int)up + (int)low;
    }

    public int testBitwiseIntrinsics(int i, long l) {
        int p1 = Integer.bitCount(i);
        int z1 = Integer.numberOfLeadingZeros(i);
        int t1 = Integer.numberOfTrailingZeros(i);
        int r1 = Integer.reverseBytes(i);
        int rb1 = Integer.reverse(i);

        int p2 = Long.bitCount(l);
        int z2 = Long.numberOfLeadingZeros(l);
        int t2 = Long.numberOfTrailingZeros(l);
        long r2 = Long.reverseBytes(l);
        long rb2 = Long.reverse(l);

        return p1 + z1 + t1 + p2 + z2 + t2;
    }

    public void sayHello() {
        // This is a comment
    }

    public int addNumbers(int a, int b) {
        return a + b;
    }

    public double getPi() {
        return 3.14159;
    }

    public int calculateFactorial(int n) {
        int result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public int sumRangeWithSkips(int n) {
        int sum = 0;
        for (int i = 1; i <= n; i++) {
            if (i == 3) {
                continue;
            }
            if (i > 8) {
                break;
            }
            sum += i;
        }
        return sum;
    }

    public int sumArray(int[] numbers) {
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return total;
    }

    public int sumArrayWithSkips(int[] numbers) {
        int total = 0;
        for (int n : numbers) {
            if (n == 3) {
                continue;
            }
            if (n > 8) {
                break;
            }
            total += n;
        }
        return total;
    }

    public double sumDoubleArray(double[] values) {
        double total = 0.0;
        for (double v : values) {
            total += v;
        }
        return total;
    }

    public int sumInlineArray() {
        int total = 0;
        for (int x : new int[]{10, 20, 30, 40}) {
            total += x;
        }
        return total;
    }

    public int createAndSumArray(int a, int b, int c) {
        int[] arr = new int[]{a, b, c};
        int sum = 0;
        for (int x : arr) {
            sum += x;
        }
        return sum;
    }

    public int evaluateSwitch(int code) {
        int result = 0;
        switch (code) {
            case 1:
                result = 100;
                break;
            case 2:
                result = 200;
                break;
            case 3:
                result = 300;
                break;
            default:
                result = -1;
                break;
        }
        return result;
    }

    public int switchFallthrough(int code) {
        int result = 0;
        switch (code) {
            case 1:
                result += 10;
            case 2:
                result += 20;
                break;
            case 3:
                result += 30;
                break;
            default:
                result += 5;
                break;
        }
        return result;
    }

    public int switchInsideLoop(int limit) {
        int total = 0;
        for (int i = 1; i <= limit; i++) {
            switch (i) {
                case 2:
                    total += 20;
                    break;
                case 4:
                    total += 40;
                    break;
                default:
                    total += 1;
                    break;
            }
        }
        return total;
    }

    public int labeledBreakNestedLoops(int maxI, int maxJ) {
        int count = 0;
        outer: for (int i = 0; i < maxI; i++) {
            for (int j = 0; j < maxJ; j++) {
                if (i == 2 && j == 1) {
                    break outer;
                }
                count++;
            }
        }
        return count;
    }

    public int labeledContinueNestedLoops(int maxI, int maxJ) {
        int count = 0;
        outer: for (int i = 0; i < maxI; i++) {
            for (int j = 0; j < maxJ; j++) {
                if (j == 2) {
                    continue outer;
                }
                count++;
            }
        }
        return count;
    }

    public int labeledBlockBreak(int flag) {
        int res = 10;
        myBlock: {
            if (flag == 1) {
                res += 5;
                break myBlock;
            }
            res += 20;
        }
        return res;
    }

    public int labeledWhileBreak(int limit) {
        int sum = 0;
        int i = 0;
        outer: while (i < limit) {
            int j = 0;
            while (j < limit) {
                if (i == 1 && j == 1) {
                    break outer;
                }
                sum++;
                j++;
            }
            i++;
        }
        return sum;
    }

    public String concatStrings() {
        String greeting = "Hello";
        String target = "WebAssembly";
        return greeting + ", " + target + "!";
    }

    public String formatUser(String name, int age) {
        return "User: " + name + " (age: " + age + ")";
    }

    public int getStringLength() {
        String s = "WebAssembly";
        return s.length();
    }

    public int getCharAtIndex(int idx) {
        String s = "WebAssembly";
        return s.charAt(idx);
    }

    public String getSubString(int start, int end) {
        String s = "Hello World";
        return s.substring(start, end);
    }

    public int compareStrings(String a, String b) {
        if (a.equals(b)) {
            return 1;
        }
        return 0;
    }

    public int findSubstringIndex() {
        String s = "Hello World";
        return s.indexOf("World");
    }

    public int findMissingSubstringIndex() {
        String s = "Hello World";
        return s.indexOf("Java");
    }

    public String buildCustomString(int count) {
        com.example.javaw.util.StringBuilder sb = new com.example.javaw.util.StringBuilder("Count: ");
        for (int i = 0; i < count; i++) {
            sb.append(i);
            sb.append(' ');
        }
        return sb.toString();
    }

    public String manipulateStringBuilder() {
        com.example.javaw.util.StringBuilder sb = new com.example.javaw.util.StringBuilder("Java");
        sb.setCharAt(0, 'j');
        sb.append('w');
        return sb.toString();
    }

    public int testPointObject(int x, int y) {
        Point p = new Point(x, y);
        p.setLocation(x + 1, y + 2);
        return p.distanceSq();
    }

    public int testDirectFieldAccess(int x, int y) {
        Point p = new Point(x, y);
        p.x += 5;
        p.y += 10;
        return p.x + p.y;
    }

    public int testPolymorphismArea(int type, int param1, int param2) {
        Shape s;
        if (type == 1) {
            s = new Circle(100, param1);
        } else {
            s = new Rectangle(200, param1, param2);
        }
        return s.area();
    }

    public int testInheritedColor(int type, int color, int param1, int param2) {
        Shape s;
        if (type == 1) {
            s = new Circle(color, param1);
        } else {
            s = new Rectangle(color, param1, param2);
        }
        return s.getColor();
    }

    public int testInstanceOf(int type) {
        Shape s;
        if (type == 1) {
            s = new Circle(10, 5);
        } else {
            s = new Rectangle(20, 4, 6);
        }
        if (s instanceof Circle) {
            return 1;
        } else if (s instanceof Rectangle) {
            return 2;
        }
        return 0;
    }

    public int testObjectArray() {
        Point[] pts = new Point[3];
        pts[0] = new Point(1, 2);
        pts[1] = new Point(3, 4);
        pts[2] = new Point(5, 6);

        int totalDist = 0;
        for (Point p : pts) {
            totalDist += p.distanceSq();
        }
        return totalDist;
    }

    public int testCanvasDraw() {
        Element canvas = DOM.getElementById("testCanvas");
        canvas.setWidth(500);
        canvas.setHeight(300);
        CanvasRenderingContext2D ctx = canvas.getContext2D();
        ctx.setFillStyle("#ff0000");
        ctx.fillRect(10, 20, 100, 50);
        ctx.beginPath();
        ctx.arc(60, 60, 20, 0, 6.28);
        ctx.fill();
        return canvas.getWidth() + canvas.getHeight();
    }

    public int testWebGLInit() {
        Element glCanvas = DOM.getElementById("glCanvas");
        WebGLRenderingContext gl = glCanvas.getWebGLContext();
        gl.clearColor(0.2, 0.4, 0.6, 1.0);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
        return 1;
    }

    public int testJsEvalAndGet() {
        JSObject obj = JSObject.eval("({ title: 'Dynamic JS', count: 42, active: true })");
        String title = obj.getString("title");
        int count = obj.getInt("count");
        boolean active = obj.getBoolean("active");
        if (active && count == 42 && title.length() == 10) {
            return count;
        }
        return 0;
    }

    public int testJsCreateAndSet() {
        JSObject obj = JSObject.createObject();
        obj.set("user", "Alice");
        obj.set("score", 95);
        obj.set("rating", 4.8);
        obj.set("premium", true);
        String user = obj.getString("user");
        int score = obj.getInt("score");
        boolean premium = obj.getBoolean("premium");
        if (premium && user.length() == 5) {
            return score;
        }
        return 0;
    }

    public int testJsArrayOperations() {
        JSObject arr = JSObject.createArray();
        arr.push("item1");
        arr.push("item2");
        arr.push(123);
        int len = arr.length();
        arr.setIndex(1, "updated");
        String item1 = arr.getIndex(1).asString();
        if (len == 3 && item1.length() == 7) {
            return len;
        }
        return 0;
    }

    public int testJsGlobalMathCall() {
        JSObject math = JSObject.global().get("Math");
        int max = math.callInt("max", 100);
        return max;
    }

    public int testWasiOutput() {
        System.out.println("Testing WASI standard output from Java!");
        System.out.print("Progress: ");
        System.out.println(100);
        System.err.println("WASI stderr stream message");
        return 1;
    }

    public int testWasiFacade() {
        com.example.javaw.wasi.WASI.print("WASI Facade message: ");
        com.example.javaw.wasi.WASI.println("Active");
        com.example.javaw.wasi.WASI.println(42);
        com.example.javaw.wasi.WASI.eprintln("WASI Facade Error");
        return 42;
    }

    public int testAtomicIntegerOperations() {
        com.example.javaw.concurrent.AtomicInteger counter = new com.example.javaw.concurrent.AtomicInteger(10);
        int val1 = counter.incrementAndGet(); // 11
        int val2 = counter.addAndGet(5); // 16
        boolean cas = counter.compareAndSet(16, 50); // true, now 50
        int val3 = counter.getAndAdd(10); // 50, now 60
        int finalVal = counter.get(); // 60
        if (cas && val1 == 11 && val2 == 16 && val3 == 50 && finalVal == 60) {
            return 1;
        }
        return 0;
    }

    public int testAtomicsArrayOperations() {
        int[] arr = new int[]{100, 200, 300};
        int prev1 = com.example.javaw.concurrent.Atomics.add(arr, 0, 25); // returns 100, arr[0] is 125
        int prev2 = com.example.javaw.concurrent.Atomics.compareExchange(arr, 1, 200, 777); // returns 200, arr[1] is 777
        int loaded0 = com.example.javaw.concurrent.Atomics.load(arr, 0); // 125
        int loaded1 = com.example.javaw.concurrent.Atomics.load(arr, 1); // 777
        if (prev1 == 100 && prev2 == 200 && loaded0 == 125 && loaded1 == 777) {
            return 1;
        }
        return 0;
    }

    public int testWasmThread() {
        int hc = com.example.javaw.concurrent.WasmThread.hardwareConcurrency();
        int tid = com.example.javaw.concurrent.WasmThread.currentThreadId();
        com.example.javaw.concurrent.WasmThread.sleep(1);
        if (hc >= 1 && tid == 0) {
            return 1;
        }
        return 0;
    }

    public int testSimpleTryCatch(int shouldThrow) {
        int result = 0;
        try {
            if (shouldThrow == 1) {
                throw new RuntimeException("Test error");
            }
            result = 100;
        } catch (RuntimeException e) {
            result = 999;
        }
        return result;
    }

    public String testExceptionMessage() {
        try {
            throw new Exception("Custom exception message");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public int testCustomExceptionHandling() {
        try {
            throw new CustomAppException("App failed", 404);
        } catch (CustomAppException ce) {
            return ce.getCode();
        }
    }

    public int testMultiCatchDispatch(int type) {
        try {
            if (type == 1) {
                throw new CustomAppException("Code error", 500);
            } else if (type == 2) {
                throw new IllegalArgumentException("Bad arg");
            } else {
                throw new Exception("Generic error");
            }
        } catch (CustomAppException ce) {
            return 1000 + ce.getCode();
        } catch (IllegalArgumentException iae) {
            return 2000;
        } catch (Exception e) {
            return 3000;
        }
    }

    public int testTryFinally(int shouldThrow) {
        int state = 0;
        try {
            if (shouldThrow == 1) {
                try {
                    throw new RuntimeException("Inner error");
                } catch (RuntimeException e) {
                    state += 10;
                }
            } else {
                state += 1;
            }
        } finally {
            state += 100;
        }
        return state;
    }

    public Pair makePair(int a, int b) {
        return new Pair(a, b);
    }

    public int sumPair(Pair p) {
        return p.first() + p.second();
    }

    public int computePairDifference(int a, int b) {
        Pair p = makePair(a, b);
        return p.first() - p.second();
    }

    public DivisionResult divideWithRemainder(int dividend, int divisor) {
        return new DivisionResult(dividend / divisor, dividend % divisor);
    }

    public int sumDivisionResult(int dividend, int divisor) {
        DivisionResult res = divideWithRemainder(dividend, divisor);
        return res.total();
    }

    public Point3D scalePoint3D(Point3D pt, double factor) {
        return new Point3D(pt.x() * factor, pt.y() * factor, pt.z() * factor);
    }

    public double dotProductPoint3D(Point3D a, Point3D b) {
        return (a.x() * b.x()) + (a.y() * b.y()) + (a.z() * b.z());
    }

    public int testArrayCopyInt() {
        int[] src = new int[]{10, 20, 30, 40, 50};
        int[] dest = new int[5];
        System.arraycopy(src, 1, dest, 2, 3);
        return dest[0] + dest[1] + dest[2] + dest[3] + dest[4];
    }

    public double testArrayCopyDouble() {
        double[] src = new double[]{1.5, 2.5, 3.5, 4.5};
        double[] dest = new double[4];
        System.arraycopy(src, 0, dest, 1, 3);
        return dest[0] + dest[1] + dest[2] + dest[3];
    }

    public int testArraysFillInt() {
        int[] arr = new int[5];
        Arrays.fill(arr, 7);
        int sum1 = arr[0] + arr[1] + arr[2] + arr[3] + arr[4];
        Arrays.fill(arr, 1, 4, 9);
        int sum2 = arr[0] + arr[1] + arr[2] + arr[3] + arr[4];
        Arrays.fill(arr, 0);
        int sum3 = arr[0] + arr[1] + arr[2] + arr[3] + arr[4];
        return sum1 + sum2 + sum3;
    }

    public int testArraysCopyOf() {
        int[] src = new int[]{10, 20, 30};
        int[] copy1 = Arrays.copyOf(src, 5);
        int sum1 = copy1[0] + copy1[1] + copy1[2] + copy1[3] + copy1[4];

        int[] copy2 = Arrays.copyOfRange(src, 1, 3);
        int sum2 = copy2[0] + copy2[1];

        boolean eq1 = Arrays.equals(src, copy1);
        int[] src2 = new int[]{10, 20, 30};
        boolean eq2 = Arrays.equals(src, src2);

        return sum1 + sum2 + (eq1 ? 100 : 0) + (eq2 ? 1 : 0);
    }

    public String testWasiFileWriteAndRead() {
        WASI.writeString("/hello.txt", "Hello WASI VFS");
        return WASI.readString("/hello.txt");
    }

    public boolean testWasiFileExists(String path) {
        return WASI.fileExists(path);
    }

    public int testWasiFileLength(String path) {
        return WASI.fileLength(path);
    }

    public String testWasiFileAppend() {
        WASI.writeString("/append.txt", "Part1");
        WASI.appendString("/append.txt", "Part2");
        return WASI.readString("/append.txt");
    }

    public boolean testWasiFileDelete() {
        WASI.writeString("/to_delete.txt", "temp data");
        boolean existsBefore = WASI.fileExists("/to_delete.txt");
        boolean deleted = WASI.deleteFile("/to_delete.txt");
        boolean existsAfter = WASI.fileExists("/to_delete.txt");
        return existsBefore && deleted && !existsAfter;
    }

    public boolean testWasiTimeAndRandom() {
        double time = WASI.currentTimeMillis();
        int rand = WASI.randomInt();
        return time > 0.0;
    }

    public String testWasiFileObject() {
        WASIFile file = new WASIFile("/app/data.txt");
        file.writeString("Config=42");
        file.appendString(";Active=true");
        return file.readString();
    }

    public boolean testWasiFileObjectOps() {
        WASIFile file = new WASIFile("/app/temp.log");
        file.writeString("12345");
        int len = file.length();
        boolean exists = file.exists();
        boolean deleted = file.delete();
        boolean existsAfter = file.exists();
        return len == 5 && exists && deleted && !existsAfter;
    }

    public double testSimdFloatAddAndSum() {
        Float32x4 a = Float32x4.of(1.0f, 2.0f, 3.0f, 4.0f);
        Float32x4 b = Float32x4.of(5.0f, 6.0f, 7.0f, 8.0f);
        Float32x4 c = a.add(b);
        return c.sum();
    }

    public double testSimdFloatMulAndDot() {
        Float32x4 a = Float32x4.of(2.0f, 3.0f, 4.0f, 5.0f);
        Float32x4 b = Float32x4.of(1.0f, 2.0f, 3.0f, 4.0f);
        return a.dot(b);
    }

    public double testSimdFloatMathOps() {
        Float32x4 a = Float32x4.splat(16.0f);
        Float32x4 b = a.sqrt();
        Float32x4 c = b.withLane(0, 100.0f);
        return c.getX() + c.getY();
    }

    public int testSimdIntAddAndBitwise() {
        Int32x4 a = Int32x4.of(10, 20, 30, 40);
        Int32x4 b = Int32x4.of(1, 2, 3, 4);
        Int32x4 c = a.add(b);
        Int32x4 d = c.and(Int32x4.splat(15));
        return d.sum();
    }

    public int testSimdArrayLoadStore() {
        int[] arr = new int[]{100, 200, 300, 400};
        Int32x4 vec = Int32x4.load(arr, 0);
        Int32x4 doubled = vec.add(vec);
        doubled.store(arr, 0);
        return arr[0] + arr[1] + arr[2] + arr[3];
    }

    public int testMultiMemoryHeapOps(int val) {
        Memory heap = Memory.HEAP;
        heap.storeI32(0, val);
        heap.storeI32(4, val * 2);
        return heap.loadI32(0) + heap.loadI32(4);
    }

    public double testMultiMemoryDoubleOps(double val) {
        Memory heap = Memory.HEAP;
        heap.storeF64(16, val);
        heap.storeF64(24, val * 3.0);
        return heap.loadF64(16) + heap.loadF64(24);
    }

    public int testMultiMemoryCopyStringsToHeap() {
        Memory heap = Memory.HEAP;
        Memory.copy(heap, 100, Memory.STRINGS, 0, 5);
        return heap.loadU8(100);
    }

    public int testMultiMemorySizeAndGrowth() {
        Memory heap = Memory.HEAP;
        int s1 = heap.size();
        heap.grow(1);
        int s2 = heap.size();
        return s2 - s1;
    }

    @JS("return Math.hypot(a, b);")
    public static native double jsHypot(double a, double b);

    @JS("return 'Hello ' + name + '!';")
    public static native String jsGreet(String name);

    @JS("return a * b + 10;")
    public static native int jsMultiplyAndAdd(int a, int b);

    @JS("return x > 100;")
    public static native boolean jsIsGreaterThan100(int x);

    @JS("return arr.reduce((acc, x) => acc + x, 0);")
    public static native int jsSumArray(@JS.Coerce int[] arr);

    @JS("return arr.map(x => x * 2);")
    @JS.Coerce
    public static native int[] jsDoubleArray(@JS.Coerce int[] arr);

    @JS("return items.join(', ');")
    public static native String jsJoinStrings(@JS.Coerce String[] items);

    @JS("return { title: 'WASM in Action', year: 2026 };")
    @JS.Coerce
    public static native String jsGetBookJson();

    public int testJsCoerceArraySum() {
        int[] numbers = new int[]{10, 20, 30, 40};
        return jsSumArray(numbers);
    }

    public int testJsCoerceArrayMap() {
        int[] numbers = new int[]{1, 2, 3, 4};
        int[] doubled = jsDoubleArray(numbers);
        int total = 0;
        for (int v : doubled) {
            total += v;
        }
        return total;
    }

    public int testJsValueHierarchy() {
        JSValue numVal = JSValue.of(42);
        JSValue strVal = JSValue.of("hello world");
        JSValue boolVal = JSValue.of(true);
        JSValue undefVal = JSValue.undefined();
        JSValue nullVal = JSValue.nullValue();

        int score = 0;
        if (numVal.isNumber()) score += 1;
        if (strVal.isString()) score += 2;
        if (boolVal.isBoolean()) score += 4;
        if (undefVal.isUndefined()) score += 8;
        if (nullVal.isNull()) score += 16;
        if ("number".equals(numVal.getTypeof())) score += 32;
        if ("string".equals(strVal.getTypeof())) score += 64;
        if ("boolean".equals(boolVal.getTypeof())) score += 128;

        return score;
    }

    public int testJsNumberTyped() {
        JSNumber num = JSNumber.of(100.5);
        JSNumber intNum = JSNumber.of(50);
        int score = 0;
        if (num.doubleValue() > 100.0) score += 1;
        if (intNum.intValue() == 50) score += 2;
        if (!num.isNaN() && num.isFinite()) score += 4;
        if (intNum.isInteger()) score += 8;
        return score;
    }

    public int testJsSymbolAndBigInt() {
        JSSymbol sym = JSSymbol.of("id_key");
        JSBigInt big = JSBigInt.of("9007199254740995");
        int score = 0;
        if (sym.isSymbol()) score += 1;
        if ("id_key".equals(sym.getDescription())) score += 2;
        if (big.isBigInt()) score += 4;
        return score;
    }

    public int testJsErrorAndInstanceof() {
        JSError err = JSError.typeError("Invalid type argument");
        int score = 0;
        if (err.isError()) score += 1;
        if ("Invalid type argument".equals(err.getMessage())) score += 2;
        if ("TypeError".equals(err.getName())) score += 4;
        if (err.instanceOf("Error") && err.instanceOf("TypeError")) score += 8;
        return score;
    }

    public int testJsStrictEquals() {
        JSValue v1 = JSValue.of(42);
        JSValue v2 = JSValue.of(42);
        JSValue v3 = JSValue.of("42");
        int score = 0;
        if (v1.strictEquals(v2)) score += 1;
        if (!v1.strictEquals(v3)) score += 2;
        return score;
    }

    public double testJsMathImport() {
        double hyp = com.example.javaw.js.JSMath.hypot(3.0, 4.0);
        double pi = com.example.javaw.js.JSMath.PI();
        return hyp + pi;
    }

    public double testJsDateImport() {
        com.example.javaw.js.JSDate d = com.example.javaw.js.JSDate.create(1700000000000.0);
        return d.getTime();
    }

    public String testJsJsonImport() {
        JSObject obj = JSObject.createObject();
        obj.set("name", "WebAssembly");
        obj.set("version", 2026);
        return com.example.javaw.js.JSJSON.stringify(obj);
    }

    public String testJsResourceGetString() {
        return com.example.javaw.js.JSResource.getAsString("config.json");
    }

    public boolean testJsResourceHas() {
        return com.example.javaw.js.JSResource.hasResource("theme.css") &&
               !com.example.javaw.js.JSResource.hasResource("nonexistent.txt");
    }

    public int testJsResourceLength() {
        return com.example.javaw.js.JSResource.getLength("theme.css");
    }

    public void testJsResourceInjectStyle() {
        com.example.javaw.js.JSResource.injectStyle(".app-header { font-size: 24px; }");
    }

    public void testJsResourceInjectStylesheet() {
        com.example.javaw.js.JSResource.injectStyleSheet("theme.css");
    }

    public void testJsResourceInjectScript() {
        com.example.javaw.js.JSResource.injectScript("globalThis.__injected_script_run = true;");
    }

    public int testBundledJsCode() {
        JSValue val = JSObject.global().get("__bundled_test_var");
        return val.asInt();
    }

    public String testTerminalReadLine(String prompt) {
        return WASITerminal.readLine(prompt);
    }

    public int testTerminalReadChar() {
        return WASITerminal.readChar();
    }

    public void testTerminalPrint(String msg) {
        WASITerminal.setColor(WASITerminal.GREEN);
        WASITerminal.print("[WASI] ");
        WASITerminal.resetColor();
        WASITerminal.println(msg);
    }

    public int testTerminalDimensions() {
        return WASITerminal.getColumns() * 100 + WASITerminal.getRows();
    }

    public void testTerminalClear() {
        WASITerminal.clear();
    }
}

record Pair(int first, int second) {}

record Point3D(double x, double y, double z) {}

record DivisionResult(int quotient, int remainder) {
    public int total() {
        return quotient() + remainder();
    }
}

class CustomAppException extends Exception {
    public int code;

    public CustomAppException(String msg, int code) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setLocation(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    public int distanceSq() {
        return x * x + y * y;
    }
}

class Shape {
    public int color;

    public Shape(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public int area() {
        return 0;
    }
}

class Circle extends Shape {
    public int radius;

    public Circle(int color, int radius) {
        super(color);
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }

    public int area() {
        return 3 * radius * radius;
    }
}

class Rectangle extends Shape {
    public int width;
    public int height;

    public Rectangle(int color, int width, int height) {
        super(color);
        this.width = width;
        this.height = height;
    }

    public int area() {
        return width * height;
    }
}

@com.example.javaw.js.JS.Export("JavaCounter")
class ExportedCounter {
    private int count;

    public ExportedCounter(int initialCount) {
        this.count = initialCount;
    }

    public void increment() {
        this.count++;
    }

    public void add(int amount) {
        this.count += amount;
    }

    public int getCount() {
        return this.count;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    public String describe(String label) {
        return label + ": " + this.count;
    }
}
