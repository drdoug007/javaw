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
}
