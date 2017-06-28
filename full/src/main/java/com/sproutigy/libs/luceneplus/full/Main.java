package com.sproutigy.libs.luceneplus.full;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;

import java.io.IOException;

/**
 * Simple meaningless class just to ensure proper building
 */
public class Main {
    public static void main(String[] args) throws IOException {
        LuceneIndex index = new LuceneIndex();
        index.close();
    }
}
