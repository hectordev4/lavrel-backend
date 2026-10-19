package com.lavrel.shared.error;

/** Body of every error response: a stable machine-readable code plus a human-readable message. */
public record ApiError(String error, String message) {
}
