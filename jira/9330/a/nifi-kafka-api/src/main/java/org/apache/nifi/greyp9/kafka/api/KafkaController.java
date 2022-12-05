package org.apache.nifi.greyp9.kafka.api;

import org.apache.nifi.greyp9.kafka.api.consumer.NifiConsumerRecord;
import org.apache.nifi.greyp9.kafka.api.producer.NifiProducerRecord;

public interface KafkaController<K, V> {

    //NifiProducer<K, V> getProducer();

    void publishValue(V value, String topic);

    void publishRecord(NifiProducerRecord<K, V> record);

    V consumeValue(String topic);

    NifiConsumerRecord<K, V> consumeRecord(String topic);
}
