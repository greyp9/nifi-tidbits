package org.apache.nifi.greyp9.kafka26;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.apache.nifi.greyp9.kafka.api.TestUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaInteractTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int EVENT_COUNT = new Random().nextInt(5);

    @Test
    public void test_0_CheckVersion() {
        final URL codeLocation = AdminClientConfig.class.getProtectionDomain().getCodeSource().getLocation();
        logger.info(codeLocation.toExternalForm());
        assertTrue(codeLocation.toExternalForm().contains("kafka-clients-2.6"));
    }

    @Test
    public void test_1_ProduceMessage() throws ExecutionException, InterruptedException {
        final String topic = TestUtils.getTestTopic();
        try (final KafkaProducer<String, String> producer = new KafkaProducer<>(TestUtils.getTestProducerProperties())) {
            logger.info("publish to topic={}, count={}", topic, EVENT_COUNT);
            for (int i = 0; (i < EVENT_COUNT); ++i) {
                final ProducerRecord<String, String> record = new ProducerRecord<>(topic, "mykey", topic);
                final Future<RecordMetadata> future = producer.send(record);
                final RecordMetadata metadata = future.get();
                logger.info("RecordMetadata={}", metadata);
            }
        }
    }

    @Test
    public void test_2_ConsumeMessage() {
        final String topic = TestUtils.getTestTopic();
        try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(TestUtils.getTestConsumerProperties())) {
            logger.info("consume from topic={}, count={}", topic, EVENT_COUNT);
            consumer.subscribe(Collections.singletonList(topic));
            final Collection<ConsumerRecord<String, String>> records = queryRecords(consumer, 10);
            assertEquals(EVENT_COUNT, records.size());
            for (final ConsumerRecord<String, String> record : records) {
                assertEquals("mykey", record.key());
                assertEquals(topic, record.value());
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

    @Test
    public void test_2_InstantiateController30Reflect() {
        final String className = "org.apache.nifi.greyp9.kafka30.KafkaController26";
        final KafkaController<String, String> kafkaControllerProducer =
                new KafkaController26<>(TestUtils.getTestProducerProperties());
        final String topic = TestUtils.getTestTopic() + "I";
        //final NifiProducer<String, String> producer = kafkaControllerProducer.getProducer();
        kafkaControllerProducer.publishValue(className, topic);

        final KafkaController<String, String> kafkaControllerConsumer =
                new KafkaController26<>(TestUtils.getTestConsumerProperties());
        logger.info(kafkaControllerConsumer.getClass().getName());
        final String value = kafkaControllerConsumer.consumeValue(topic);
        assertEquals(className, value);
    }
}
