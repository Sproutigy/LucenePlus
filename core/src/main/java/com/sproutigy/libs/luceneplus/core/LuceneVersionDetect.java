package com.sproutigy.libs.luceneplus.core;

public final class LuceneVersionDetect {

    private LuceneVersionDetect() { }

    private static final boolean isLucene6;

    static {
        boolean someLucene6ClassFound = false;
        try {
            Class.forName("org.apache.lucene.document.LongPoint");
            someLucene6ClassFound = true;
        } catch (ClassNotFoundException e) { }
        isLucene6 = someLucene6ClassFound;
    }

    public static boolean isLucene6() {
        return isLucene6;
    }
}
