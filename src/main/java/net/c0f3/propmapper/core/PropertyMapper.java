package net.c0f3.propmapper.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class PropertyMapper<T> {

    private static final Log LOG = LogFactory.getLog(PropertyMapper.class);

    private Map<String, T> EMPTY_MAP = new HashMap<>();

    private Class<T> clazz;
    private String folder;
    private Map<String, Field> fields = new HashMap<>();
    private String keyProperty;

    private ChangesWatcher wather;

    // ------ local storage (singletone)
    final Map<String, T> properties = new HashMap<>();
    // final List<PropertyFile> files = new LinkedList<>();

    public PropertyMapper(String basefolder, Class<T> clazz) throws IOException {
        // TODO: scan class path for annotated beans
        this.clazz = clazz;
        PropertyMappedEntry annotation = clazz.getAnnotation(PropertyMappedEntry.class);
        if(annotation==null) {
            throw new IllegalArgumentException("provided class must have PropertyMappedEntry annotation");
        }
        folder = basefolder+File.separator+annotation.folder();
        for (Field field : clazz.getDeclaredFields()) {
            PropertyMap propertyMap = field.getAnnotation(PropertyMap.class);
            if(propertyMap == null) {
                continue;
            }
            if(propertyMap.id()) {
                keyProperty = propertyMap.value();
            }
            fields.put(propertyMap.value(), field);
        }
        wather = new ChangesWatcher();
    }

    public Map<String, T> scan() {
        final Map<String, T> props = properties;

        try {
            Files.walkFileTree(Paths.get(folder), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    final Properties properties = new Properties();

                    try (InputStream in = Files.newInputStream(file)) {
                        properties.load(in);
                    } catch (Exception ex) {
                        LOG.error(ex);
                        return FileVisitResult.CONTINUE;
                    }

                    String id;
                    if(!properties.containsKey(keyProperty)) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        id = properties.getProperty(keyProperty);
                    }

                    for (String key : fields.keySet()) {
                        if(!properties.containsKey(key)) {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    if(props.containsKey(id)) {
                        return FileVisitResult.CONTINUE;
                    }

                    T object;
                    try {
                        object = clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOG.error(e);
                        return FileVisitResult.CONTINUE;
                    }

                    properties.forEach(
                            (k,v)->setValue(object, fields.get(k.toString()), v.toString())
                    );

                    props.put(id,object);
                    wather.addWathingFile(
                            new PropertyFile(file, object, fields)
                    );

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return EMPTY_MAP;
        }

        return props;
    }

    private void setValue(T object, Field field, String propertyValue) {
        Object value;
        if(field.getType().equals(String.class)) {
            value = propertyValue;
        } else
        if(field.getType().equals(Integer.class)) {
            value = Integer.parseInt(propertyValue);
        } else
        {
            value = propertyValue;
        }
        try {
            field.setAccessible(true);
            field.set(object, value);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            LOG.error(e);
        }
    }

}
