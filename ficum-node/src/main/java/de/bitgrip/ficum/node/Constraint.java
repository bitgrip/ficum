package de.bitgrip.ficum.node;

public class Constraint<T> {

    private String selector;

    private Comparison comparison;

    private T argument;

    public Constraint(String selector, Comparison comparison, T argument) {
        super();
        checkArgNotNull(comparison, "comparison");
        checkArgNotNull(selector, "selector");
        this.selector = selector;
        this.comparison = comparison;
        this.argument = argument;
    }

    public Constraint(Selector selector, Comparison comparison, T argument) {
        this(checkArgNotNull(selector, "selector").getValue(), comparison, argument);
    }

    public static <T> T checkArgNotNull(T reference, String parameterName) {
        if (reference == null) {
            throw new IllegalArgumentException(String.format("'%s' must not be null", parameterName));
        }
        return reference;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Constraint))
            return false;
        Constraint<?> other = (Constraint<?>) obj;
        if (argument == null) {
            if (other.argument != null)
                return false;
        } else if (!argument.equals(other.argument))
            return false;
        if (comparison != other.comparison)
            return false;
        if (selector == null) {
            if (other.selector != null)
                return false;
        } else if (!selector.equals(other.selector))
            return false;
        return true;
    }

    public T getArgument() {
        return argument;
    }

    public Comparison getComparison() {
        return comparison;
    }

    public String getSelector() {
        return selector;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argument == null) ? 0 : argument.hashCode());
        result = prime * result + ((comparison == null) ? 0 : comparison.hashCode());
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format("'%s%s(%s)%s'", selector, comparison.getSign(),
                argument == null ? "null" : argument.getClass().getSimpleName(), argument);
    }

}
