package com.sproutigy.libs.luceneplus.core;

import lombok.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;

public class LuceneFields {
    private LuceneFields() { }

    @Data
    @Builder
    public static class FieldOptions {
        public static FieldOptions STORE = FieldOptions.builder().store(true).build();
        public static FieldOptions INDEX = FieldOptions.builder().index(true).build();
        public static FieldOptions DOCVALUE = FieldOptions.builder().docValue(true).build();
        public static FieldOptions STORE_INDEX = FieldOptions.builder().index(true).store(true).build();
        public static FieldOptions INDEX_DOCVALUE = FieldOptions.builder().index(true).docValue(true).build();
        public static FieldOptions STORE_DOCVALUE = FieldOptions.builder().store(true).docValue(true).build();
        public static FieldOptions STORE_INDEX_DOCVALUE = FieldOptions.builder().index(true).store(true).docValue(true).build();

        @Getter
        @Builder.Default
        boolean store = false;
        @Getter
        @Builder.Default
        boolean index = false;
        @Getter
        @Builder.Default
        boolean docValue = false;
    }


    public static final class Binary {
        private Binary() { }

        public static Field create(@NonNull String name, byte[] data) {
            if (data != null) {
                return new StoredField(name, data);
            }
            return new StoredField(name, new byte[0]);
        }

        public static Field add(@NonNull Document doc, @NonNull String name, byte[] data) {
            Field field = null;
            if (data != null) {
                field = create(name, data);
                doc.add(field);
            }
            return field;
        }

        public static byte[] get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getBinaryValue(name));
        }

        public static byte[] get(IndexableField field) {
            if (field != null) {
                return get(field.binaryValue());
            }
            return null;
        }

        public static byte[] get(BytesRef bytesRef) {
            if (bytesRef != null) {
                if (bytesRef.offset > 0 || bytesRef.length < bytesRef.bytes.length) {
                    byte[] ret = new byte[bytesRef.length];
                    System.arraycopy(bytesRef.bytes, bytesRef.offset, ret, 0, bytesRef.length);
                    return ret;
                }
                return bytesRef.bytes;
            }
            return null;
        }
    }


    public static final class Boolean {
        public static final String TRUE = "T";
        public static final String FALSE = "F";

        private Boolean() { }

        public static Field create(String name, java.lang.Boolean value, @NonNull FieldOptions options) {
            return createKeyword(name, value ? "T" : "F", options);
        }

        public static Field add(Document doc, String name, java.lang.Boolean value, @NonNull FieldOptions options) {
            Field field = create(name, value, options);
            if (field != null) {
                doc.add(field);
            }
            return null;
        }

        public static java.lang.Boolean get(Document doc, String name) {
            return get(doc.getField(name));
        }

        public static java.lang.Boolean get(IndexableField field) {
            if (field != null) {
                String s = field.stringValue();
                if (TRUE.equals(s)) return true;
                if (FALSE.equals(s)) return false;
            }
            return null;
        }
    }


    public static final class Double {
        private Double() { }

        public static void add(@NonNull Document doc, @NonNull String name, java.lang.Double value, @NonNull FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    if (LuceneVersionDetect.isLucene6()) {
                        doc.add(new DoublePoint(name, value));
                    } else {
                        doc.add(createLegacyLuceneField("org.apache.lucene.document.DoubleField", name, java.lang.Double.TYPE, value, options.isStore()));
                    }
                }
                if (options.isDocValue()) {
                    doc.add(new DoubleDocValuesField(name, value));
                }
                if (options.isStore() && (LuceneVersionDetect.isLucene6() || !options.isIndex())) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static java.lang.Double get(Document doc, String name) {
            return get(doc.getField(name));
        }

        public static java.lang.Double get(IndexableField field) {
            return field != null && field.numericValue() != null ? field.numericValue().doubleValue() : null;
        }
    }


    public static final class Float {
        private Float() { }

        @SneakyThrows
        public static void add(@NonNull Document doc, @NonNull String name, java.lang.Float value, FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    if (LuceneVersionDetect.isLucene6()) {
                        doc.add(new FloatPoint(name, value));
                    } else {
                        doc.add(createLegacyLuceneField("org.apache.lucene.document.FloatField", name, java.lang.Long.TYPE, value, options.isStore()));
                    }
                }
                if (options.isDocValue()) {
                    doc.add(new FloatDocValuesField(name, value));
                }
                if (options.isStore() && (LuceneVersionDetect.isLucene6() || !options.isIndex())) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static java.lang.Float get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getField(name));
        }

        public static java.lang.Float get(IndexableField field) {
            return field != null && field.numericValue() != null ? field.numericValue().floatValue() : null;
        }
    }


    public static final class Integer {
        private Integer() { }

        @SneakyThrows
        public static void add(@NonNull Document doc, @NonNull String name, java.lang.Integer value, FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    if (LuceneVersionDetect.isLucene6()) {
                        doc.add(new IntPoint(name, value));
                    } else {
                        doc.add(createLegacyLuceneField("org.apache.lucene.document.IntField", name, java.lang.Long.TYPE, value, options.isStore()));
                    }
                }
                if (options.isDocValue()) {
                    doc.add(new FloatDocValuesField(name, value));
                }
                if (options.isStore() && (LuceneVersionDetect.isLucene6() || !options.isIndex())) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static java.lang.Integer get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getField(name));
        }

        public static java.lang.Integer get(IndexableField field) {
            return field != null && field.numericValue() != null ? field.numericValue().intValue() : null;
        }
    }


    public static final class Long {
        private Long() { }

        @SneakyThrows
        public static void add(@NonNull Document doc, @NonNull String name, java.lang.Long value, FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    if (LuceneVersionDetect.isLucene6()) {
                        doc.add(new LongPoint(name, value));
                    } else {
                        doc.add(createLegacyLuceneField("org.apache.lucene.document.LongField", name, java.lang.Long.TYPE, value, options.isStore()));
                    }
                }
                if (options.isDocValue()) {
                    doc.add(new NumericDocValuesField(name, value));
                }
                if (options.isStore() && (LuceneVersionDetect.isLucene6() || !options.isIndex())) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static java.lang.Long get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getField(name));
        }

        public static java.lang.Long get(IndexableField field) {
            return field != null && field.numericValue() != null ? field.numericValue().longValue() : null;
        }
    }


    public static final class Keyword {
        private Keyword() { }

        @SneakyThrows
        public static void add(@NonNull Document doc, @NonNull String name, String value, FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    doc.add(createKeyword(name, value, options));
                }
                if (options.isDocValue()) {
                    doc.add(new BinaryDocValuesField(name, new BytesRef(value)));
                }
                if (!options.isIndex() && options.isStore()) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static String get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getField(name));
        }

        public static String get(IndexableField field) {
            return field != null ? field.stringValue() : null;
        }
    }


    public static final class Text {
        private Text() { }

        @SneakyThrows
        public static void add(@NonNull Document doc, @NonNull String name, String value, FieldOptions options) {
            if (value != null) {
                if (options.isIndex()) {
                    doc.add(new TextField(name, value, options.isStore() ? Field.Store.YES : Field.Store.NO));
                }
                if (options.isDocValue()) {
                    doc.add(new BinaryDocValuesField(name, new BytesRef(value)));
                }
                if (!options.isIndex() && options.isStore()) {
                    doc.add(new StoredField(name, value));
                }
            }
        }

        public static String get(@NonNull Document doc, @NonNull String name) {
            return get(doc.getField(name));
        }

        public static String get(IndexableField field) {
            return field != null ? field.stringValue() : null;
        }
    }


    private static Field createString(@NonNull java.lang.String name, java.lang.String value, @NonNull FieldType fieldType) {
        Field field = null;
        if (value != null) {
            field = new Field(name, value, fieldType);
        }
        return field;
    }

    private static Field createKeyword(@NonNull String name, String value, FieldOptions options) {
        if (value != null) {
            FieldType fieldType = new FieldType();
            fieldType.setTokenized(false);
            fieldType.setOmitNorms(true);
            fieldType.setStored(options.isStore());
            fieldType.setIndexOptions(options.isIndex() ? IndexOptions.DOCS : IndexOptions.NONE);
            return createString(name, value, fieldType);
        }
        return null;
    }


    public static void addNumber(@NonNull Document doc, @NonNull String name, Number value, @NonNull FieldOptions options) {
        if (value != null) {
            if (value instanceof BigDecimal || value instanceof java.lang.Double || value instanceof java.lang.Float || value.toString().contains(".")) {
                if (value instanceof java.lang.Double || value instanceof java.lang.Float) {
                    Double.add(doc, name, value.doubleValue(), options);
                }
                else {
                    Double.add(doc, name, value.doubleValue(), FieldOptions.builder().index(options.isIndex()).store(false).docValue(options.isDocValue()).build());
                    if (options.isStore()) {
                        String s = value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value.toString();
                        doc.add(new StoredField(name, s));
                    }
                }
            }
            else {
                if (value instanceof BigInteger) {
                    if (
                            ((BigInteger) value).compareTo(new BigInteger(java.lang.Long.toString(java.lang.Long.MAX_VALUE))) > 0
                            ||
                            ((BigInteger) value).compareTo(new BigInteger(java.lang.Long.toString(java.lang.Long.MIN_VALUE))) < 0
                        )
                    {
                        if (options.isIndex() || options.isDocValue()) {
                            throw new IllegalArgumentException("Unsupported value");
                        }
                        doc.add(new StoredField(name, value.toString()));
                    }
                }
                else {
                    Long.add(doc, name, value.longValue(), options);
                }
            }
        }
    }

    public static Number getNumber(@NonNull Document doc, @NonNull String name) {
        return getNumber(doc.getField(name));
    }

    public static Number getNumber(IndexableField field) {
        return field != null ? field.numericValue() : null;
    }



    @SneakyThrows
    private static IndexableField createLegacyLuceneField(String className, String name, Class<?> valueClass, Object value, boolean store) {
        Class<?> clazz = Class.forName(className);
        Constructor<?> cstr = clazz.getDeclaredConstructor(String.class, valueClass, Field.Store.class);
        return (IndexableField)cstr.newInstance(name, value, store ? Field.Store.YES : Field.Store.NO);
    }
}
