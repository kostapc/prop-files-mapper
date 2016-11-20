package net.c0f3.propmapper.core;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kostapc on 21.11.16
 *
 */
public enum MapperRegistry {

    INSTANCE;

    private final Map<Class<?>, PropertyMapper<?>> mappers = new ConcurrentHashMap<>();
    private final ChangesWatcher watcher = new ChangesWatcher();


    @SuppressWarnings("unchecked")
    public synchronized final <T> PropertyMapper<T> getPropertyMapper(Class<T> clazz) throws IOException {
        PropertyMapper<T> mapper = (PropertyMapper<T>) mappers.get(clazz);
        if(mapper==null) {
            mapper = new PropertyMapper<>(clazz, watcher);
            mappers.put(clazz, mapper);
        }
        return mapper;
    }

}
