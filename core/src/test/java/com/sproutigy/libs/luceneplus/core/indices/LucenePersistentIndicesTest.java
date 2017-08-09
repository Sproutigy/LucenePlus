package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneFields;
import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import org.apache.lucene.document.Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;

import static com.sproutigy.libs.luceneplus.core.indices.FSLuceneIndices.deleteDirectoryIfExists;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LucenePersistentIndicesTest {
    @Test
    public void test() throws IOException {
        Path root = Files.createTempDirectory("LucenePlus-test");
        deleteDirectoryIfExists(root);

        LuceneIndices indices = new FSLuceneIndices(root);
        try {
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

            assertTrue(Files.exists(root.resolve("test1")));
            assertTrue(Files.exists(root.resolve("test2")));
            assertFalse(Files.exists(root.resolve("test3")));
        } finally {
            indices.close();
            deleteDirectoryIfExists(root);
        }
    }
}
