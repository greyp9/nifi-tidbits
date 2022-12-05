package org.apache.nifi.greyp9.kafka.api.common;

public class RecordHeader implements Header {
    private final String key;
    private final byte[] value;

    public RecordHeader(final String key, final byte[] value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }
}
