package com.tngo.cognac.config.support;

import com.tngo.cognac.config.exceptions.CognacConfigException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.lang.System.*;

@Slf4j
public class EnvironmentVariables {

    private static final Pattern ENV_VARIABLE_PATTERN = Pattern.compile("(?i)\\$\\{([a-z0-9_]+)\\}");

    public static Properties resolve(Properties properties) {
        var propertiesResult = new Properties();
        properties.forEach((key, value) -> propertiesResult.put(key, resolve((String) value)));
        return propertiesResult;
    }

    public static String resolve(String input) {

        var matcher = ENV_VARIABLE_PATTERN.matcher(input);

        var sb = new StringBuilder();

        var lastIndex = 0;
        while (matcher.find()) {
            var start = matcher.start();
            var end = matcher.end();

            var envName = matcher.group(1);
            var envValue = resolveEnvironmentVariable(envName);
            if (envValue == null) {
                log.warn("Environment variable `{}` cannot be resolved", envName);
                envValue = input.substring(start, end);
            }

            if (start > lastIndex)
                sb.append(input, lastIndex, start);

            sb.append(envValue);

            lastIndex = end;
        }

        if (lastIndex < input.length())
            sb.append(input.substring(lastIndex));

        return sb.toString().trim();
    }

    private static String resolveEnvironmentVariable(@NonNull String envName) {
        return Optional.ofNullable(getenv(envName)).orElseGet(() -> getProperty(envName, null));
    }

    public static void setSystemProperties(File file) {
        try (var input = new FileInputStream(file)) {
            setSystemProperties(input);
        } catch (Exception e) {
            throw new CognacConfigException("Error while loading file", e);
        }
    }

    public static void setSystemProperties(InputStream input) {
        var props = new Properties();
        try {
            props.load(input);
        } catch (IOException e) {
            throw new CognacConfigException("Error while loading input stream", e);
        }
        setSystemProperties(props);
    }

    public static void setSystemProperties(Properties props) {
        props.entrySet().stream() //
                .filter(entry -> entry.getValue() != null) //
                .forEach(entry -> setProperty(entry.getKey().toString(), entry.getValue().toString()));
    }
}
