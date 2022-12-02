package org.apache.nifi.greyp9.kafka30;

import org.apache.kafka.clients.admin.AdminClientConfig;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaInteractTest {
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

    private static final int EVENT_COUNT = new Random().nextInt(5);

    @Test
    public void test_0_CheckVersion() {
        final URL codeLocation = AdminClientConfig.class.getProtectionDomain().getCodeSource().getLocation();
        logger.info(codeLocation.toExternalForm());
        assertTrue(codeLocation.toExternalForm().contains("kafka-clients-3.0"));
    }

    @Test
    public void test_1_ProduceMessage() throws ExecutionException, InterruptedException {
        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            logger.info("publish to topic={}, count={}", TOPIC, EVENT_COUNT);
            for (int i = 0; (i < EVENT_COUNT); ++i) {
                final ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "mykey",
                        ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                final Future<RecordMetadata> future = producer.send(record);
                final RecordMetadata metadata = future.get();
                logger.info("RecordMetadata={}", metadata);
            }
        }
    }

    @Test
    public void test_2_ConsumeMessage() {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            logger.info("consume from topic={}, count={}", TOPIC, EVENT_COUNT);
            consumer.subscribe(Collections.singletonList(TOPIC));
            final Collection<ConsumerRecord<String, String>> records = queryRecords(consumer, 10);
            assertEquals(EVENT_COUNT, records.size());
            for (final ConsumerRecord<String, String> record : records) {
                assertEquals("mykey", record.key());
            }
            consumer.unsubscribe();
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
