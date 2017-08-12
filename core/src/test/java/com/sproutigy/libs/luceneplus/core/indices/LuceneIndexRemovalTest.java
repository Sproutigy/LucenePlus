package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneFields;
import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchHit;
import org.apache.lucene.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.sproutigy.libs.luceneplus.core.indices.FSLuceneIndices.deleteDirectoryIfExists;
import static org.junit.Assert.*;

public class LuceneIndexRemovalTest {
    Path root;
    LuceneIndices indices;

    @Before
    public void beforeEach() throws IOException {
        root = Files.createTempDirectory("LucenePlus-test");
        deleteDirectoryIfExists(root);
        indices = new FSLuceneIndices(root);

        indices.setAutoCloseInstantly();

        try (Reference<LuceneIndex> index1 = indices.provide("test1")) {
            Document doc = new Document();
            LuceneFields.Text.add(doc, "text", "Hello World", LuceneFields.FieldOptions.STORE_INDEX);
            index1.use().addDocument(doc);
        }

        try (Reference<LuceneIndex> index2 = indices.provide("test2")) {
            Document doc = new Document();
            LuceneFields.Text.add(doc, "text", "Sorry, nothing important here", LuceneFields.FieldOptions.STORE_INDEX);
            index2.use().addDocument(doc);
        }
    }


    private void preAssertions() throws IOException {
        assertTrue(indices.exists("test1"));
        assertFalse(indices.isOpen("test1"));
    }


    @Test
    public void testIndexDeleteByAPI() throws IOException, InterruptedException {
        preAssertions();

        indices.delete("test1");

        assertFalse(indices.exists("test1", true));
        assertFalse(indices.names(true).contains("test1"));

        postAssertions();
    }


    @Test
    public void testIndexDeleteByDirectoryRemoval() throws IOException, InterruptedException {
        preAssertions();

        deleteDirectoryIfExists(root.resolve("test1"));

        assertTrue(indices.exists("test1", true));
        assertTrue(indices.names(true).contains("test1"));

        postAssertions();
    }


    private void postAssertions() throws IOException {
        //check search - should gracefully ignore deleted index
        List<LuceneSearchHit> hits = indices.search(LuceneSearch.MATCH_ALL).toList();
        assertEquals(1, hits.size());

        assertFalse(indices.exists("test1"));
        assertTrue(indices.exists("test2"));
        assertEquals(1, indices.names().size());
        assertEquals("test2", indices.names().iterator().next());
    }


    @After
    public void afterEach() throws IOException {
        indices.close();
        deleteDirectoryIfExists(root);
    }
}
