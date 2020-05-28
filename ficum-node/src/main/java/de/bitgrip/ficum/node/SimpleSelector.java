package de.bitgrip.ficum.node;

public class SimpleSelector implements Selector {
    private String value;

    public SimpleSelector(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
