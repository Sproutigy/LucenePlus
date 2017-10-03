package com.sproutigy.libs.luceneplus.core.search;

import com.sproutigy.libs.luceneplus.core.CloseableIterator;

import java.io.IOException;
import java.util.List;

public interface LuceneSearchResults extends CloseableIterator<LuceneSearchHit> {
    boolean hasTotal();
    Long total();

    boolean hasCount();
    Integer count();

    /**
     * Loads entire results with whole documents into memory.
     * Not recommended for larger result sets, in such cases use iterator instead.
     * @return list of hits
     * @throws IOException
     */
    List<LuceneSearchHit> toList() throws IOException;
}
