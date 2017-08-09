package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.*;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import lombok.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface LuceneIndices extends Iterable<LuceneIndex>, LuceneIndexOperations, Closeable {
    void setIndexWriterConfigSupplier(IndexWriterConfigSupplier indexWriterConfigSupplier);
    IndexWriterConfigSupplier getIndexWriterConfigSupplier();

    int count();

    void setAutoOpen(boolean autoOpen);
    boolean isAutoOpen();

    void setAutoCloseMillis(Long delayMillis);
    void setAutoClose(Long delay, @NonNull TimeUnit unit);
    void setAutoCloseInstantly();
    Long getAutoCloseMillis();

    boolean isEmpty(String prefix) throws IOException;

    boolean exists(String name) throws IOException;
    boolean exists(String name, boolean allowCache) throws IOException;

    Reference<LuceneIndex> provide(String name) throws IOException;
    LuceneIndex acquire(String name) throws IOException;
    void release(LuceneIndex index) throws IOException;

    Collection<String> names() throws IOException;
    Collection<String> names(boolean allowCache) throws IOException;
    Collection<String> names(String prefix) throws IOException;
    Collection<String> names(String prefix, boolean allowCache) throws IOException;

    LuceneSearchResults search(LuceneSearch search, String... names) throws IOException;

    boolean isOpen(String name);
    Collection<LuceneIndex> getOpenedIndices();

    CloseableIterator<LuceneIndex> iterator();
    CloseableIterator<LuceneIndex> iterator(String prefix) throws IOException;

    void invalidate(String name) throws IOException;
    void invalidate() throws IOException;

    boolean delete(String name) throws IOException;
    void close(String name) throws IOException;
}
