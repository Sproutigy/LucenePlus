package com.sproutigy.libs.luceneplus.core.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class EmptyLuceneSearchResults extends AbstractLuceneSearchResults {
    public static final EmptyLuceneSearchResults INSTANCE = new EmptyLuceneSearchResults();

    @Override
    public boolean hasTotal() {
        return true;
    }

    @Override
    public long total() {
        return 0;
    }

    @Override
    public boolean hasCount() {
        return true;
    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public LuceneSearchHit next() {
        throw new NoSuchElementException();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public List<LuceneSearchHit> toList() throws IOException {
        return Collections.emptyList();
    }
}
