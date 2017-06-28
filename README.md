# LucenePlus
*LucenePlus* is a Java library that simplifies and enhances the usage of [Apache Lucene](http://lucene.apache.org/core/) - leading search engine technology. 
It handles index readers, searches, writers correctly, preventing memory leaks and unneeded index grow.
By default is setup to use Near Real Time (NRT) search feature.
Plus it adds ability to manage multiple indices with optional time series support.  


## Lucene version
Currently supported Lucene version is: *6.6.0*. 

It may be compatible with any 5.x and 6.x Lucene version. See "Java 7 and Android compatibility" section for more details.


## Usage (API)

### Index creation or opening:
Memory index:
```java
LuceneIndex index = new LuceneIndex();
```

File System index:
```java
LuceneIndex index = new LuceneIndex(Paths.get("index_data_dir"));
```

### Write
To acquire `IndexWriter`, `index.provideWriter()` may be called, that returns writer wrapped in `Closeable` reference to release it:
```java
try (Reference<IndexWriter> writer = index.provideWriter()) {
    Document doc = new Document();
    LuceneFields.Long.add(doc, "id", (long)1, LuceneFields.FieldOptions.STORE_INDEX);
    LuceneFields.Keyword.add(doc, "name", "John", LuceneFields.FieldOptions.STORE_INDEX);
    LuceneFields.Text.add(doc, "motto", "Hello World!", LuceneFields.FieldOptions.STORE_INDEX);
    LuceneFields.Double.add(doc, "age", 30.5d, LuceneFields.FieldOptions.STORE_INDEX_DOCVALUE);
    writer.use().addDocument(doc);
}
```

`IndexWriter` acquisition and releasing may be done explicitly:
```java
IndexWriter writer = index.acquireWriter();
Document doc = new Document();
LuceneFields.Long.add(doc, "id", (long)1, LuceneFields.FieldOptions.STORE_INDEX);
LuceneFields.Keyword.add(doc, "name", "John", LuceneFields.FieldOptions.STORE_INDEX);
LuceneFields.Text.add(doc, "motto", "Hello World!", LuceneFields.FieldOptions.STORE_INDEX);
LuceneFields.Double.add(doc, "age", 30.5d, LuceneFields.FieldOptions.STORE_INDEX_DOCVALUE);
writer.addDocument(doc);
index.release(writer);
```


### Search
Similarily to writer, `IndexSearcher` may be provided as releasable reference:
```java
try (Reference<IndexSearcher> search = index.provideSearcher()) {
    searcher.use(). //TODO search
}
```
or explicitly acquired and released:
```java
IndexSearcher searcher = index.acquireSearcher();
searcher. //TODO search
searcher.release(searcher);
```

### Simplified Search
Using `IndexSearcher` directly may be difficult. For most typical searches LucenePlus provides own additional search API:
```java
LuceneSearch search = LuceneSearch.builder()
    .query(new TermQuery(new Term("name", "John")))
    .sort(new Sort(new SortField("age", SortField.Type.DOUBLE)))
    .build();

LuceneSearchResults results = index.search(search);

for (LuceneSearchHit hit : results) {
    Document doc = hit.getDocument();
    long id = LuceneFields.Long.get(doc, "id");
    System.out.println("document #" + id);
}
```

### Analyzer
By default *LucenePlus* uses Lucene's `StandardAnalyzer`. It can be changed to use custom analyzer:
```java
index.setAnalyzer(myAnalyzer);
```
It must be done before first use of index writer.

### Indices
`LuceneIndices` is management interface for multiple index instances.

#### Indices creation or opening
Memory indices:
```java
LuceneIndices indices = new MemoryLuceneIndices();
```

File System indices:
```java
LuceneIndices indices = new FSLuceneIndices(Paths.get("indices_root_dir"));
```
File System indices directory is a root directory for index-specific directories. This directory should be available only for managing indices, do not put other directories or files there. 

#### Specific index from Indices
Provide releasable reference:
```java
try (Reference<LuceneIndex> index = indices.provide("myindex")) {
    index.use(). //TODO    
}
```
or acquire and release explicitly:
```java
LuceneIndex index = indices.acquire("myindex");
indices.release(index);
```

#### Index names
To fetch all index names call `names()` method:
```
Collection<String> names = indices.names();
```

#### Indices API
Indices share same API with single index when possible, e.g. `setAutoCommit()`, `setAnalyzer()`, `search()`, `commit()`, `clear()`...
There are some specific methods to help manage multiple indices, e.g. `exists(name)`, `isOpen(name)`, `iterator(prefix)`,`names()`, `names(prefix)`, `search(search, names)`, `close(name)`, `delete(name)`, `getOpenIndices()`...

#### Searching
Simple search API allows to search through all indices in similar way as in single index.
Use `search(LuceneSearch search)` or `search(LuceneSearch search, String... names)` method.

#### Time Series indexing
Time series indexing feature allows to keep data in separate indices based on timestamp:
```java
LuceneTimeSeries timeSeries = new LuceneTimeSeries(indices, "mylogs", LuceneTimeSeries.Resolution.HOUR);
long timestamp = new Date().getTime();
String indexName = timeSeries.indexName(timestamp);
```
Supported resolutions are: `DAY`, `HOUR`, `MINUTE`, `SECOND`.

For searching in a specific time-range use `indicesNames()`:
```java
long t1 = new Date().getTime() - 1000;
long t2 = new Date().getTime();
String[] names = timeSeries.indicesNames(t1, t2);
LuceneSearch search = LuceneSearch.builder(). ... .build() //TODO
LuceneSearchResults results = indices.search(search, names);
```

#### Auto close
When using multiple indices, especially when dealing with time series, some opened indices may not be needed but are left opened, consuming memory and file handles. 
It is recommended to close index when it is not needed for longer time. Auto close feature helps to ensure that.

```java
indices.setAutoCloseMillis(0L); // auto-close instantly
indices.setAutoCloseInstantly(); // same as above

indices.setAutoCloseMillis(3000L); // auto-close after 3 seconds
indices.setAutoClose(3, TimeUnit.SECONDS); // same as above

indices.setAutoCloseMillis(null); // disable auto-close
```
Be aware that when used on Memory indices, data will be lost after close. By default this feature is disabled.

## Additional Notes

### Thread safety
Excluding iterators, LucenePlus classes are thread-safe, except Lucene-specific ones (check Lucene documentation for details).

### Performance notes
By default indexing performance is low, because auto-commit feature is enabled to ensure you will not lost any data.
You can disable it by calling `setAutoCommit(false)` and do `commit()`  manually when need (e.g. after some time or after indexing bunch of data).  

### Java 7 and Android compatibility
This library is compatible with Java 7. Unfortunately last supported Lucene version for Java 7 was 5.x branch.
You have to manually change dependencies to older Lucene version in your project in order it:
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.lucene</groupId>
        <artifactId>lucene-core</artifactId>
        <version>5.5.4</version>
    </dependency>
    <dependency>
        <groupId>org.apache.lucene</groupId>
        <artifactId>lucene-analyzers-common</artifactId>
        <version>5.5.4</version>
    </dependency>
</dependencies>
```
Additional library `lucene-analyzer-commons` dependency must be available, because it contains implementation of `StandardAnalyzer` (which in Lucene 6 has been moved to the core).

Versions of all additionally used Lucene modules must also be changed. It means you should not use `luceneplus-full` artifact or replace versions of all modules specified in that artifact as dependencies.

### Distributed systems
`Lucene` and `LucenePlus` were designed to work on single computer and single JVM, as embedded indexing engine. If you need more, take a look at [Apache Solr](http://lucene.apache.org/solr/) and [ElasticSearch](https://www.elastic.co/products/elasticsearch).  


## Latest release  

### Maven
To use as a dependency add to your `pom.xml` into `<dependencies>` section:
```xml
<dependency>
    <groupId>com.sproutigy.libs.luceneplus</groupId>
    <artifactId>luceneplus-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Additional artifact `luceneplus-full` has been provided that aggregates all additional Lucene modules:
```xml
<dependency>
    <groupId>com.sproutigy.libs.luceneplus</groupId>
    <artifactId>luceneplus-full</artifactId>
    <version>1.0.0</version>
</dependency>
```
It is not recommended to use `luceneplus-full` in production as most of added modules probably would not be used, but it is good for starting playing with Lucene and testing its features.  

## More
For more information and commercial support visit [Sproutigy](http://www.sproutigy.com/opensource)