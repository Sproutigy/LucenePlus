package com.sproutigy.libs.luceneplus.core.search;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractLuceneSearchResults implements LuceneSearchResults {

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public List<LuceneSearchHit> toList() throws IOException {
        List<LuceneSearchHit> items = new LinkedList<>();
        while (hasNext()) {
            LuceneSearchHit item = next();
            if (item instanceof LuceneSearchHitImpl) {
                ((LuceneSearchHitImpl)item).fetchDocument();
                ((LuceneSearchHitImpl)item).unlinkSearcher();
            }
            items.add(item);
        }
        return items;
    }
}
