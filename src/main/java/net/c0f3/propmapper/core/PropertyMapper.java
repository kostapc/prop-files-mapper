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

    private final Class<T> clazz;
    private final String folder;
    private final Map<String, Field> fields = new HashMap<>();
    private final String keyProperty;

    private final ChangesWatcher watcher;

    // ------ local storage (singletone)
    private final Map<String, T> properties = new HashMap<>();

    PropertyMapper(Class<T> clazz, ChangesWatcher watcher) throws IOException {
        this.watcher = watcher;
        // TODO: scan class path for annotated beans
        this.clazz = clazz;
        PropertyMappedEntry annotation = clazz.getAnnotation(PropertyMappedEntry.class);
        if(annotation==null) {
            throw new IllegalArgumentException("provided class must have PropertyMappedEntry annotation");
        }
        folder = annotation.folder();
        String key = null;
        for (Field field : clazz.getDeclaredFields()) {
            PropertyMap propertyMap = field.getAnnotation(PropertyMap.class);
            if(propertyMap == null) {
                continue;
            }
            if(propertyMap.id()) {
                key = propertyMap.value();
            }
            fields.put(propertyMap.value(), field);
        }
        if(key==null) {
            throw new IllegalArgumentException("provided class has no key field");
        }
        keyProperty = key;
    }

    Class<T> getPropertyClass() {
        return clazz;
    }

    Map<String, Field> getPropertyClassFields() {
        return fields;
    }

    String getKeyProperty() {
        return keyProperty;
    }

    public Map<String, T> getObjects() {
        return properties;
    }

    public Map<String, T> scan() {
        final Map<String, T> props = properties;
        final PropertyMapper<T> mapper = this;
        try {
            Files.walkFileTree(Paths.get(folder), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // check file has key field and this key not already loaded
                        final Properties properties = mapper.loadProperties(file);
                        if(properties==null) {
                            return FileVisitResult.CONTINUE;
                        }

                        String id;
                        if (!properties.containsKey(keyProperty)) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            id = properties.getProperty(keyProperty);
                        }

                        if (props.containsKey(id)) {
                            return FileVisitResult.CONTINUE;
                        }

                        // here create new Object and add it to monitoring
                        // then forget about it. Later calls adds only new Objects
                        // TODO: add folder monitoring for add/delete objects by new/deleted files
                        // driven by OS event, not periodic scan folder
                        PropertyFile<T> propertyFile = new PropertyFile<>(file, mapper);
                        propertyFile.update();

                        props.put(id, propertyFile.getObject());
                        watcher.addWathingFile(propertyFile);

                        return FileVisitResult.CONTINUE;
                    } catch (Exception e) {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            return EMPTY_MAP;
        }

        return props;
    }

    Properties loadProperties(Path file) {
        final Properties properties = new Properties();

        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (Exception ex) {
            LOG.error(ex);
            return null;
        }

        for (String key : fields.keySet()) {
            if (!properties.containsKey(key)) {
                return null;
            }
        }

        return properties;
    }


}
