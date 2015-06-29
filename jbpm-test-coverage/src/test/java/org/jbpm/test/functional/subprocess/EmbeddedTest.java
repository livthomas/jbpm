package org.jbpm.test.functional.subprocess;

import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class EmbeddedTest extends JbpmTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedTest.class);
    private static final String EMBEDDED_SUBPROCESS_DESIGNER = "org/jbpm/test/functional/subprocess/EmbeddedSubprocess-designer.bpmn";
    private static final String EMBEDDED_SUBPROCESS_ECLIPSE = "org/jbpm/test/functional/subprocess/EmbeddedSubprocess-eclipse.bpmn";
    private static final String EMBEDDED_SUBPROCESS_HANDWRITTEN = "org/jbpm/test/functional/subprocess/EmbeddedSubprocess-handwritten.bpmn";
    private static final String EMBEDDED_SUBPROCESS_DESIGNER_ID = "org.jbpm.test.functional.subprocess.EmbeddedSubprocess-designer";
    private static final String EMBEDDED_SUBPROCESS_ECLIPSE_ID = "org.jbpm.test.functional.subprocess.EmbeddedSubprocess-eclipse";
    private static final String EMBEDDED_SUBPROCESS_HANDWRITTEN_ID = "org.jbpm.test.functional.subprocess.EmbeddedSubprocess-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { EMBEDDED_SUBPROCESS_ECLIPSE, EMBEDDED_SUBPROCESS_ECLIPSE_ID },
                { EMBEDDED_SUBPROCESS_DESIGNER, EMBEDDED_SUBPROCESS_DESIGNER_ID },
                { EMBEDDED_SUBPROCESS_HANDWRITTEN, EMBEDDED_SUBPROCESS_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;
    private IterableProcessEventListener eventListener;

    public EmbeddedTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
        eventListener = new IterableProcessEventListener();
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    @Test(timeout = 30000)
    public void testProcessWithEmbeddedSubprocess() {
        kieSession.addEventListener(eventListener);
        kieSession.execute((Command<?>) getCommands().newStartProcess(processId));

        assertProcessStarted(eventListener, processId);
        assertNextNode(eventListener, "start");
        assertNextNode(eventListener, "ScriptOuter");
        assertTriggered(eventListener, "embedded");

        LOGGER.info("inside embedded subprocess");

        assertNextNode(eventListener, "sub-start");
        assertNextNode(eventListener, "ScriptInner");
        assertNextNode(eventListener, "sub-end");
        assertLeft(eventListener, "embedded");

        LOGGER.info("outside embedded subprocess");

        assertNextNode(eventListener, "end");
        assertProcessCompleted(eventListener, processId);
    }

}
