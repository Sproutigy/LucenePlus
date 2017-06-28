package com.sproutigy.libs.luceneplus.core;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<E> extends Iterator<E>, Closeable {
}
