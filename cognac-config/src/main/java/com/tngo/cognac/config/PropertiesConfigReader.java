package com.tngo.cognac.config;

import com.tngo.cognac.config.support.ReflectiveObjectMaker;
import io.gridgo.bean.BElement;
import io.gridgo.bean.BObject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.tngo.cognac.config.support.EnvironmentVariables.resolve;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertiesConfigReader<ConfigType> {

    public static <T> PropertiesConfigReader<T> forType(Class<T> type) {
        return new PropertiesConfigReader<>(type);
    }

    private final Class<ConfigType> type;

    public ConfigType readProperties(String propertiesFilePath) throws Exception {
        return readProperties(new File(propertiesFilePath));
    }

    public ConfigType readProperties(File propertiesFile) throws Exception {
        try (var inputStream = new FileInputStream(propertiesFile)) {
            return readProperties(inputStream);
        }
    }

    public ConfigType readProperties(InputStream inputStream) throws Exception {
        var props = new Properties();
        props.load(inputStream);
        return readProperties(props);
    }

    public ConfigType readProperties(Properties properties) throws Exception {
        return read(resolve(properties).entrySet().stream() //
                .map(entry -> new SimpleEntry<String, BElement>( //
                        entry.getKey().toString(), //
                        BElement.wrapAny(entry.getValue())))
                .collect(Collectors.toMap( //
                        Entry::getKey, //
                        Entry::getValue, //
                        (k1, k2) -> k1, //
                        BObject::ofEmpty)));
    }

    private ConfigType read(BObject resolved) {
        return ReflectiveObjectMaker.builder() //
                .data(resolved) //
                .type(type) //
                .build() //
                .make();
    }
}
