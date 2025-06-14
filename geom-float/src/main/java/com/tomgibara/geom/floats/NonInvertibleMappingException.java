package com.tomgibara.geom.floats;

public class NonInvertibleMappingException extends RuntimeException {

    private static final long serialVersionUID = 6149583944567333855L;

    public NonInvertibleMappingException() { }

    public NonInvertibleMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonInvertibleMappingException(String message) {
        super(message);
    }

    public NonInvertibleMappingException(Throwable cause) {
        super(cause);
    }

}
