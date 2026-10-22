package com.example.javaw.examples.records;

public class RecordMathEngine {

    // Java 17 Record mapped to multi-value WebAssembly stack returns
    public record ComplexNumber(double real, double imag) {
        public ComplexNumber add(ComplexNumber other) {
            return new ComplexNumber(this.real() + other.real(), this.imag() + other.imag());
        }

        public ComplexNumber multiply(ComplexNumber other) {
            double r = this.real() * other.real() - this.imag() * other.imag();
            double i = this.real() * other.imag() + this.imag() * other.real();
            return new ComplexNumber(r, i);
        }

        public double magnitudeSquared() {
            return this.real() * this.real() + this.imag() * this.imag();
        }
    }

    public record ComputationResult(int status, double value, String message) {}

    // Custom domain exceptions
    public static class MathDomainException extends RuntimeException {
        public MathDomainException(String message) {
            super(message);
        }
    }

    public static class OverflowException extends MathDomainException {
        public OverflowException(String message) {
            super(message);
        }
    }

    public static ComplexNumber makeComplex(double r, double i) {
        return new ComplexNumber(r, i);
    }

    public static ComplexNumber addComplex(ComplexNumber a, ComplexNumber b) {
        return a.add(b);
    }

    public static ComputationResult safeDivide(double numerator, double denominator) {
        try {
            if (denominator == 0.0) {
                throw new MathDomainException("Division by zero is undefined.");
            }
            double result = numerator / denominator;
            if (result > 1000000.0) {
                throw new OverflowException("Computation exceeded allowable maximum threshold.");
            }
            return new ComputationResult(200, result, "Success");
        } catch (OverflowException oe) {
            return new ComputationResult(422, 0.0, "Overflow: " + oe.getMessage());
        } catch (MathDomainException mde) {
            return new ComputationResult(400, 0.0, "Math error: " + mde.getMessage());
        } catch (Exception e) {
            return new ComputationResult(500, 0.0, "Unknown system error occurred.");
        } finally {
            // Cleanup hook executed reliably
        }
    }
}
