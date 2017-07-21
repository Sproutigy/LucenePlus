package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Supplier;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ToString
@EqualsAndHashCode(callSuper = false)
public class FSLuceneIndices extends AbstractLuceneIndices {
    private Path rootPath;
    private List<String> cachedNames;

    public FSLuceneIndices(@NonNull Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    protected Supplier<Directory> provideDirectorySupplier(final String name) throws IOException {
        return new Supplier<Directory>() {
            @SneakyThrows
            @Override
            public Directory get() {
                return FSDirectory.open(resolvePath(name));
            }
        };
    }

    @Override
    protected boolean doDelete(String name) throws IOException {
        if (cachedNames != null) {
            cachedNames.remove(name);
        }
        return Files.deleteIfExists(resolvePath(name));
    }

    @Override
    public boolean exists(String name) throws IOException {
        if (cachedNames != null && cachedNames.contains(name)) {
            return true;
        }
        return Files.exists(resolvePath(name));
    }

    @Override
    protected void onInstantiate(LuceneIndex index, String name) throws IOException {
        if (cachedNames == null) {
            names(); //fill cache
        }
        if (cachedNames != null) {
            cachedNames.add(name);
        }
    }

    @Override
    public Collection<String> names() throws IOException {
        if (cachedNames == null) {
            try {
                List<String> names = new LinkedList<>();
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath)) {
                    for (Path path : directoryStream) {
                        names.add(path.getFileName().toString());
                    }
                }
                cachedNames = new CopyOnWriteArrayList<>(names);
            } catch (NoSuchFileException noSuchFile) {
                return Collections.emptySet();
            }
        }
        return cachedNames;
    }

    public Path resolvePath(String name) {
        return rootPath.resolve(name);
    }

    @Override
    public String toString() {
        return rootPath.toString();
    }
}
