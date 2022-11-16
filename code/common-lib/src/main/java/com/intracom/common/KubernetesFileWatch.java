package com.intracom.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class KubernetesFileWatch
{
    private static final Logger log = LoggerFactory.getLogger(KubernetesFileWatch.class);
    private final String root;
    private final Set<String> fileEntries;
    private boolean filterNonExisting;

    /**
     * 
     * @param root              Full path to the kubernetes secret root directory
     * @param fileEntries       The file entries to watch. Names are relative to the
     *                          root path
     * @param filterNonExisting If enabled, then validating of file existence and
     *                          length will take place. Default is true
     */
    private KubernetesFileWatch(String root,
                                Set<String> fileEntries,
                                boolean filterNonExisting)
    {
        Objects.requireNonNull(root);
        Objects.requireNonNull(fileEntries);
        Objects.requireNonNull(filterNonExisting);

        this.root = root;
        this.fileEntries = fileEntries;
        this.filterNonExisting = filterNonExisting;
    }

    /**
     * @return A new builder
     */
    public static Builder create()
    {
        return new Builder();
    }

    /**
     * Start watching for changes and emit changes filenames
     * 
     * @return A Map with key a file entry and value the resolved path
     */
    Flowable<Map<String, Path>> watchFileNames()
    {
        final var rootPath = Path.of(root);
        return RxFileWatch.create(rootPath)
                          .doOnNext(ev -> log.debug("File watch event:{}", ev))
                          .debounce(1, TimeUnit.SECONDS) //
                          .observeOn(Schedulers.io()) // File validation is blocking operation so we need to change scheduler
                          .concatMapMaybe(event -> this.filterNonExisting ? KubernetesFileWatch.resolveFiles(this.fileEntries, rootPath)
                                                                          : KubernetesFileWatch.resolveFilesUnfiltered(this.fileEntries, rootPath).toMaybe())
                          .doOnError(err -> log.warn("Error while validating files", err))
                          .doOnNext(ev -> log.info("Kubernetes file change event: {}", ev));
    }

    /**
     * Start watching for changes and emit file contents
     * 
     * @return A Map with key a file entry and value the full content of the file as
     *         a UTF-8 String. If filtering of non existing files is enabled, then
     *         value of the entry will be an empty string
     */
    Flowable<Map<String, String>> watchContents()
    {
        return watchFileNames().map(newFiles ->
        {
            log.info("Reading files: {}", newFiles);
            Map<String, String> fileContents = new HashMap<>();
            for (var entry : newFiles.entrySet())
            {
                if (!this.filterNonExisting && !Files.exists(entry.getValue()))
                {
                    fileContents.put(entry.getKey(), "");
                }
                else
                {
                    fileContents.put(entry.getKey(), Files.readString(entry.getValue()));
                }
            }
            return fileContents;
        });
    }

    /**
     * Watch for changes and emit either file contents or file names
     * 
     * @param readFileContents True if the contents should be read instead of the
     *                         filenames
     * @return A Map with key a file entry and value the resolved file name or the
     *         file contents
     */
    public Flowable<Map<String, String>> watch(boolean readFileContents)
    {
        return readFileContents ? watchContents()
                                : watchFileNames().map(fns -> fns.entrySet()
                                                                 .stream()
                                                                 .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().toString())));
    }

    /**
     * Validate that fileEntries to watch exist and have non zero length
     * 
     * @return Empty Maybe if error occured or at least one file was empty
     */
    private static Maybe<Map<String, Path>> resolveFiles(Set<String> fileEntries,
                                                         Path rootPath)
    {
        return Single.fromCallable(() -> //
        fileEntries.stream().collect(Collectors.toUnmodifiableMap(fileName -> fileName, path -> realPath(path, rootPath)))) //
                     .flatMapMaybe(KubernetesFileWatch::filterNonEmptyFiles)
                     .subscribeOn(Schedulers.io()); // Blocking
                                                    // operation
    }

    /**
     * Resolves the given paths, without filtering them based on their existence or
     * their length
     */
    private static Single<Map<String, Path>> resolveFilesUnfiltered(Set<String> fileEntries,
                                                                    Path rootPath)
    {
        return Single.fromCallable(() -> //
        fileEntries.stream().collect(Collectors.toUnmodifiableMap(fileName -> fileName, path -> resolvePath(path, rootPath)))) //
                     .subscribeOn(Schedulers.io()); // Blocking
                                                    // operation
    }

    private static Path realPath(String fileEntry,
                                 Path rootPath)
    {
        try
        {
            final var resolvedPath = rootPath.resolve(fileEntry);
            log.trace("Resolving file entry: {}", resolvedPath);

            return resolvedPath.toRealPath();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static Path resolvePath(String fileEntry,
                                    Path rootPath)
    {
        final var resolvedPath = rootPath.resolve(fileEntry);
        log.trace("Resolving file entry: {}", resolvedPath);

        return resolvedPath;
    }

    private static Maybe<Map<String, Path>> filterNonEmptyFiles(Map<String, Path> input)
    {
        return Maybe.defer(() ->
        {
            for (var path : input.values())
            {
                log.debug("Validating path: {}", path);
                final var realPath = path.toRealPath();
                final var length = realPath.toFile().length();
                log.debug("{} -> length: {}", path, length);
                if (length <= 0)
                {
                    return Maybe.empty();
                }
            }
            return Maybe.just(input);
        });
    }

    public static class Builder
    {
        private Set<String> fileEntries = new HashSet<>();
        private String rootPath;
        private boolean filterNonExisting = true;

        public Builder withRoot(String rootPath)
        {
            Objects.requireNonNull(rootPath);
            this.rootPath = rootPath;
            return this;
        }

        public Builder withFile(String fileName)
        {
            Objects.requireNonNull(fileName);
            this.fileEntries.add(fileName);
            return this;
        }

        public Builder withFilterNonExistingFiles(boolean value)
        {
            Objects.requireNonNull(value);
            this.filterNonExisting = value;
            return this;
        }

        public KubernetesFileWatch build()
        {
            return new KubernetesFileWatch(rootPath, fileEntries, filterNonExisting);
        }
    }
}
