package com.sproutigy.libs.luceneplus.core;

import java.io.Closeable;
import java.io.IOException;

public interface Reference<T> extends Closeable {
    T use() throws IOException;
}
