package net.c0f3.propmapper.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by kostapc on 14.11.16.
 * one watcher per PropertyMapper
 */
class ChangesWatcher {
    protected static Logger log = Logger.getLogger(ChangesWatcher.class);

    //private final PropertiesWatcher.EventPublisher eventPublisher;

    //  one location per mapped Object
    private final List<PropertyFile> locations = new CopyOnWriteArrayList<>();
    private WatchService watchService;
    final ExecutorService service;

    public ChangesWatcher() throws IOException {
        //this.eventPublisher = eventPublisher;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.service = Executors.newCachedThreadPool();
    }

    void addWathingFile(PropertyFile file) {
        locations.add(file);
        service.submit(new ChangesWatcher.ResourceWatcher(file));
    }


    public void stop() {
        try {
            this.watchService.close();
            log.debug("Shuting down watcher");
            this.service.shutdownNow();
        } catch (final IOException e) {
            log.error("Fialed stoping watcher", e);
        }
    }


    private void publishChangedEvent(final PropertyFile file) {
        //this.eventPublisher.onChanged(resource);
        // TODO: update object fields if any changes
        file.update();
    }

    private WatchService getWatchService() {
        return this.watchService;
    }


    private class ResourceWatcher implements Runnable {

        private final PropertyFile propertyFile;
        //private final List<Path> resources;

        ResourceWatcher(final PropertyFile propertyFile) {
            this.propertyFile = propertyFile;
            //this.resources = resources;
        }

        @Override
        public void run() {
            try {
                log.debug("START");
                log.debug(String.format("Watching %s", this.propertyFile.toString()));
                Path path = propertyFile.getPath();
                while (!Thread.currentThread().isInterrupted()) {

                    final WatchKey pathBeingWatched = path.register(getWatchService(), ENTRY_MODIFY);

                    WatchKey watchKey = null;
                    try {
                        watchKey = getWatchService().take();
                    } catch (final ClosedWatchServiceException | InterruptedException e) {
                        log.debug("END");
                        Thread.currentThread().interrupt();
                    }

                    if (watchKey == null) {
                        continue;
                    }

                    for (final WatchEvent<?> event : pathBeingWatched.pollEvents()) {
                        log.debug("File modification Event Triggered");
                        final Path target = path(event.context());
                        if (isValidTargetFile(target)) {
                            log.debug(String.format("Watched Resource changed, modified file [%s]",
                                    target.getFileName().toString()
                            ));
                            log.debug(String.format("  event [%s] target [%s] path [%s]",
                                    event.kind(), target, path(watchKey.watchable())
                            ));
                            publishChangedEvent(propertyFile);
                        }
                    }
                    if (!watchKey.reset()) {
                        log.debug("END");
                        Thread.currentThread().interrupt();

                        return;
                    }

                }
            } catch (Exception e) {
                log.error(String.format(
                        "Exception thrown when watching resources, path %s\nException:",
                        propertyFile.getPath().toString()
                ), e);
                stop();
            }
        }

        private Path path(final Object object) {
            return (Path) object;
        }

        private boolean isValidTargetFile(final Path target) {
            // TODO: test this
            return target.equals(propertyFile.getPath());
        }

    }
}
