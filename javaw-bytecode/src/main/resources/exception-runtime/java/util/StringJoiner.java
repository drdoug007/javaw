package java.util;

import java.util.Objects;

/**
 * Implementation of StringJoiner for the javaw bytecode compiler.
 * This is a minimal implementation matching JDK 21 behavior exactly.
 */
public final class StringJoiner {
    private String prefix;
    private String delimiter;
    private String suffix;
    private String emptyValue;
    private StringBuilder elements;
    private int elementCount;

    /**
     * Creates a StringJoiner with the specified delimiter.
     * Equivalent to new StringJoiner(delimiter, "", "").
     */
    public StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    /**
     * Creates a StringJoiner with the specified delimiter, prefix, and suffix.
     *
     * @param delimiter the delimiter to use between elements
     * @param prefix the prefix to use at the beginning
     * @param suffix the suffix to use at the end
     * @throws NullPointerException if any of prefix, delimiter, or suffix is null
     */
    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        Objects.requireNonNull(prefix, "The prefix must not be null");
        Objects.requireNonNull(delimiter, "The delimiter must not be null");
        Objects.requireNonNull(suffix, "The suffix must not be null");

        this.delimiter = delimiter.toString();
        this.prefix = prefix.toString();
        this.suffix = suffix.toString();
        this.elements = new StringBuilder();
        this.elementCount = 0;
    }

    /**
     * Sets the empty value to use when no elements have been added.
     *
     * @param emptyValue the empty value to use
     * @return this StringJoiner instance
     * @throws NullPointerException if emptyValue is null
     */
    public StringJoiner setEmptyValue(CharSequence emptyValue) {
        Objects.requireNonNull(emptyValue, "The empty value must not be null");
        this.emptyValue = emptyValue.toString();
        return this;
    }

    /**
     * Adds a new element to the joiner.
     *
     * @param newElement the element to add
     * @return this StringJoiner instance
     */
    public StringJoiner add(CharSequence newElement) {
        if (elementCount > 0) {
            elements.append(delimiter);
        }
        elements.append(newElement == null ? "null" : newElement.toString());
        elementCount++;
        return this;
    }

    /**
     * Merges another StringJoiner into this one.
     *
     * @param other the StringJoiner to merge
     * @return this StringJoiner instance
     * @throws NullPointerException if other is null
     */
    public StringJoiner merge(StringJoiner other) {
        Objects.requireNonNull(other);

        if (other.elementCount > 0) {
            // Get the joined elements from other before appending to this
            String otherJoined = other.elements.toString();

            if (elementCount > 0) {
                elements.append(delimiter);
            }
            elements.append(otherJoined);
            elementCount += other.elementCount;
        }

        return this;
    }

    /**
     * Returns the length of the joined string.
     *
     * @return the length of the joined string
     */
    public int length() {
        return toString().length();
    }

    /**
     * Returns the joined string.
     *
     * @return the joined string
     */
    public String toString() {
        if (elementCount == 0) {
            if (emptyValue != null) {
                return emptyValue;
            }
            return prefix + suffix;
        }
        return prefix + elements.toString() + suffix;
    }
}