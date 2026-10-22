package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IfMatch {

    private static final Pattern STRONG_VERSION = Pattern.compile("^\"?(\\d{1,18})\"?$");

    private IfMatch() {
    }

    public static long toVersion(String headerValue) {
        Matcher matcher = STRONG_VERSION.matcher(headerValue.trim());
        if (!matcher.matches()) {
            throw new InvalidIfMatchException(headerValue);
        }
        return Long.parseLong(matcher.group(1));
    }

    public static class InvalidIfMatchException extends RuntimeException {

        InvalidIfMatchException(String headerValue) {
            super("Unsupported If-Match value: " + headerValue);
        }
    }
}
