package net.c0f3.propmapper.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kostapc on 14.11.16.
 *
 */
class PropertyFile<T> {
    private static Logger LOG = Logger.getLogger(PropertyFile.class);

    final Path path;
    final T object;

    final PropertyMapper<T> mapper;
    final Lock lock = new ReentrantLock();


    PropertyFile(Path file, PropertyMapper<T> mapper) {
        this.path = file;
        this.mapper = mapper;
        try {
            this.object = mapper.getPropertyClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("cannot create new property mapped object",e);
            throw new IllegalStateException(e);
        }

    }

    boolean update() throws IOException {
        lock.lock();

        Properties properties = mapper.loadProperties(path);
        if(properties==null) {
            return false;
        }

        properties.load(Files.newInputStream(path));

        properties.forEach(
                (k,v)->setValue(object, mapper.getPropertyClassFields().get(k.toString()), v.toString())
        );
        lock.unlock();
        return true;
    }

    /*
    boolean isDifferent(T source, T check) {
        boolean isDifferent = false;
        for (Field field : fields.values()) {
            try {
                field.setAccessible(true);
                if(!field.get(source).equals(field.get(check))) {
                    isDifferent = true;
                }
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                LOG.error(e);
                return true;
            }
        }
        return isDifferent;
    }
    */

    Path getPath() {
        return path;
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

    public T getObject() {
        return object;
    }
}
