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
}
