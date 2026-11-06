package com.lavrel.assistant.domain;

/** A piece of a reference page given to the model, with where it came from and its license. */
public record Excerpt(String title, String url, String license, String text) {
}
