package io.github.greyp9.nifi10197;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.processors.script.ExecuteScript;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NullHandling12Test {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testExecuteScriptNullHandling() throws IOException {
        final TestRunner runner = TestRunners.newTestRunner(ExecuteScript.class);
        runner.setValidateExpressionUsage(false);
        final URL url = getClass().getClassLoader().getResource("script.ecma");
        Assertions.assertNotNull(url);
        final String scriptBody = IOUtils.toString(url, StandardCharsets.UTF_8);
        Assertions.assertNotNull(scriptBody);

        final MockFlowFile flowFileIn = new MockFlowFile(1L);
        final Map<String, String> attributes = new TreeMap<>();
        attributes.put("xyzId", "myattribute");  // if this line commented out, 1.12.1 pass, 1.16.3 fail
        flowFileIn.putAttributes(attributes);
        runner.enqueue(flowFileIn);

        runner.setProperty("Script Engine", "ECMAScript");
        runner.setProperty("Script Body", scriptBody);
        runner.run();

        runner.assertAllFlowFilesTransferred(ExecuteScript.REL_SUCCESS, 1);
        final List<MockFlowFile> flowFiles = runner.getFlowFilesForRelationship(ExecuteScript.REL_SUCCESS);
        final MockFlowFile flowFileOut = flowFiles.iterator().next();
        logger.info(flowFileOut.getAttributes().toString());
    }
}
