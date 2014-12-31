package com.opentable.jaxrs.mock;

public class NoExpectationAssertionError extends AssertionError {
    private static final long serialVersionUID = 1;

    public NoExpectationAssertionError(String message) {
        super(message);
    }
}
