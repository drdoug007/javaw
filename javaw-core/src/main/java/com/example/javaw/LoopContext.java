package com.example.javaw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoopContext {
    final List<String> javaLabels;
    final String breakLabel;
    final String continueLabel;

    LoopContext(String breakLabel, String continueLabel) {
        this(Collections.emptyList(), breakLabel, continueLabel);
    }

    LoopContext(List<String> javaLabels, String breakLabel, String continueLabel) {
        this.javaLabels = javaLabels != null ? new ArrayList<>(javaLabels) : Collections.emptyList();
        this.breakLabel = breakLabel;
        this.continueLabel = continueLabel;
    }

    public boolean hasLabel(String label) {
        return javaLabels.contains(label);
    }
}
