package com.sproutigy.libs.luceneplus.core;

import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchHit;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LuceneIndexTest {
    @Test
    public void testKeywords() throws IOException {
        LuceneIndex index = new LuceneIndex();
        try (Reference<IndexWriter> writer = index.provideWriter()) {
            Document doc1 = new Document();
            LuceneFields.Keyword.add(doc1,"name", "John", LuceneFields.FieldOptions.STORE_INDEX);
            writer.use().addDocument(doc1);

            Document doc2 = new Document();
            LuceneFields.Keyword.add(doc2,"name", "James", LuceneFields.FieldOptions.STORE_INDEX);
            writer.use().addDocument(doc2);
        }

        LuceneSearchResults results = index.search(LuceneSearch.builder().query(new TermQuery(new Term("name", "James"))).build());
        assertTrue(results.hasCount());
        assertEquals(1, (int)results.count());
        assertEquals("James", results.toList().get(0).getField("name").stringValue());
    }

    @Test
    public void testNumericValues() throws IOException {
        LuceneIndex index = new LuceneIndex();
        try (Reference<IndexWriter> writer = index.provideWriter()) {
            Document doc1 = new Document();
            LuceneFields.Long.add(doc1, "id", 1L, LuceneFields.FieldOptions.STORE_INDEX);
            LuceneFields.Double.add(doc1, "val", 5.0d, LuceneFields.FieldOptions.INDEX_DOCVALUE);
            writer.use().addDocument(doc1);

            Document doc2 = new Document();
            LuceneFields.Long.add(doc2, "id", 2L, LuceneFields.FieldOptions.STORE_INDEX);
            LuceneFields.Double.add(doc2, "val", 3.0d, LuceneFields.FieldOptions.INDEX_DOCVALUE);
            writer.use().addDocument(doc2);

            Document doc3 = new Document();
            LuceneFields.Long.add(doc3, "id", 3L, LuceneFields.FieldOptions.STORE_INDEX);
            LuceneFields.Double.add(doc3, "val", 7.0d, LuceneFields.FieldOptions.INDEX_DOCVALUE);
            writer.use().addDocument(doc3);
        }

        LuceneSearch search = LuceneSearch.builder().sort(new Sort(new SortField("val", SortField.Type.DOUBLE))).build();
        List<LuceneSearchHit> hits = index.search(search).toList();
        assertEquals(3, hits.size());
        assertEquals(2L, (long)LuceneFields.Long.get(hits.get(0).getField("id")));
        assertEquals(1L, (long)LuceneFields.Long.get(hits.get(1).getField("id")));
        assertEquals(3L, (long)LuceneFields.Long.get(hits.get(2).getField("id")));
    }
}
