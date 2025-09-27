package de.c7h12.webuntis.client

class WebUntisException extends RuntimeException {
    WebUntisException(String message) {
        super(message)
    }

    WebUntisException(String message, Throwable cause) {
        super(message, cause)
    }
}