package com.sproutigy.libs.luceneplus.core;

import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import com.sproutigy.libs.luceneplus.core.search.SingleLuceneSearchResults;
import lombok.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public class LuceneIndex implements LuceneIndexOperations, Closeable {
    public static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();

    private Supplier<Directory> directorySupplier;
    private IndexWriterConfigSupplier indexWriterConfigSupplier;

    @Getter
    private String name;

    @Getter
    protected Directory directory;
    protected boolean owningDirectory = false;

    @Getter @Setter
    private boolean autoCommit = true;

    @Getter @Setter
    private boolean autoFlush = false;

    @Getter
    private LuceneOpenMode openMode = LuceneOpenMode.CREATE_OR_UPDATE;

    private Analyzer analyzer = DEFAULT_ANALYZER;

    protected IndexWriterConfig writerConfig;
    protected IndexWriter writer;

    protected ReferenceManager<IndexSearcher> searcherManager;

    private final Object lock = new Object();


    public LuceneIndex(@NonNull Supplier<Directory> directorySupplier) {
        this(null, directorySupplier, null);
    }

    public LuceneIndex(String name, @NonNull Supplier<Directory> directorySupplier) {
        this(name, directorySupplier, null);
    }

    public LuceneIndex(@NonNull Supplier<Directory> directorySupplier, IndexWriterConfigSupplier indexWriterConfigSupplier) {
        this(null, directorySupplier, indexWriterConfigSupplier);
    }

    public LuceneIndex(String name, @NonNull Supplier<Directory> directorySupplier, IndexWriterConfigSupplier indexWriterConfigSupplier) {
        this.name = name;
        this.directorySupplier = directorySupplier;
        this.indexWriterConfigSupplier = indexWriterConfigSupplier;
    }

    public LuceneIndex() {
        this(new RAMDirectory());
        owningDirectory = true;
    }

    public LuceneIndex(@NonNull Path path) throws IOException {
        this(path, LuceneOpenMode.CREATE_OR_UPDATE);
    }

    public LuceneIndex(@NonNull Path path, @NonNull LuceneOpenMode openMode) throws IOException {
        this(FSDirectory.open(path));
        setOpenMode0(openMode);
        owningDirectory = true;
    }

    public LuceneIndex(@NonNull Directory directory) {
        this.directory = directory;
    }

    public boolean hasName() {
        return name != null;
    }

    @Override
    public boolean isEmpty() throws IOException {
        checkOpenState();
        if (directory.listAll().length == 0) return true;

        IndexSearcher searcher = acquireSearcher();
        try {
            return searcher.getIndexReader().numDocs() == 0;
        } finally {
            release(searcher);
        }
    }

    public void setOpenMode(LuceneOpenMode openMode) {
        if (isOpen()) {
            throw new IllegalStateException("Could not change openMode while index is opened");
        }
        setOpenMode0(openMode);
    }

    protected void setOpenMode0(LuceneOpenMode mode) {
        this.openMode = mode;

        if (mode == LuceneOpenMode.CREATE) getWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        if (mode == LuceneOpenMode.UPDATE) getWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        if (mode == LuceneOpenMode.CREATE_OR_UPDATE) getWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    public boolean isReadOnly() {
        return openMode == LuceneOpenMode.READ_ONLY;
    }

    @Override
    public Analyzer getAnalyzer() {
        if (writerConfig != null) {
            return writerConfig.getAnalyzer();
        }
        return analyzer;
    }

    @Override
    public void setAnalyzer(@NonNull Analyzer analyzer) {
        if (writerConfig != null) {
            throw new IllegalStateException("Writer configuration is already instantiated");
        }
        this.analyzer = analyzer;
    }

    public IndexWriterConfig getWriterConfig() {
        if (writerConfig == null) {
            synchronized (lock) {
                if (writerConfig == null) {
                    if (indexWriterConfigSupplier != null) {
                        writerConfig = indexWriterConfigSupplier.get(analyzer);
                    } else {
                        writerConfig = new IndexWriterConfig(analyzer);
                    }
                }
            }
        }
        return writerConfig;
    }

    public void setWriterConfig(@NonNull IndexWriterConfig writerConfig) {
        this.writerConfig = writerConfig;
    }

    public Reference<IndexWriter> provideWriter() {
        return new Reference<IndexWriter>() {
            IndexWriter indexWriter;

            @Override
            public IndexWriter use() throws IOException {
                if (indexWriter == null) {
                    indexWriter = acquireWriter();
                }
                return indexWriter;
            }

            @Override
            public void close() throws IOException {
                if (indexWriter != null) {
                    release(indexWriter);
                }
            }
        };
    }

    @SneakyThrows
    public IndexWriter acquireWriter() throws IOException {
        if (writer == null) {
            synchronized (lock) {
                if (writer == null) {
                    if (isReadOnly()) {
                        throw new IllegalStateException("Index is opened in read-only openMode");
                    }
                    checkOpenState();
                    writer = new IndexWriter(directory, getWriterConfig());
                }
            }
        }

        return writer;
    }

    public void release(IndexWriter writer) throws IOException {
        if (writer != null) {
            try {
                if (autoFlush) {
                    writer.flush();
                }
                if (autoCommit) {
                    writer.commit();
                }
            } catch (Exception e) {
                this.writer = null;
            }

            if (this.writer != null && this.writer.isOpen()) {
                if (searcherManager != null) {
                    searcherManager.maybeRefreshBlocking();
                }
            } else {
                synchronized (lock) {
                    this.writer = null;
                    if (searcherManager != null) {
                        searcherManager.close();
                        searcherManager = null;
                    }
                }
            }
        }
    }

    public Reference<DirectoryReader> provideReader() {
        return new Reference<DirectoryReader>() {
            DirectoryReader indexReader;

            @Override
            public DirectoryReader use() throws IOException {
                if (indexReader == null) {
                    indexReader = acquireReader();
                }
                return indexReader;
            }

            @Override
            public void close() throws IOException {
                if (indexReader != null) {
                    release(indexReader);
                }
            }
        };
    }

    public DirectoryReader acquireReader() throws IOException {
        checkOpenState();
        if (isReadOnly()) {
            return DirectoryReader.open(directory);
        } else {
            return DirectoryReader.open(acquireWriter());
        }
    }

    public void release(IndexReader reader) throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    public Reference<IndexSearcher> provideSearcher() {
        return new Reference<IndexSearcher>() {
            IndexSearcher indexSearcher;

            @Override
            public IndexSearcher use() throws IOException {
                if (indexSearcher == null) {
                    indexSearcher = acquireSearcher();
                }
                return indexSearcher;
            }

            @Override
            public void close() throws IOException {
                if (indexSearcher != null) {
                    release(indexSearcher);
                }
            }
        };
    }

    public IndexSearcher acquireSearcher() throws IOException {
        checkOpenState();
        if (searcherManager == null) {
            synchronized (lock) {
                if (searcherManager == null) {
                    searcherManager = new SearcherManager(acquireReader(), null);
                }
            }
        }
        return searcherManager.acquire();
    }

    public void release(IndexSearcher searcher) throws IOException {
        if (searcher != null) {
            searcherManager.release(searcher);
        }
    }

    @Override
    public LuceneSearchResults search(LuceneSearch search) throws IOException {
        TopDocs topDocs;
        IndexSearcher searcher = acquireSearcher();
        Query query = search.getQuery() != null ? search.getQuery() : LuceneSearch.MATCH_ALL_QUERY;
        int numHits = (search.getNumHits() != null && search.getNumHits() > 0) ? search.getNumHits() : Integer.MAX_VALUE;
        if (search.getSort() != null) {
            topDocs = searcher.search(query, numHits, search.getSort(), search.isDoDocScore(), search.isDoMaxScore());
        } else {
            topDocs = searcher.search(query, numHits);
        }
        return new SingleLuceneSearchResults(topDocs, searcher, this);
    }

    public void addDocument(Iterable<IndexableField> doc) throws IOException {
        try (Reference<IndexWriter> writer = provideWriter()) {
            writer.use().addDocument(doc);
        }
    }

    public void updateDocument(Term term, Iterable<IndexableField> doc) throws IOException {
        try (Reference<IndexWriter> writer = provideWriter()) {
            writer.use().updateDocument(term, doc);
        }
    }

    @Override
    public void optimize() throws IOException {
        synchronized (lock) {
            IndexWriter writer = acquireWriter();
            writer.forceMerge(1, true);
            writer.forceMergeDeletes(true);
            writer.deleteUnusedFiles();
            release(writer);
        }
    }

    @Override
    public void commit() throws IOException {
        try (val writer = provideWriter()) {
            writer.use().commit();
        }
    }

    @Override
    public void flush() throws IOException {
        try (val writer = provideWriter()) {
            writer.use().flush();
        }
    }

    @Override
    public void clear() throws IOException {
        try (val writer = provideWriter()) {
            writer.use().deleteAll();
        }
    }

    public void open() {
        synchronized (lock) {
            if (directory == null) {
                if (directorySupplier == null) {
                    throw new IllegalStateException("Could not open index as directory supplier is not provided");
                }
                directory = directorySupplier.get();
                owningDirectory = true;
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (isOpen()) {
                flush();
            }

            if (searcherManager != null) {
                searcherManager.close();
                searcherManager = null;
            }
            if (writerConfig != null) {
                writerConfig = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (owningDirectory) {
                if (directory != null) {
                    directory.close();
                    directory = null;
                }
            }
        }
    }

    public boolean isOpen() {
        return directory != null;
    }

    protected void checkOpenState() {
        if (!isOpen()) {
            synchronized (lock) {
                if (!isOpen()) {
                    if (hasName()) {
                        throw new IllegalStateException("Index \"" + getName() + "\" is closed");
                    } else {
                        throw new IllegalStateException("Index is closed");
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } catch (Throwable ignore) { }
        super.finalize();
    }

    @Override
    public String toString() {
        if (name != null) {
            return name;
        }
        return super.toString();
    }
}

