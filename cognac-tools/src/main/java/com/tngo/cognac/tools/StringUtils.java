package com.tngo.cognac.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class StringUtils {

    private static final String COMMA = ",";

    public static Set<String> fromCommaSeparatedString(String s) {
        if (isBlank(s)) return new HashSet<>();

        return new HashSet<>(Arrays.asList(s.split("\\s*,\\s*")));
    }

    public static <T> String toCommaSeparatedString(Collection<T> collection, Function<T, String> mapper) {
        if (collection == null || collection.isEmpty()) return null;

        return collection.stream()
                .map(mapper)
                .collect(Collectors.joining(COMMA));
    }
}
