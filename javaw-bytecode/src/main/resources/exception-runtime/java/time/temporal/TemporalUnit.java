package java.time.temporal;

import java.time.Duration;

public interface TemporalUnit {
    Duration getDuration();
    boolean isDurationEstimated();
    boolean isDateBased();
    boolean isTimeBased();
    String toString();
}
