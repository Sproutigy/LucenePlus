package com.sproutigy.libs.luceneplus.core.search;

import lombok.Getter;
import lombok.ToString;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@ToString(of = {"indexName", "docId", "score"})
public final class LuceneSearchHitImpl implements LuceneSearchHit {
    @Getter
    private String indexName;

    IndexSearcher searcher;

    @Getter
    private int docId;

    @Getter
    private float score;

    private Document document;

    LuceneSearchHitImpl(String indexName, IndexSearcher searcher, int docId, float score) {
        this.indexName = indexName;
        this.searcher = searcher;
        this.docId = docId;
        this.score = score;
    }

    @Override
    public Document getDocument() throws IOException {
        if (document == null) {
            fetchDocument();
        }
        return document;
    }

    void fetchDocument() throws IOException {
        document = getSearcher().doc(docId);
    }

    @Override
    public Document getDocument(Set<String> fieldNames) throws IOException {
        if (document == null) {
            return getSearcher().doc(docId, fieldNames);
        } else {
            Document doc = new Document();
            for (String fieldName : fieldNames) {
                IndexableField field = document.getField(fieldName);
                if (field != null) {
                    doc.add(field);
                }
            }
            return doc;
        }
    }

    @Override
    public IndexableField getField(String name) throws IOException {
        Document document = getDocument(Collections.singleton(name));
        if (document != null) {
            for (IndexableField field : document) {
                if (field != null && Objects.equals(field.name(), name)) {
                    return field;
                }
            }
        }
        return null;
    }

    @Override
    public IndexSearcher getSearcher() {
        if (searcher == null) {
            throw new IllegalStateException("Searcher not available - already unlinked or closed");
        }
        return searcher;
    }

    @Override
    public IndexReader getReader() {
        return getSearcher().getIndexReader();
    }

    void unlinkSearcher() {
        searcher = null;
    }

    @Override
    public void close() {
        unlinkSearcher();
    }
}
