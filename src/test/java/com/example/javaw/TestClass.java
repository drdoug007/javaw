package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;
import com.example.javaw.dom.CanvasRenderingContext2D;
import com.example.javaw.dom.WebGLRenderingContext;
import com.example.javaw.dom.Window;
import com.example.javaw.dom.JSObject;

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
