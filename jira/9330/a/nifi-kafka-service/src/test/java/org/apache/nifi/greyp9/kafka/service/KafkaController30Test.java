package org.apache.nifi.greyp9.kafka.service;

import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.apache.nifi.greyp9.kafka.api.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaController30Test {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Properties propertiesProducer;
    private Properties propertiesConsumer;
    private String topic;

    @BeforeEach
    void setUp() {
        // only run this test when the kafka 3 profile is activated
        assumeTrue("kafka-3-0".equals(System.getProperty("kafka.version")));
        propertiesProducer = TestUtils.getTestProducerProperties();
        propertiesConsumer = TestUtils.getTestConsumerProperties();
        topic = TestUtils.getTestTopic();
        logger.info("TOPIC={}", topic);
    }

    @Test
    public void testProduceConsumeValue() throws ReflectiveOperationException {
        final String className = "org.apache.nifi.greyp9.kafka30.KafkaController30";
        final Class<? extends KafkaController> kafkaControllerClass =
                Class.forName(className).asSubclass(KafkaController.class);
        final KafkaController<String, String> kafkaControllerProducer =
                kafkaControllerClass.getConstructor(Properties.class).newInstance(propertiesProducer);
        logger.info(kafkaControllerProducer.getClass().getName());
        //final NifiProducer<String, String> producer = kafkaController.getProducer();
        //logger.info(producer.toString());
        kafkaControllerProducer.publishValue(className, topic);
        final KafkaController<String, String> kafkaControllerConsumer =
                kafkaControllerClass.getConstructor(Properties.class).newInstance(propertiesConsumer);
        final String value = kafkaControllerConsumer.consumeValue(topic);
        assertEquals(className, value);
    }
}
