package com.sproutigy.libs.luceneplus.core.search;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import com.sproutigy.libs.luceneplus.core.indices.LuceneIndices;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class MultiLuceneSearchResults extends AbstractLuceneSearchResults {
    @NonNull
    private LuceneIndices indices;
    @NonNull
    private String[] names;
    @NonNull
    private LuceneSearch search;

    private int nameIndex = -1;
    private LuceneIndex currentIndex;
    private LuceneSearchResults currentSearchResults;
    private Boolean next;
    private int count = 0;
    private LuceneSearchHit currentItem;

    @Override
    public boolean hasTotal() {
        return false;
    }

    @Override
    public Long total() {
        return null;
    }

    @Override
    public boolean hasCount() {
        return false;
    }

    @Override
    public Integer count() {
        return null;
    }

    @SneakyThrows
    @Override
    public boolean hasNext() {
        if (next != null) {
            return next;
        }

        if (search.getNumHits() != null && search.getNumHits() > 0 && count >= search.getNumHits()) {
            close();
            next = false;
            return false;
        }

        if (currentSearchResults != null && currentSearchResults.hasNext()) {
            next = true;
            return true;
        }

        while (nameIndex < names.length - 1) {
            if (currentIndex == null) {
                nameIndex++;
                String name = names[nameIndex];

                if (!indices.isOpen(name) && !indices.exists(name)) {
                    //in case index has been removed, skip it gracefully
                    indices.invalidate(name);
                    continue;
                }

                currentIndex = indices.acquire(name);
                currentSearchResults = currentIndex.search(search);
            }

            if (currentSearchResults != null && currentSearchResults.hasNext()) {
                next = true;
                return true;
            }

            close();
        }

        next = false;
        return false;
    }

    @SneakyThrows
    @Override
    public LuceneSearchHit next() {
        if (currentItem != null && currentItem instanceof LuceneSearchHitImpl) {
            ((LuceneSearchHitImpl)currentItem).unlinkSearcher();
        }
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        next = null;
        count++;
        currentItem = currentSearchResults.next();
        return currentItem;
    }

    @SneakyThrows
    @Override
    public void remove() {
        if (currentItem instanceof LuceneSearchHitImpl) {
            try (Reference<IndexWriter> writer = currentIndex.provideWriter()) {
                writer.use().tryDeleteDocument(((LuceneSearchHitImpl)currentItem).searcher.getIndexReader(), ((LuceneSearchHitImpl) currentItem).docId);
            }
        }
        throw new UnsupportedOperationException();
    }

    private static void unlinkSearcher(LuceneSearchHit item) {
        if (item != null && item instanceof LuceneSearchHitImpl) {
            ((LuceneSearchHitImpl)item).unlinkSearcher();
        }
    }

    @Override
    public void close() throws IOException {
        unlinkSearcher(currentItem);
        currentItem = null;

        if (currentSearchResults != null) {
            currentSearchResults.close();
            currentSearchResults = null;
        }
        if (currentIndex != null) {
            indices.release(currentIndex);
            currentIndex = null;
        }
    }
}
