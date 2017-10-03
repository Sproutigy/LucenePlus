package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.CloseableIterator;
import com.sproutigy.libs.luceneplus.core.LuceneFields;
import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchHit;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class LuceneIndicesTest {
    @Test
    public void testSearch() throws IOException {
        LuceneIndices indices = new MemoryLuceneIndices();
        fillIndex(indices, "a");
        fillIndex(indices, "b");
        assertTrue(indices.exists("a"));
        assertTrue(indices.exists("b"));
        assertFalse(indices.exists("c"));

        LuceneSearch search = LuceneSearch.builder().query(new TermQuery(new Term("n", "b"))).numHits(1).build();
        LuceneSearchResults results = indices.search(search);

        assertFalse(results.hasCount());
        assertEquals(1, results.count());
        assertTrue(results.hasCount());

        assertFalse(results.hasTotal());
        assertEquals(1, results.total());
        assertTrue(results.hasTotal());

        List<LuceneSearchHit> items = results.toList();
        assertEquals(1, items.size());
        assertEquals("b", items.get(0).getIndexName());
        assertEquals("b", items.get(0).getField("n").stringValue());

        assertEquals(2, indices.names().size());
        assertTrue(indices.isOpen("a"));
        assertTrue(indices.isOpen("b"));
        assertFalse(indices.isOpen("c"));

        int count = 0;
        try (CloseableIterator<LuceneIndex> iterator = indices.iterator()) {
            while(iterator.hasNext()) {
                LuceneIndex index = iterator.next();
                count++;
                if (count == 1) assertEquals("a", index.getName());
                if (count == 2) assertEquals("b", index.getName());
                iterator.remove();
            }
        }
        assertEquals(2, count);

        //after deletion
        assertFalse(indices.isOpen("a"));
        assertFalse(indices.isOpen("b"));
        assertFalse(indices.exists("a"));
        assertFalse(indices.exists("b"));
        assertEquals(0, indices.names().size());
    }

    @Test
    public void testAutoCloseInstantly() throws IOException {
        LuceneIndices indices = new MemoryLuceneIndices();
        fillIndex(indices, "x");
        assertTrue(indices.isOpen("x"));
        indices.setAutoClosePolicy(AutoClosePolicy.INSTANTLY_OPTIMIZE);
        fillIndex(indices, "x");
        assertFalse(indices.isOpen("x"));
    }

    @Test
    public void testAutoCloseDelayed() throws IOException, InterruptedException {
        LuceneIndices indices = new MemoryLuceneIndices();
        indices.setAutoClosePolicy(AutoClosePolicy.builder().delay(100, TimeUnit.MILLISECONDS).build());
        fillIndex(indices, "x");
        assertTrue(indices.isOpen("x"));

        Thread.sleep(1000);
        assertFalse(indices.isOpen("x"));
    }

    @Test
    public void testAutoCloseWithMultiplyAcquiredIndex() throws IOException {
        LuceneIndices indices = new MemoryLuceneIndices();
        indices.setAutoClosePolicy(AutoClosePolicy.INSTANTLY);
        LuceneIndex indexInstance1 = indices.acquire("a");
        LuceneIndex indexInstance2 = indices.acquire("a");
        indices.release(indexInstance2);
        assertTrue(indices.isOpen("a"));
        indices.release(indexInstance1);
        assertFalse(indices.isOpen("a"));
    }

    private void fillIndex(LuceneIndices indices, String indexName) throws IOException {
        try (Reference<LuceneIndex> index = indices.provide(indexName)) {
            try (Reference<IndexWriter> writer = index.use().provideWriter()) {
                Document doc = new Document();
                LuceneFields.Keyword.add(doc, "n", indexName, LuceneFields.FieldOptions.STORE_INDEX);
                writer.use().addDocument(doc);
            }
        }
    }
}
