package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.*;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import com.sproutigy.libs.luceneplus.core.search.MultiLuceneSearchResults;
import lombok.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AbstractLuceneIndices implements LuceneIndices {
    protected final ConcurrentHashMap<String, LuceneIndex> instantiated = new ConcurrentHashMap<>();

    protected final Map<String, Integer> acquisitionsCounters = new HashMap<>();
    protected Map<String, Long> lastReleaseTimestamp;

    @Getter @Setter
    private LuceneOpenMode openMode = LuceneOpenMode.CREATE_OR_UPDATE;

    @Getter
    private boolean autoCommit = true;

    @Getter
    private boolean autoFlush = false;

    @Getter @Setter
    private boolean autoOpen = true;

    @Getter @Setter
    private Analyzer analyzer = LuceneIndex.DEFAULT_ANALYZER;

    @Getter @Setter
    private IndexWriterConfigSupplier indexWriterConfigSupplier;

    @Getter
    private Long autoCloseMillis = null;

    private ScheduledExecutorService scheduler;

    private final Object lock = new Object();

    protected LuceneIndex prepareIndex(String name, Supplier<Directory> directorySupplier) {
        LuceneIndex index = new LuceneIndex(name, directorySupplier, indexWriterConfigSupplier);
        index.setAnalyzer(analyzer);
        index.setAutoCommit(false);
        index.setAutoFlush(false);
        index.setOpenMode(openMode);
        return index;
    }

    protected abstract Supplier<Directory> provideDirectorySupplier(String name) throws IOException;
    protected abstract boolean doDelete(String name) throws IOException;

    @Override
    public boolean exists(String name) throws IOException {
        return instantiated.containsKey(name) || names().contains(name);
    }

    @Override
    public Reference<LuceneIndex> provide(final String name) throws IOException {
        return new Reference<LuceneIndex>() {
            LuceneIndex index = null;

            @Override
            public LuceneIndex use() throws IOException {
                if (index == null) {
                    index = acquire(name);
                }
                return index;
            }

            @Override
            public void close() throws IOException {
                if (index != null) {
                    release(index);
                }
            }
        };
    }

    @Override
    public LuceneIndex acquire(@NonNull String name) throws IOException {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }

        if (autoCloseMillis != null) {
            synchronized (acquisitionsCounters) {
                Integer counter = acquisitionsCounters.get(name);
                if (counter == null) {
                    acquisitionsCounters.put(name, 1);
                } else {
                    acquisitionsCounters.put(name, counter + 1);
                }
            }
        }

        return instantiate(name); //TODO: count acquisitions per index to allow automatic close
    }

    @Override
    public void release(LuceneIndex index) throws IOException {
        if (index != null) {
            if (autoCloseMillis != null) {
                String name = index.getName();
                synchronized (acquisitionsCounters) {
                    Integer counter = acquisitionsCounters.get(name);
                    if (counter != null) {
                        counter--;
                        acquisitionsCounters.put(name, counter);
                        if (counter == 0) {
                            if (autoCloseMillis == 0) {
                                close(name);
                            } else {
                                lastReleaseTimestamp.put(name, System.currentTimeMillis());
                            }
                        }
                    }
                }
            }
        }
    }

    protected LuceneIndex instantiate(String name) throws IOException {
        LuceneIndex index = instantiated.get(name);
        if (index == null) {
            synchronized (lock) {
                index = instantiated.get(name);
                if (index == null) {
                    index = prepareIndex(name, provideDirectorySupplier(name));
                    if (isAutoOpen()) {
                        index.open();
                    }
                    onInstantiate(index, name);
                    instantiated.put(name, index);
                }
            }
        }
        return index;
    }

    protected void onInstantiate(LuceneIndex index, String name) throws IOException {
    }

    @Override
    public boolean delete(String name) throws IOException {
        try (val index = provide(name)) {
            try (val writer = index.use().provideWriter()) {
                writer.use().deleteAll();
            }
        }
        close(name);
        return doDelete(name);
    }

    @Override
    public Collection<String> names(String prefix) throws IOException {
        if (prefix == null || prefix.isEmpty()) {
            return names();
        }

        List<String> result = new LinkedList<>();
        for (String name : names()) {
            if (name.startsWith(prefix)) {
                result.add(name);
            }
        }
        return result;
    }

    @Override
    public void clear() throws IOException {
        while (!instantiated.isEmpty()) {
            for (String name : instantiated.keySet()) {
                delete(name);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        for (LuceneIndex index : instantiated.values()) {
            if (index.isOpen()) {
                try {
                    index.flush();
                } catch (IllegalStateException ignore) { }
            }
        }
    }

    @Override
    public void commit() throws IOException {
        for (LuceneIndex index : instantiated.values()) {
            if (index.isOpen()) {
                try {
                    index.commit();
                } catch (IllegalStateException ignore) { }
            }
        }
    }

    @Override
    public LuceneSearchResults search(LuceneSearch search) throws IOException {
        return search(search, new String[0]);
    }

    @Override
    public void optimize() throws IOException {
        for (LuceneIndex index : instantiated.values()) {
            if (!index.isOpen()) {
                index.open();
                index.optimize();
                index.close();
            } else {
                index.optimize();
            }
        }
    }

    @Override
    public LuceneSearchResults search(LuceneSearch search, String... names) throws IOException {
        String[] indicesNames = names;
        if (names == null || names.length == 0) {
            indicesNames = names().toArray(new String[0]);
        }
        return new MultiLuceneSearchResults(this, indicesNames, search);
    }

    @Override
    public boolean isOpen(String name) {
        LuceneIndex index = instantiated.get(name);
        if (index != null) {
            return index.isOpen();
        }
        return false;
    }

    @Override
    public Collection<LuceneIndex> getOpenedIndices() {
        return Collections.unmodifiableCollection(instantiated.values());
    }

    @Override
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        for (LuceneIndex index : instantiated.values()) {
            index.setAutoCommit(autoCommit);
        }
    }

    @Override
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
        for (LuceneIndex index : instantiated.values()) {
            index.setAutoFlush(autoFlush);
        }
    }

    @Override
    public void setAutoCloseMillis(Long delayMillis) {
        if (delayMillis != null && delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis < 0");
        }

        synchronized (acquisitionsCounters) {
            if (autoCloseMillis == null && lastReleaseTimestamp != null) {
                lastReleaseTimestamp.clear();
            } else if (delayMillis != null) {
                lastReleaseTimestamp = new HashMap<>();
            }

            autoCloseMillis = delayMillis;

            if (scheduler != null) {
                 scheduler.shutdownNow();
            }

            if (autoCloseMillis != null && autoCloseMillis > 0) {
                scheduler = Executors.newScheduledThreadPool(1);
                scheduler.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            doAutoClose();
                        } catch (Throwable ignore) { } //ensure that this will be called in the future
                    }
                }, autoCloseMillis, autoCloseMillis, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void setAutoClose(Long delay, @NonNull TimeUnit unit) {
        if (delay != null) {
            setAutoCloseMillis(unit.toMillis(delay));
        } else {
            setAutoCloseMillis(null);
        }
    }

    @Override
    public void setAutoCloseInstantly() {
        setAutoCloseMillis(0L);
    }

    @Override
    public boolean isEmpty() throws IOException {
        return isEmpty(null);
    }

    @SneakyThrows
    @Override
    public int count() {
        return names().size();
    }

    @Override
    public boolean isEmpty(String prefix) throws IOException {
        LuceneIndicesIterator iterator = iterator(prefix);
        try {
            while (iterator.hasNext()) {
                LuceneIndex index = iterator.next();
                if (!index.isEmpty()) {
                    return false;
                }
            }
        } finally {
            iterator.close();
        }

        return true;
    }

    @SneakyThrows
    @Override
    public LuceneIndicesIterator iterator() {
        return iterator(null);
    }

    @Override
    public LuceneIndicesIterator iterator(String prefix) throws IOException {
        final String[] names = names(prefix).toArray(new String[0]);
        return new LuceneIndicesIterator(this, names);
    }

    @Override
    public void close(String name) throws IOException {
        if (name != null) {
            LuceneIndex index;
            if (autoCloseMillis != null) {
                synchronized (acquisitionsCounters) {
                    index = instantiated.remove(name);
                    acquisitionsCounters.remove(name);
                    if (lastReleaseTimestamp != null) {
                        lastReleaseTimestamp.remove(name);
                    }
                }
            } else {
                index = instantiated.remove(name);
            }
            closeIndex(index);
        }
    }

    @Override
    public void close() throws IOException {
        while (!instantiated.isEmpty()) {
            for (String name : instantiated.keySet()) {
                close(name);
            }
        }
    }

    private void closeIndex(LuceneIndex index) throws IOException {
        if (index != null && index.isOpen()) {
            index.close();
        }
    }

    private void doAutoClose() {
        Collection<LuceneIndex> indicesToClose = new LinkedList<>();
        synchronized (acquisitionsCounters) {
            for (Map.Entry<String, Integer> entry : acquisitionsCounters.entrySet()) {
                if (entry.getValue() == 0) {
                    if (lastReleaseTimestamp != null) {
                        String name = entry.getKey();
                        Long timestamp = lastReleaseTimestamp.get(name);
                        if (timestamp != null) {
                            if (timestamp + autoCloseMillis < System.currentTimeMillis()) {
                                indicesToClose.add(instantiated.get(name));
                            }
                        }
                    }
                }
            }
        }

        for (LuceneIndex index : indicesToClose) {
            try {
                closeIndex(index);
            } catch (IOException ignore) { } //tried and failed, maybe next time
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable ignore) { }
        super.finalize();
    }
}
