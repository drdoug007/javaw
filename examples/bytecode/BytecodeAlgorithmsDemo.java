package com.example.javaw.examples.bytecode;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;

/**
 * Demonstrates complex algorithmic routines structured for bytecode-to-WebAssembly
 * compilation and Relooper control flow structuring:
 * 1. Fast Euclidean GCD and modulo arithmetic.
 * 2. Iterative Fibonacci sequence calculation.
 * 3. Collatz Conjecture step counter and sequence analysis.
 * 4. Prime Sieve of Eratosthenes with boolean array allocation.
 * 5. State Machine Lexer with multi-state transitions and switch dispatch.
 */
public class BytecodeAlgorithmsDemo {

    public static int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static long fibonacci(int n) {
        if (n <= 0) return 0;
        if (n == 1) return 1;
        long a = 0;
        long b = 1;
        for (int i = 2; i <= n; i++) {
            long next = a + b;
            a = b;
            b = next;
        }
        return b;
    }

    public static int collatzSteps(long n) {
        int steps = 0;
        while (n > 1) {
            if ((n & 1) == 0) {
                n = n / 2;
            } else {
                n = 3 * n + 1;
            }
            steps++;
        }
        return steps;
    }

    public static int countPrimes(int limit) {
        if (limit < 2) return 0;
        boolean[] isPrime = new boolean[limit + 1];
        for (int i = 2; i <= limit; i++) {
            isPrime[i] = true;
        }
        for (int p = 2; p * p <= limit; p++) {
            if (isPrime[p]) {
                for (int i = p * p; i <= limit; i += p) {
                    isPrime[i] = false;
                }
            }
        }
        int count = 0;
        for (int i = 2; i <= limit; i++) {
            if (isPrime[i]) count++;
        }
        return count;
    }

    // State machine token counter: 0=START, 1=IN_WORD, 2=IN_NUMBER, 3=IN_PUNCT
    public static int countTokens(String input) {
        if (input == null || input.isEmpty()) return 0;
        int state = 0;
        int tokens = 0;
        int len = input.length();

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            boolean isSpace = (c == ' ' || c == '\t' || c == '\n' || c == '\r');
            boolean isDigit = (c >= '0' && c <= '9');
            boolean isAlpha = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');

            switch (state) {
                case 0: // START
                    if (isAlpha) {
                        state = 1; // IN_WORD
                        tokens++;
                    } else if (isDigit) {
                        state = 2; // IN_NUMBER
                        tokens++;
                    } else if (!isSpace) {
                        state = 3; // IN_PUNCT
                        tokens++;
                    }
                    break;
                case 1: // IN_WORD
                    if (isSpace) {
                        state = 0;
                    } else if (isDigit) {
                        state = 2;
                    } else if (!isAlpha) {
                        state = 3;
                        tokens++;
                    }
                    break;
                case 2: // IN_NUMBER
                    if (isSpace) {
                        state = 0;
                    } else if (isAlpha) {
                        state = 1;
                        tokens++;
                    } else if (!isDigit) {
                        state = 3;
                        tokens++;
                    }
                    break;
                case 3: // IN_PUNCT
                    if (isSpace) {
                        state = 0;
                    } else if (isAlpha) {
                        state = 1;
                        tokens++;
                    } else if (isDigit) {
                        state = 2;
                        tokens++;
                    }
                    break;
                default:
                    state = 0;
                    break;
            }
        }
        return tokens;
    }

    public static void start() {
        Element root = DOM.getElementById("bytecode-app");
        if (root == null) return;

        Element card = DOM.createElement("div");
        card.setStyle("background", "#1e293b");
        card.setStyle("padding", "20px");
        card.setStyle("borderRadius", "8px");
        card.setStyle("color", "#f8fafc");

        Element title = DOM.createElement("h3");
        title.setInnerText("Bytecode & Relooper Algorithmic Engine");
        title.setStyle("color", "#38bdf8");
        title.setStyle("marginBottom", "12px");
        card.appendChild(title);

        int gcdVal = gcd(1071, 462);
        long fibVal = fibonacci(25);
        int collatzVal = collatzSteps(27);
        int primeCount = countPrimes(100);
        int tokenCount = countTokens("javaw: compile Java 17 bytecode to WebAssembly!");

        Element results = DOM.createElement("div");
        results.setStyle("lineHeight", "1.8");
        results.setStyle("fontFamily", "monospace");
        results.setStyle("fontSize", "14px");

        results.appendChild(createRow("GCD(1071, 462)", "" + gcdVal));
        results.appendChild(createRow("Fibonacci(25)", "" + fibVal));
        results.appendChild(createRow("Collatz Steps for 27", "" + collatzVal + " steps"));
        results.appendChild(createRow("Primes up to 100", "" + primeCount + " primes"));
        results.appendChild(createRow("Lexer Token Count", "" + tokenCount + " tokens"));

        card.appendChild(results);
        root.appendChild(card);
    }

    private static Element createRow(String label, String value) {
        Element row = DOM.createElement("div");
        row.setStyle("display", "flex");
        row.setStyle("justifyContent", "space-between");
        row.setStyle("borderBottom", "1px solid #334155");
        row.setStyle("padding", "6px 0");

        Element lbl = DOM.createElement("span");
        lbl.setStyle("color", "#94a3b8");
        lbl.setInnerText(label + ":");

        Element val = DOM.createElement("span");
        val.setStyle("color", "#34d399");
        val.setStyle("fontWeight", "bold");
        val.setInnerText(value);

        row.appendChild(lbl);
        row.appendChild(val);
        return row;
    }

    public static void main(String[] args) {
        start();
    }
}
