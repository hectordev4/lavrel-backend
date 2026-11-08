package com.lavrel.shared.storage;

/** File storage is not configured or did not answer. */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
