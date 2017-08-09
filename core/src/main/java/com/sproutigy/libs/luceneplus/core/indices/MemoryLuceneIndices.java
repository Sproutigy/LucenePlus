package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.Supplier;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

@ToString
public class MemoryLuceneIndices extends AbstractLuceneIndices {
    @Override
    protected Supplier<Directory> provideDirectorySupplier(String name) throws IOException {
        return new Supplier<Directory>() {
            @SneakyThrows
            @Override
            public Directory get() {
                return new RAMDirectory();
            }
        };
    }

    @Override
    protected boolean doDelete(String name) throws IOException {
        return true;
    }

    @Override
    public Collection<String> names(boolean allowCache) {
        return Collections.unmodifiableSet(new TreeSet<>(instantiated.keySet()));
    }
}
