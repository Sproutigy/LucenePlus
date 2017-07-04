package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneFields;
import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import org.apache.lucene.document.Document;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LucenePersistentIndicesTest {
    @Test
    public void test() throws IOException {
        Path root = Files.createTempDirectory("LucenePlus-test");
        deleteDirectoryIfExists(root);

        try {
            LuceneIndices indices = new FSLuceneIndices(root);
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
            indices.close();
        } finally {
            deleteDirectoryIfExists(root);
        }
    }

    private static void deleteDirectoryIfExists(final Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
