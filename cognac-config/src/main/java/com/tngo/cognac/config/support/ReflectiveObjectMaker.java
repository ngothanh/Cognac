package com.tngo.cognac.config.support;

import com.tngo.cognac.config.ConfigField;
import com.tngo.cognac.config.ConfigPrefix;
import com.tngo.cognac.config.ConfigScope;
import com.tngo.cognac.config.exceptions.PrebootException;
import io.gridgo.bean.BObject;
import lombok.Builder;
import lombok.NonNull;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Builder
public class ReflectiveObjectMaker {

    @NonNull
    private Class<?> type;

    @NonNull
    @Builder.Default
    private BObject data = BObject.ofEmpty();

    private ReflectiveObjectMaker parent;

    private ReflectiveObjectMaker getRoot() {
        var visited = new HashSet<ReflectiveObjectMaker>();

        var root = this;
        while (true) {
            if (root.parent == null)
                return root;

            if (visited.contains(root))
                throw new PrebootException("Circular reference detected");

            visited.add(root);
            root = root.parent;
        }
    }

    private Object _make() throws Exception {
        var result = type.getConstructor().newInstance();
        var fields = type.getDeclaredFields();

        for (var field : fields) {
            Object value = null;

            if (field.isAnnotationPresent(ConfigField.class)) {
                var annotation = field.getAnnotation(ConfigField.class);
                var fieldName = annotation.value();
                var scope = annotation.scope();
                var _data = scope == ConfigScope.RELATIVE ? data : getRoot().data;

                if (_data.containsKey(fieldName))
                    value = _data.getValue(fieldName).getDataAs(field.getType());
            } else if (field.isAnnotationPresent(ConfigPrefix.class)) {
                var annotation = field.getAnnotation(ConfigPrefix.class);
                var prefix = annotation.value();
                var scope = annotation.scope();
                var _data = scope == ConfigScope.RELATIVE ? data : getRoot().data;

                var subResolved = _data.entrySet().stream() //
                        .filter(entry -> entry.getKey().startsWith(prefix)) //
                        .map(entry -> new AbstractMap.SimpleEntry<>(
                                entry.getKey().substring(prefix.length()), //
                                entry.getValue())) //
                        .collect(Collectors.toMap( //
                                Map.Entry::getKey, //
                                Map.Entry::getValue, //
                                (x, y) -> y, //
                                BObject::ofEmpty));

                if (field.getType().isAssignableFrom(BObject.class)) {
                    value = subResolved;
                } else if (field.getType().isAssignableFrom(Map.class)) {
                    value = subResolved.toMap();
                } else if (field.getType() == Properties.class) {
                    var props = new Properties();
                    //
                    subResolved.forEach((key, value1) -> props.put( //
                            key, //
                            value1.asValue().getData()));
                    value = props;
                } else
                    value = ReflectiveObjectMaker.builder() //
                            .type(field.getType()) //
                            .data(subResolved) //
                            .parent(this) //
                            .build() //
                            .make();
            }

            if (value != null && field.trySetAccessible())
                field.set(result, value);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T> T make() {
        try {
            return (T) _make();
        } catch (Exception e) {
            throw new PrebootException("Cannot make target object", e);
        }
    }
}
