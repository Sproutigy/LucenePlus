package com.sproutigy.libs.luceneplus.core.search;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
public class SingleLuceneSearchResults extends AbstractLuceneSearchResults {
    @NonNull @Getter
    private TopDocs topDocs;

    @NonNull
    private IndexSearcher searcher;

    @NonNull
    private LuceneIndex index;

    private LuceneSearchHitImpl current;

    private int i = -1;

    @Override
    public boolean hasTotal() {
        return true;
    }

    @Override
    public long total() {
        return topDocs.totalHits;
    }

    @Override
    public boolean hasCount() {
        return true;
    }

    @Override
    public int count() {
        return topDocs.scoreDocs.length;
    }

    @SneakyThrows
    @Override
    public boolean hasNext() {
        if (i < topDocs.scoreDocs.length - 1) {
            return true;
        }
        close();
        return false;
    }

    @SneakyThrows
    @Override
    public LuceneSearchHit next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (current != null) {
            current.unlinkSearcher();
        }

        i++;
        ScoreDoc scoreDoc = topDocs.scoreDocs[i];
        current = new LuceneSearchHitImpl(index.getName(), searcher, scoreDoc.doc, scoreDoc.score);
        return current;
    }

    @SneakyThrows
    @Override
    public void remove() {
        try (Reference<IndexWriter> writer = index.provideWriter()) {
            writer.use().tryDeleteDocument(current.getReader(), current.getDocId());
        }
    }

    @Override
    public void close() throws IOException {
        if (current != null) {
            current.unlinkSearcher();
            current = null;
        }

        if (searcher != null) {
            index.release(searcher);
            searcher = null;
        }
    }
}
