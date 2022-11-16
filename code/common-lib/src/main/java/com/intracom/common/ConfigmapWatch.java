package com.intracom.common;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;

/**
 * 
 */
public class ConfigmapWatch
{
    private static final Logger log = LoggerFactory.getLogger(ConfigmapWatch.class);
    private final String root;
    private final String fileName;
    private final boolean readContents;

    public ConfigmapWatch(ConfigmapWatchBuilder builder)
    {
        this.fileName = builder.fileName;
        this.root = builder.root;
        this.readContents = true;
    }

    public static ConfigmapWatchBuilder builder()
    {
        return new ConfigmapWatchBuilder();
    }

    public static ConfigmapWatch file(String root)
    {
        return builder().build(root);
    }

    public Flowable<ConfigmapFile> watch()
    {
        return KubernetesFileWatch.create() // create file watch operation
                                  .withRoot(this.root + "/") // set the root folder
                                  .withFile(this.fileName) // set the file name
                                  .build()
                                  .watch(this.readContents) // watch for changes and emit file contents
                                  .doOnSubscribe(c -> log.info("Started monitoring configmap file: {}/{}", this.root, this.fileName))
                                  .map(ConfigmapFile::new) // create new configmap object with data
                                  .filter(ConfigmapFile::isNonEmpty); // keep the non empty files
    }

    public class ConfigmapFile
    {
        private final String data;

        ConfigmapFile(Map<String, String> contents)
        {
            Objects.requireNonNull(contents);
            this.data = contents.get(fileName);
        }

        boolean isNonEmpty()
        {
            return !this.data.isBlank();
        }

        public String getData()
        {
            return this.data;
        }
    }

    public static class ConfigmapWatchBuilder
    {
        private String root;
        private String fileName;

        public ConfigmapWatchBuilder withRoot(String root)
        {
            this.root = root;
            return this;
        }

        public ConfigmapWatchBuilder withFileName(String fileName)
        {
            this.fileName = fileName;
            return this;
        }

        public ConfigmapWatch build(String root)
        {
            this.root = root;
            return new ConfigmapWatch(this);
        }

        public ConfigmapWatch build()
        {
            return new ConfigmapWatch(this);
        }
    }
}
