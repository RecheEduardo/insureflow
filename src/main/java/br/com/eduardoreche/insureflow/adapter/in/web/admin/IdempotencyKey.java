package br.com.eduardoreche.insureflow.adapter.in.web.admin;

import br.com.eduardoreche.insureflow.application.policy.dto.IdempotencyKeyDTO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class IdempotencyKey {

    private static final Pattern PRINTABLE_ASCII = Pattern.compile("^[\\x21-\\x7E]{1,255}$");

    private IdempotencyKey() {
    }

    public static IdempotencyKeyDTO of(String headerValue, String canonicalBody) {
        if (!PRINTABLE_ASCII.matcher(headerValue).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        return new IdempotencyKeyDTO(headerValue, sha256(canonicalBody));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class InvalidIdempotencyKeyException extends RuntimeException {

        InvalidIdempotencyKeyException() {
            super("Idempotency-Key must have 1 to 255 printable ASCII characters");
        }
    }
}
