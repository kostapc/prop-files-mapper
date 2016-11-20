package net.c0f3.propmapper.core;

import java.util.concurrent.Executors;

/**
 * Created by kostapc on 21.11.16.
 *
 */
public class PropertyMapException extends Exception {
    public PropertyMapException(Exception ex) {
        super(ex);
    }
}
