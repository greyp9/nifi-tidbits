package org.apache.nifi.greyp9.kafka.api.consumer;

import org.apache.nifi.greyp9.kafka.api.common.Headers;

public class NifiConsumerRecord<K, V> {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;

    private final Headers headers;
    private final K key;
    private final V value;

    public NifiConsumerRecord(String topic, int partition, long offset, long timestamp, Headers headers, K key, V value) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Headers getHeaders() {
        return headers;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
