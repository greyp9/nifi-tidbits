package org.apache.nifi.greyp9.kafka.service;

import org.apache.nifi.greyp9.kafka.api.KafkaController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class KafkaController26Test {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue("kafka-2-6".equals(System.getProperty("kafka.version")));
    }

    @Test
    void test_1_InstantiateController26() {
        //final KafkaController kafkaController = new KafkaController26();  // this won't compile in profile='30'
        //logger.info(kafkaController.getClass().getName());
    }

    @Test
    void test_2_InstantiateController26Reflect() throws ReflectiveOperationException {
        final String className = "org.apache.nifi.greyp9.kafka26.KafkaController26";
        final KafkaController kafkaController = Class.forName(className).asSubclass(KafkaController.class).newInstance();
        logger.info(kafkaController.getClass().getName());
        kafkaController.publishOne(className);
        final String value = kafkaController.consumeOne();
        Assertions.assertEquals(className, value);
    }
}
