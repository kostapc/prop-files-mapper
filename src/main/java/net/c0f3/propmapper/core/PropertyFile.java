package net.c0f3.propmapper.core;

import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by kostapc on 14.11.16.
 *
 */
public class PropertyFile<T> {
    protected static Logger LOG = Logger.getLogger(PropertyFile.class);

    final Path path;
    final T object;
    // TODO: remove this from dependency hierarchy
    final private Map<String, Field> fields;

    public PropertyFile(Path file, T object, Map<String, Field> fields) {
        this.path = file;
        this.object = object;
        this.fields = fields;
    }

    public void update() {
        synchronized (object) {
            //TODO: update ObjectField
        }
    }

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

    public Path getPath() {
        return path;
    }
}
