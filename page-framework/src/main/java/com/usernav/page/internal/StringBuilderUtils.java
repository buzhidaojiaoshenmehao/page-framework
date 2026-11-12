package com.usernav.page.internal;

public final class StringBuilderUtils {
    private StringBuilderUtils() {}

    public static StringBuilder concat(String prefix, String firstPart, String secondPart,
            String thirdPart, String suffix) {
        StringBuilder builder = new StringBuilder(prefix);
        builder.append(firstPart);
        builder.append(secondPart);
        builder.append(thirdPart);
        builder.append(suffix);
        return builder;
    }
}
