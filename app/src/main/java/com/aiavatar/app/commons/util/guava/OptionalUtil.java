package com.aiavatar.app.commons.util.guava;

import java.util.Arrays;

public final class OptionalUtil {

    private OptionalUtil() {
    }

    public static boolean byteArrayEquals(Optional<byte[]> a, Optional<byte[]> b) {
        if (a.isPresent() != b.isPresent()) {
            return false;
        }

        if (a.isPresent()) {
            return Arrays.equals(a.get(), b.get());
        }

        return true;
    }

    public static int byteArrayHashCode(Optional<byte[]> bytes) {
        return bytes.isPresent() ? Arrays.hashCode(bytes.get()) : 0;
    }

    public static Optional<String> absentIfEmpty(String value) {
        if (value == null || value.length() == 0) {
            return Optional.absent();
        } else {
            return Optional.of(value);
        }
    }

    public static Optional<String> defaultIfEmpty(String value, String defValue) {
        if (value == null || value.length() == 0) {
            return Optional.of(defValue);
        } else {
            return Optional.of(value);
        }
    }
}