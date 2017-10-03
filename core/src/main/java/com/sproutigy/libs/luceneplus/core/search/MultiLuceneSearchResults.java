package com.sproutigy.libs.luceneplus.core.search;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import com.sproutigy.libs.luceneplus.core.indices.LuceneIndices;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.lucene.index.IndexWriter;

import java.io.IOException;
import java.util.NoSuchElementException;

public class MultiLuceneSearchResults extends AbstractLuceneSearchResults {
    @NonNull
    private LuceneIndices indices;
    @NonNull
    private String[] names;
    @NonNull
    private LuceneSearch search;

    public MultiLuceneSearchResults(@NonNull LuceneIndices indices, @NonNull String[] names, @NonNull LuceneSearch search) {
        this.indices = indices;
        this.names = names;
        this.search = search;

        calculateTotal = search.getNumHits() == null;
    }

    private int nameIndex = -1;
    private LuceneIndex currentIndex;
    private LuceneSearchResults currentSearchResults;
    private Boolean next;
    private int count = -1;
    private int total = -1;
    private LuceneSearchHit currentItem;

    private boolean calculateTotal;
    private boolean skipping = false;
    private int aggregatedCount = 0;
    private int aggregatedTotal = 0;

    @Override
    public boolean hasTotal() {
        return total != -1;
    }

    @Override
    public long total() {
        if (!hasTotal()) {
            calculateTotal = true;
            iterateAndFinish();
        }
        return total;
    }

    @Override
    public boolean hasCount() {
        return count != -1;
    }

    @Override
    public int count() {
        if (!hasCount()) {
            iterateAndFinish();
        }
        return count;
    }

    private void iterateAndFinish() {
        while (hasNext()) {
            next();
        }
        try {
            finished();
        } catch (Exception ignore) { }
    }

    @SneakyThrows
    @Override
    public boolean hasNext() {
        if (next != null) {
            return next;
        }

        if (!skipping && search.getNumHits() != null && search.getNumHits() > 0 && aggregatedCount >= search.getNumHits()) {
            if (calculateTotal) {
                skipping = true;
                iterateAndFinish();
            } else {
                finished();
            }
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
                if (calculateTotal) {
                    aggregatedTotal += currentSearchResults.total();
                }
            }

            if (currentSearchResults != null && currentSearchResults.hasNext()) {
                next = true;
                return true;
            }

            release();
        }

        finished();
        next = false;
        return false;
    }

    private void finished() throws IOException {
        if (!hasCount()) {
            count = aggregatedCount;
        }
        if (!hasTotal() && calculateTotal) {
            total = aggregatedTotal;
        }
        close();
    }

    private void release() throws IOException {
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
        aggregatedCount++;
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
        aggregatedCount = 0;
        aggregatedTotal = 0;

        nameIndex = -1;
        skipping = false;
        next = null;

        release();
    }
}
