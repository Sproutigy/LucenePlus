package com.sproutigy.libs.luceneplus.core.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

@Builder
@EqualsAndHashCode
@AllArgsConstructor
public final class LuceneSearch {

    public static final MatchAllDocsQuery MATCH_ALL_QUERY = new MatchAllDocsQuery();

    public static final LuceneSearch MATCH_ALL = builder().query(MATCH_ALL_QUERY).build();


    @Getter
    @Builder.Default
    private Query query = MATCH_ALL_QUERY;

    @Getter
    private Integer numHits;

    @Getter
    private Sort sort;

    @Getter
    private boolean doDocScore;

    @Getter
    private boolean doMaxScore;
}
