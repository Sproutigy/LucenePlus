package com.sproutigy.libs.luceneplus.core;

import com.sproutigy.libs.luceneplus.core.search.LuceneSearch;
import com.sproutigy.libs.luceneplus.core.search.LuceneSearchResults;
import org.apache.lucene.analysis.Analyzer;

import java.io.IOException;

public interface LuceneIndexOperations {
    void setOpenMode(LuceneOpenMode openMode);
    LuceneOpenMode getOpenMode();

    void setAutoCommit(boolean autoCommit);
    boolean isAutoCommit();

    void setAutoFlush(boolean autoFlush);
    boolean isAutoFlush();

    void setAnalyzer(Analyzer analyzer);
    Analyzer getAnalyzer();

    boolean isEmpty() throws IOException;

    LuceneSearchResults search(LuceneSearch search) throws IOException;
    void optimize() throws IOException;
    void commit() throws IOException;
    void flush() throws IOException;
    void clear() throws IOException;
}
