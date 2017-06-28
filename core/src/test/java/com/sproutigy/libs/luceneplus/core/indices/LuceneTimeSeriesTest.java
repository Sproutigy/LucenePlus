package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LuceneTimeSeriesTest {
    static final long TEST_TIMESTAMP = 1498685442000L;
    static final String TEST_PREFIX = "ts-test-";
    static final String TEST_INDEX_NAME = TEST_PREFIX + "20170628213042";

    @Test
    public void testRange() throws IOException {
        LuceneIndices indices = new MemoryLuceneIndices();
        LuceneTimeSeries luceneTimeSeries = new LuceneTimeSeries(indices, TEST_PREFIX, LuceneTimeSeries.Resolution.DAY);
        long t1 = 1501027200000L; //26.07.2017
        long t2 = 1501286400000L; //29.07.2017

        String[] indicesNames = luceneTimeSeries.indicesNames(t1, t2);
        assertEquals(0, indicesNames.length);

        String indexName = luceneTimeSeries.indexName(((t2 - t1) / 2) + t1);
        assertEquals(TEST_PREFIX + "20170727", indexName);
        Reference<LuceneIndex> refIndex = indices.provide(indexName);
        refIndex.use();
        refIndex.close();

        indicesNames = luceneTimeSeries.indicesNames(t1, t2);
        assertEquals(1, indicesNames.length);
        assertEquals(indexName, indicesNames[0]);
    }

    @Test
    public void testResolutionDay() {
        assertEquals(TEST_INDEX_NAME.substring(0, TEST_PREFIX.length() + 8), indexNameForResolution(LuceneTimeSeries.Resolution.DAY));
    }

    @Test
    public void testResolutionHour() {
        assertEquals(TEST_INDEX_NAME.substring(0, TEST_PREFIX.length() + 10), indexNameForResolution(LuceneTimeSeries.Resolution.HOUR));
    }

    @Test
    public void testResolutionMinute() {
        assertEquals(TEST_INDEX_NAME.substring(0, TEST_PREFIX.length() + 12), indexNameForResolution(LuceneTimeSeries.Resolution.MINUTE));
    }

    @Test
    public void testResolutionSecond() {
        assertEquals(TEST_INDEX_NAME.substring(0, TEST_PREFIX.length() + 14), indexNameForResolution(LuceneTimeSeries.Resolution.SECOND));
    }

    @SneakyThrows
    private String indexNameForResolution(LuceneTimeSeries.Resolution resolution) {
        LuceneIndices indices = new MemoryLuceneIndices();
        LuceneTimeSeries luceneTimeSeries = new LuceneTimeSeries(indices, TEST_PREFIX, resolution);
        String indexName = luceneTimeSeries.indexName(TEST_TIMESTAMP);
        try (Reference<LuceneIndex> index = luceneTimeSeries.index(TEST_TIMESTAMP)) {
            index.use().open();
            index.use().close();
        }
        assertTrue(indices.exists(indexName));
        return indexName;
    }
}
