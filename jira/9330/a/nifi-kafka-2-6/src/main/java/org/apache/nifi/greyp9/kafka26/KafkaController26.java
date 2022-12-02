package org.apache.nifi.greyp9.kafka26;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaController26 implements KafkaController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The default Kafka endpoint.
     */
    private static final String BOOTSTRAP_SERVER = "localhost:9092";

    /**
     * Ensure fresh data for each test run.
     */
    private static final long TIMESTAMP = System.currentTimeMillis();

    /**
     * The name of the test kafka topic to be created.
     */
    private static final String TOPIC = "nifi-topic-" + TIMESTAMP;

    /**
     * The name of the test kafka group to use.
     */
    private static final String GROUP = "nifi-group-" + TIMESTAMP;

    @Override
    public void publishOne(String value) {
        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            logger.info("publish to topic={}, count={}", TOPIC, 1);
            final ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "mykey", value);
            final Future<RecordMetadata> future = producer.send(record);
            final RecordMetadata metadata = future.get();
            logger.info("RecordMetadata={}", metadata);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String consumeOne() {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            logger.info("class={}", consumer.getClass().getProtectionDomain().getCodeSource().getLocation());
            logger.info("consume from topic={}, count={}", TOPIC, 1);
            consumer.subscribe(Collections.singletonList(TOPIC));
            final Collection<ConsumerRecord<String, String>> records = queryRecords(consumer, 10);
            consumer.unsubscribe();
            if (records.isEmpty()) {
                return null;
            } else {
                final ConsumerRecord<String, String> record = records.iterator().next();
                return record.value();
            }
        }
    }

    private Collection<ConsumerRecord<String, String>> queryRecords(
            final KafkaConsumer<String, String> consumer, final int iterations) {
        final Collection<ConsumerRecord<String, String>> records = new ArrayList<>();
        for (int i = 0; (i < iterations); ++i) {
            final ConsumerRecords<String, String> recordsIt = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : recordsIt) {
                records.add(record);
                logger.info("RECORD @ {}/{} {}={}", record.partition(), record.offset(), record.key(), record.value());
            }
        }
        return records;
    }
}
