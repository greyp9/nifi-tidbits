package org.apache.nifi.greyp9.kafka30;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.apache.nifi.greyp9.kafka.api.common.Headers;
import org.apache.nifi.greyp9.kafka.api.common.RecordHeaders;
import org.apache.nifi.greyp9.kafka.api.consumer.NifiConsumerRecord;
import org.apache.nifi.greyp9.kafka.api.producer.NifiProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@SuppressWarnings("unused")
public class KafkaController30<K, V> implements KafkaController<K, V> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Properties properties;

    public KafkaController30(final Properties properties) {
        this.properties = properties;
    }

    //public NifiProducer<K, V> getProducer() {
    //    return new NifiProducer<>();
    //}

    @Override
    public void publishValue(final V value, final String topic) {
        try (final KafkaProducer<K, V> producer = new KafkaProducer<>(properties)) {
            logger.info("publish to topic={}, count={}", topic, 1);
            final ProducerRecord<K, V> record = new ProducerRecord<>(topic, null, value);
            final Future<RecordMetadata> future = producer.send(record);
            final RecordMetadata metadata = future.get();
            logger.info("RecordMetadata={}", metadata);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V consumeValue(final String topic) {
        try (final KafkaConsumer<K, V> consumer = new KafkaConsumer<>(properties)) {
            logger.info("class={}", consumer.getClass().getProtectionDomain().getCodeSource().getLocation());
            logger.info("consume from topic={}, count={}", topic, 1);
            consumer.subscribe(Collections.singletonList(topic));
            final Collection<ConsumerRecord<K, V>> records = queryRecords(consumer, 10);
            consumer.unsubscribe();
            if (records.isEmpty()) {
                return null;
            } else {
                final ConsumerRecord<K, V> record = records.iterator().next();
                return record.value();
            }
        }
    }

    @Override
    public void publishRecord(final NifiProducerRecord<K, V> record) {
        // transform version-agnostic record to version-specific record
        final ProducerRecord<K, V> producerRecord = new ProducerRecord<>(
                record.getTopic(), record.getPartition(), record.getTimestamp(), record.getKey(), record.getValue());
        logger.info("KEY={}, VALUE={}", producerRecord.key(), producerRecord.value());
        record.getHeaders().forEach(h -> producerRecord.headers().add(new RecordHeader(h.key(), h.value())));
        // publish
        try (final KafkaProducer<K, V> producer = new KafkaProducer<>(properties)) {
            logger.info("publish to topic={}, count={}", producerRecord.topic(), 1);
            final Future<RecordMetadata> future = producer.send(producerRecord);
            final RecordMetadata metadata = future.get();
            logger.info("RecordMetadata={}", metadata);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NifiConsumerRecord<K, V> consumeRecord(final String topic) {
        try (final KafkaConsumer<K, V> consumer = new KafkaConsumer<>(properties)) {
            logger.info("class={}", consumer.getClass().getProtectionDomain().getCodeSource().getLocation());
            logger.info("consume from topic={}, count={}", topic, 1);
            consumer.subscribe(Collections.singletonList(topic));
            final Collection<ConsumerRecord<K, V>> records = queryRecords(consumer, 10);
            consumer.unsubscribe();
            if (records.isEmpty()) {
                return null;
            } else {
                final ConsumerRecord<K, V> record = records.iterator().next();
                final Headers nifiHeaders = new RecordHeaders();
                record.headers().forEach(h -> nifiHeaders.add(
                        new org.apache.nifi.greyp9.kafka.api.common.RecordHeader(h.key(), h.value())));
                logger.info("KEY={}, VALUE={}", record.key(), record.value());
                // transform version-agnostic record to version-specific record
                return new NifiConsumerRecord<>(
                        record.topic(), record.partition(), record.offset(), record.timestamp(),
                        nifiHeaders, record.key(), record.value());
            }
        }
    }

    private Collection<ConsumerRecord<K, V>> queryRecords(
            final KafkaConsumer<K, V> consumer, final int iterations) {
        final Collection<ConsumerRecord<K, V>> records = new ArrayList<>();
        for (int i = 0; (i < iterations); ++i) {
            final ConsumerRecords<K, V> recordsIt = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<K, V> record : recordsIt) {
                records.add(record);
                logger.info("RECORD @ {}/{} {}={}", record.partition(), record.offset(), record.key(), record.value());
            }
        }
        return records;
    }
}
