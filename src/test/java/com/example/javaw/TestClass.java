package com.example.javaw;

public class TestClass {
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
