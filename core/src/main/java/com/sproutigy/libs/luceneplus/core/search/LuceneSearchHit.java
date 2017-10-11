package com.sproutigy.libs.luceneplus.core.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

public interface LuceneSearchHit extends Closeable {
    String getIndexName();
    float getScore();
    int getDocId();
    Document getDocument() throws IOException;
    Document getDocument(Set<String> fieldNames) throws IOException;
    IndexableField getField(String name) throws IOException;
    IndexSearcher getSearcher();
    IndexReader getReader();
}
