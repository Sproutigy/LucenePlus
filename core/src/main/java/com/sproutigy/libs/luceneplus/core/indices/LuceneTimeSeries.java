package com.sproutigy.libs.luceneplus.core.indices;

import com.sproutigy.libs.luceneplus.core.LuceneIndex;
import com.sproutigy.libs.luceneplus.core.Reference;
import lombok.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RequiredArgsConstructor
public class LuceneTimeSeries {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    static {
        DATE_FORMAT.setTimeZone(UTC);
    }

    public enum Resolution {
        SECOND(1000L, 14),
        MINUTE(60 * 1000L, 12),
        HOUR(60 * 60 * 1000L, 10),
        DAY(24 * 60 * 60 * 1000L, 8)
        ;

        Resolution(long durationMilliseconds, int substringLength) {
            this.durationMilliseconds = durationMilliseconds;
            this.substringLength = substringLength;
        }

        @Getter
        private long durationMilliseconds;

        int substringLength;
    }

    @Data
    @AllArgsConstructor
    public static class TimeRange {

        @NonNull
        @Getter
        private long startAsTimestamp;

        @NonNull
        @Getter
        private long endAsTimestamp;

        public long getDurationMillis() {
            return endAsTimestamp - startAsTimestamp;
        }

        public boolean matches(long timestamp) {
            return (startAsTimestamp >= timestamp && endAsTimestamp <= timestamp);
        }
    }


    @NonNull
    private LuceneIndices luceneIndices;

    @NonNull @Getter
    private String prefix;

    @NonNull @Getter @Setter
    private Resolution resolution = Resolution.DAY;


    public LuceneTimeSeries(LuceneIndices luceneIndices) {
        this(luceneIndices, "");
    }

    public LuceneTimeSeries(LuceneIndices luceneIndices, String prefix, Resolution resolution) {
        this(luceneIndices, prefix);
        setResolution(resolution);
    }

    public String indexName(long time) {
        synchronized (DATE_FORMAT) {
            return prefix + DATE_FORMAT.format(new Date(time)).substring(0, resolution.substringLength);
        }
    }

    public Reference<LuceneIndex> index(long time) throws IOException {
        return luceneIndices.provide(indexName(time));
    }

    public String[] indicesNames() throws IOException {
        return indicesNames(null, null);
    }

    public String[] indicesNames(Long from, Long to) throws IOException {
        if (from != null && to != null && to < from) {
            return indicesNames(to, from, true);
        } else {
            return indicesNames(from, to, false);
        }
    }

    public String[] indicesNames(Long from, Long to, boolean reverse) throws IOException {
        Collection<String> selectedCollection = new LinkedList<>();
        if (from != null && to != null) {
            selectedCollection = new LinkedList<>();
            long current = from;
            while (truncateTime(current, resolution) <= to) {
                String s = indexName(current);
                if (luceneIndices.exists(s, true)) {
                    selectedCollection.add(s);
                }
                current += resolution.durationMilliseconds;
            }
        }
        else {
            for (String name : luceneIndices.names(prefix, true)) {
                boolean ok = true;
                if (from != null || to != null) {
                    TimeRange timeRange = rangeOf(name);
                    if (timeRange == null) continue;

                    if (from != null) {
                        ok = timeRange.getEndAsTimestamp() > from;
                    }
                    if (ok && to != null) {
                        ok = timeRange.getStartAsTimestamp() < to;
                    }
                }
                if (ok) {
                    selectedCollection.add(name);
                }
            }
        }

        String[] selected = selectedCollection.toArray(new String[selectedCollection.size()]);
        if (!reverse) {
            Arrays.sort(selected);
        } else {
            Arrays.sort(selected, Collections.reverseOrder());
        }

        return selected;
    }

    public TimeRange rangeOf(String name) {
        StringBuilder timeString = new StringBuilder(name.substring(prefix.length()));
        while (timeString.length() < 14) {
            timeString.append("00");
        }
        try {
            long time;
            synchronized (DATE_FORMAT) {
                time = DATE_FORMAT.parse(timeString.toString()).getTime();
            }

            long start = truncateTime(time, resolution);
            long end = nextTime(time, resolution) - 1;
            return new TimeRange(start, end);
        } catch (ParseException ignore) {
            return null;
        }
    }

    private static long truncateTime(long time, Resolution resolution) {
        long t = time;
        final Calendar cal = Calendar.getInstance(UTC);
        cal.setTime(new Date(t));
        final int millis = cal.get(Calendar.MILLISECOND);
        t -= millis;
        if (resolution != Resolution.SECOND) {
            final int seconds = cal.get(Calendar.SECOND);
            t -= seconds * 1000L;
            if (resolution != Resolution.MINUTE) {
                final int minutes = cal.get(Calendar.MINUTE);
                t -= minutes * 60 * 1000L;
                if (resolution != Resolution.HOUR) {
                    final int hours = cal.get(Calendar.HOUR_OF_DAY);
                    t -= hours * 60 * 60 * 1000L;
                }
            }
        }
        return t;
    }

    private static long nextTime(long time, Resolution resolution) {
        return truncateTime(time, resolution) + resolution.durationMilliseconds;
    }
}
