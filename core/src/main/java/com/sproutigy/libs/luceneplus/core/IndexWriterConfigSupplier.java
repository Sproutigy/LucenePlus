package com.sproutigy.libs.luceneplus.core;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriterConfig;

public interface IndexWriterConfigSupplier {
    IndexWriterConfig get(Analyzer analyzer);
}
