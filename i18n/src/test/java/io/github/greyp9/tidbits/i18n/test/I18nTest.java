package io.github.greyp9.tidbits.i18n.test;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

public class I18nTest {
    private final Logger logger = Logger.getLogger(getClass().getName());

    @Test
    public void testDumpLocale() {
        final Map<String, String> env = System.getenv();
        env.entrySet().stream()
                .filter(e -> (e.getKey().startsWith("LC")
                        || e.getKey().startsWith("LANG")
                        || e.getKey().startsWith("JAVA")))
                .map(e -> String.format("ENVIRONMENT: NAME=[%s] VALUE=[%s]", e.getKey(), e.getValue()))
                .forEach(logger::info);
        logger.info(String.format("LOCALE: LANGUAGE=[%s] COUNTRY=[%s] REGION=[%s]",
                System.getProperty("user.language"),
                System.getProperty("user.country"),
                System.getProperty("user.region"),
                System.getProperty("user.timezone")));
    }
}
