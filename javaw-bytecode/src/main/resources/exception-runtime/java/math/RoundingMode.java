package java.math;
public enum RoundingMode {
    UP, DOWN, CEILING, FLOOR, HALF_UP, HALF_DOWN, HALF_EVEN, UNNECESSARY;
    public static RoundingMode valueOf(int mode){if(mode<0 || mode>7)throw new IllegalArgumentException();return values()[mode];}
}
