package com.magenc.platform.agencies.domain;

public class SlugAlreadyTakenException extends RuntimeException {
    public SlugAlreadyTakenException(String slug) { super("Agency slug already taken: " + slug); }
}
