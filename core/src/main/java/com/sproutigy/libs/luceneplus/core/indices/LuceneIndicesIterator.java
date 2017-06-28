package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.CloseableIterator;
import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class LuceneIndicesIterator implements CloseableIterator<LuceneIndex> {
    @NonNull
    private LuceneIndices indices;
    @NonNull
    private String[] names;

    private int i = -1;
    private String name;
    private LuceneIndex currentIndex;

    @SneakyThrows
    @Override
    public boolean hasNext() {
        boolean hasNext = i < names.length - 1;
        if (!hasNext) {
            release();
        }
        return hasNext;
    }

    @SneakyThrows
    @Override
    public LuceneIndex next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        i++;
        name = names[i];
        currentIndex = indices.acquire(name);
        return currentIndex;
    }

    @SneakyThrows
    @Override
    public void remove() {
        release();
        indices.delete(name);
    }

    private void release() throws IOException {
        if (currentIndex != null) {
            indices.release(currentIndex);
            currentIndex = null;
        }
    }

    @Override
    public void close() throws IOException {
        release();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable ignore) { }

        super.finalize();
    }
}
