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
import java.util.concurrent.*;

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

    @NonNull @Getter
    private AutoClosePolicy autoClosePolicy = AutoClosePolicy.DISABLED;

    private ScheduledExecutorService scheduler;

    private ExecutorService optimizationExecutor;

    protected final Object lock = new Object();

    protected LuceneIndex prepareIndex(String name, Supplier<Directory> directorySupplier) {
        LuceneIndex index = new LuceneIndex(name, directorySupplier, indexWriterConfigSupplier);
        index.setAnalyzer(analyzer);
        index.setAutoCommit(autoCommit);
        index.setAutoFlush(autoFlush);
        index.setOpenMode(openMode);
        return index;
    }

    protected abstract Supplier<Directory> provideDirectorySupplier(String name) throws IOException;
    protected abstract boolean doDelete(String name) throws IOException;

    @Override
    public boolean exists(String name, boolean allowCache) throws IOException {
        if (allowCache) {
            if (instantiated.containsKey(name)) {
                return true;
            }
        }

        return names(allowCache).contains(name);
    }

    @Override
    public boolean exists(String name) throws IOException {
        return exists(name, false);
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

        if (getAutoCloseMillis() != null) {
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
        AutoClosePolicy autoClosePolicy = getAutoClosePolicy();
        if (index != null) {
            if (autoClosePolicy.isEnabled()) {
                String name = index.getName();
                synchronized (acquisitionsCounters) {
                    Integer counter = acquisitionsCounters.get(name);
                    if (counter != null) {
                        counter--;
                        acquisitionsCounters.put(name, counter);
                        if (counter == 0) {
                            if (autoClosePolicy.getDelayMillis() == 0) {
                                if (autoClosePolicy.isOptimize()) {
                                    try {
                                        index.optimize();
                                    } catch (Throwable ignore) {
                                    }
                                }

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
        else {
            if (!index.isOpen() && isAutoOpen()) {
                index.open();
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
    public Collection<String> names() throws IOException {
        return names(false);
    }

    @Override
    public Collection<String> names(String prefix) throws IOException {
        return names(prefix, false);
    }

    @Override
    public Collection<String> names(String prefix, boolean allowCache) throws IOException {
        if (prefix == null || prefix.isEmpty()) {
            return names(allowCache);
        }

        List<String> result = new LinkedList<>();
        for (String name : names(allowCache)) {
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
            indicesNames = names(true).toArray(new String[0]);
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

    public void setAutoClosePolicy(@NonNull AutoClosePolicy autoClosePolicy) {
        this.autoClosePolicy = autoClosePolicy;

        Long delayMillis = autoClosePolicy.isEnabled() ? autoClosePolicy.getDelayMillis() : null;
        if (delayMillis != null && delayMillis < 0) {
            throw new IllegalArgumentException("delay < 0");
        }

        synchronized (acquisitionsCounters) {
            if (delayMillis == null && lastReleaseTimestamp != null) {
                lastReleaseTimestamp.clear();
            } else if (delayMillis != null) {
                lastReleaseTimestamp = new HashMap<>();
            }

            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }

            if (optimizationExecutor != null) {
                optimizationExecutor.shutdownNow();
                optimizationExecutor = null;
            }

            optimizationExecutor = Executors.newSingleThreadExecutor( new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, AbstractLuceneIndices.this.toString() + "-optimize");
                    if (t.isDaemon())
                        t.setDaemon(false);
                    if (t.getPriority() != Thread.MIN_PRIORITY + 1)
                        t.setPriority(Thread.MIN_PRIORITY + 1);
                    return t;
                }
            });

            if (delayMillis != null && delayMillis > 0) {
                scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, AbstractLuceneIndices.this.toString() + "-autoClose");
                        if (!t.isDaemon())
                            t.setDaemon(true);
                        if (t.getPriority() != Thread.NORM_PRIORITY - 1)
                            t.setPriority(Thread.NORM_PRIORITY - 1);
                        return t;
                    }
                });

                scheduler.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            doAutoClose();
                        } catch (Throwable ignore) { } //ensure that this will be called in the future
                    }
                }, delayMillis, delayMillis, TimeUnit.MILLISECONDS);
            }
        }
    }

    private Long getAutoCloseMillis() {
        AutoClosePolicy policy = getAutoClosePolicy();
        return policy.isEnabled() ? policy.getDelayMillis() : null;
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
    public void invalidate(String name) throws IOException {
        if (!exists(name)) {
            try {
                close(name);
            } catch (Exception ignore) {
            }
            instantiated.remove(name);
        }
    }

    @Override
    public void invalidate() throws IOException {
        for (String name : instantiated.keySet()) {
            synchronized (lock) {
                invalidate(name);
            }
        }
    }


    @Override
    public void close(String name) throws IOException {
        if (name != null) {
            LuceneIndex index;
            if (getAutoCloseMillis() != null) {
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
                try {
                    close(name);
                } catch (Throwable e) {
                    throw new IOException("Could not close index: " + name, e);
                }
            }
        }
    }

    private void closeIndex(LuceneIndex index) throws IOException {
        if (index != null && index.isOpen()) {
            index.close();
        }
    }

    private void doAutoClose() {
        AutoClosePolicy policy = getAutoClosePolicy();
        if (policy == null) return;

        long autoCloseMillis = policy.getDelayMillis();

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

        for (final LuceneIndex index : indicesToClose) {
            try {
                instantiated.remove(index.getName());

                Executor optimizationExecutor = this.optimizationExecutor;
                if (policy.isOptimize() && optimizationExecutor != null) {
                    optimizationExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                index.optimize();
                            } catch (Throwable ignore) { }

                            try {
                                closeIndex(index);
                            } catch (Throwable ignore) { }
                        }
                    });
                } else {
                    closeIndex(index);
                }
            } catch (Throwable ignore) { }
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
