package org.apache.nifi.greyp9.kafka.api.producer;

import org.apache.nifi.greyp9.kafka.api.common.Header;
import org.apache.nifi.greyp9.kafka.api.common.Headers;
import org.apache.nifi.greyp9.kafka.api.common.RecordHeaders;

import java.util.List;

public class NifiProducerRecord<K, V> {
    private final String topic;
    private final Integer partition;
    private final Headers headers;
    private final K key;
    private final V value;
    private final Long timestamp;

    public NifiProducerRecord(final K key, final V value, final List<Header> headers) {
        this.topic = null;
        this.partition = null;
        this.timestamp = null;
        this.key = key;
        this.value = value;
        this.headers = new RecordHeaders(headers);
    }

    public NifiProducerRecord(final String topic, final Integer partition, final Long timestamp, final K key, final V value, final List<Header> headers) {
        this.topic = topic;
        this.partition = partition;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
        this.headers = new RecordHeaders(headers);
    }

    public String getTopic() {
        return topic;
    }

    public Integer getPartition() {
        return partition;
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

    public Long getTimestamp() {
        return timestamp;
    }
}
