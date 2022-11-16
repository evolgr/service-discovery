package com.intracom.common.file;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

/**
 * 
 */
public class RxFileWatch
{
    private static final Logger log = LoggerFactory.getLogger(RxFileWatch.class);
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private boolean trace = true;
    private final EventHandler eventHandler;
    private volatile boolean running = true;
    private boolean recursive;

    public static final WatchEvent.Kind<Path> INIT_EVENT = new WatchEvent.Kind<Path>()
    {
        @Override
        public String name()
        {
            return "INIT_EVENT";
        }

        @Override
        public Class<Path> type()
        {
            return Path.class;
        }

        @Override
        public String toString()
        {
            return "INIT_EVENT";
        }
    };

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event)
    {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException
    {
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        if (trace)
        {
            Path prev = keys.get(key);
            if (prev == null)
            {
                log.debug("register: {}", dir);
            }
            else
            {
                if (!dir.equals(prev))
                {
                    log.debug("update: {} -> {}", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException
    {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    RxFileWatch(Path dir,
                final EventHandler eventHandler,
                boolean recursive) throws IOException
    {
        this.eventHandler = eventHandler;
        this.recursive = recursive;
        this.watcher = FileSystems.getDefault().newWatchService();

        this.keys = new HashMap<>();

        // enable trace after initial registration
        this.trace = true;
        registerAll(dir);
    }

    /**
     * Process all events for keys queued to the watcher
     * 
     * @throws IOException
     */
    private void processEvents()
    {
        log.debug("Starting event loop");
        try
        {
            while (running)
            {
                // wait for key to be signaled
                final var key = watcher.poll(1, TimeUnit.SECONDS);
                if (key == null)
                {
                    continue;
                }

                final var dir = keys.get(key);
                if (dir == null)
                {
                    log.error("WatchKey not recognized: {}", key);
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents())
                {
                    final var kind = event.kind();

                    // Context for directory entry event is the file name of entry
                    WatchEvent<Path> watchEvent = cast(event);
                    final var changedFile = kind != StandardWatchEventKinds.OVERFLOW ? dir.resolve(watchEvent.context()) : dir; // Treat overflow event as a
                                                                                                                                // change for root dir

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE //
                        && Files.isDirectory(changedFile, LinkOption.NOFOLLOW_LINKS) && recursive)
                    {
                        try
                        {
                            registerAll(changedFile);
                        }
                        catch (Exception e)
                        {
                            // This might happen if directory is created and deleted in rapid succession
                            log.warn("Unexpected error after processing directory creation event for file {}", changedFile, e);
                        }
                    }

                    this.eventHandler.onEvent(new Event(watchEvent.kind(), changedFile));
                }

                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid)
                {
                    log.debug("Directory no longer valid key: {} dir: {}", key, dir);

                    keys.remove(key);
                    if (keys.isEmpty())
                    {
                        log.warn("Directory no longer valid, file watch terminating, dir: {}", dir);
                        this.running = false;
                    }
                }
            }
        }
        catch (InterruptedException x)
        {
            log.debug("File Watch interrupted");
            Thread.currentThread().interrupt();
        }
        finally
        {
            log.debug("Cleanup event loop");
            try
            {
                keys.keySet().forEach(WatchKey::cancel);
                keys.clear();
            }
            finally
            {
                try
                {
                    watcher.close();
                }
                catch (IOException e)
                {
                    log.debug("Failed to close watcher", e);
                }
            }
        }
    }

    public static Flowable<Event> create(Path path)
    {
        return create(path, true);
    }

    public static Flowable<Event> create(Path path,
                                         boolean recursive)
    {
        return Flowable.<Event>create(emitter ->
        {

            final var watcher = new RxFileWatch(path, event ->
            {
                if (!emitter.isCancelled())
                {
                    emitter.onNext(event);
                }
            }, recursive);
            emitter.setCancellable(() ->
            {
                watcher.running = false;
                log.debug("Disposing File Watcher for path: {}", path);
            });
            emitter.onNext(new Event(INIT_EVENT, path));
            try
            {
                watcher.processEvents(); // infinite loop
            }
            catch (Exception err)
            {
                emitter.onError(err);
            }
            // event loop terminated
            emitter.onComplete();

        }, BackpressureStrategy.LATEST).subscribeOn(Schedulers.newThread());

    }

    public interface EventHandler
    {
        void onEvent(Event event);
    }

    public static class Event
    {
        final Path path;
        final Kind<Path> kind;

        public Event(Kind<Path> kind,
                     Path path)
        {
            this.path = path;
            this.kind = kind;
        }

        public Path getPath()
        {
            return path;
        }

        public Kind<Path> getKind()
        {
            return kind;
        }

        @Override
        public String toString()
        {
            var builder = new StringBuilder();
            builder.append("Event [path=");
            builder.append(path);
            builder.append(", kind=");
            builder.append(kind);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(kind, path);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Event other = (Event) obj;
            return Objects.equals(kind, other.kind) && Objects.equals(path, other.path);
        }

    }
}
