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

import java.util.Arrays;
import java.util.Collection;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertChangedVariable;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class CallActivityTest extends JbpmTestCase {
    private static final String REUSABLE_SP_DESIGNER = "org/jbpm/test/functional/subprocess/CallActivity-reusableSubprocess-designer.bpmn";
    private static final String REUSABLE_SP_ECLIPSE = "org/jbpm/test/functional/subprocess/CallActivity-reusableSubprocess-eclipse.bpmn";
    private static final String REUSABLE_SP_HANDWRITTEN = "org/jbpm/test/functional/subprocess/CallActivity-reusableSubprocess-handwritten.bpmn";
    private static final String CALL_ACTIVITY_DESIGNER = "org/jbpm/test/functional/subprocess/CallActivity-designer.bpmn";
    private static final String CALL_ACTIVITY_ECLIPSE = "org/jbpm/test/functional/subprocess/CallActivity-eclipse.bpmn";
    private static final String CALL_ACTIVITY_HANDWRITTEN = "org/jbpm/test/functional/subprocess/CallActivity-handwritten.bpmn";
    private static final String REUSABLE_SP_DESIGNER_ID = "org.jbpm.test.functional.subprocess.CallActivity-reusableSubprocess-designer";
    private static final String REUSABLE_SP_ECLIPSE_ID = "org.jbpm.test.functional.subprocess.CallActivity-reusableSubprocess-eclipse";
    private static final String REUSABLE_SP_HANDWRITTEN_ID = "org.jbpm.test.functional.subprocess.CallActivity-reusableSubprocess-handwritten";
    private static final String CALL_ACTIVITY_DESIGNER_ID = "org.jbpm.test.functional.subprocess.CallActivity-designer";
    private static final String CALL_ACTIVITY_ECLIPSE_ID = "org.jbpm.test.functional.subprocess.CallActivity-eclipse";
    private static final String CALL_ACTIVITY_HANDWRITTEN_ID = "org.jbpm.test.functional.subprocess.CallActivity-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { REUSABLE_SP_ECLIPSE + "," + CALL_ACTIVITY_ECLIPSE,
                        CALL_ACTIVITY_ECLIPSE_ID, REUSABLE_SP_ECLIPSE_ID },
                { REUSABLE_SP_DESIGNER + "," + CALL_ACTIVITY_DESIGNER,
                        CALL_ACTIVITY_DESIGNER_ID, REUSABLE_SP_DESIGNER_ID },
                { REUSABLE_SP_HANDWRITTEN + "," + CALL_ACTIVITY_HANDWRITTEN,
                        CALL_ACTIVITY_HANDWRITTEN_ID, REUSABLE_SP_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private String subprocessId;
    private IterableProcessEventListener eventListener;

    private KieSession kieSession;

    public CallActivityTest(String processPath, String processId, String subprocessId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
        this.subprocessId = subprocessId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath.split(","));
        eventListener = new IterableProcessEventListener();
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    /**
     * Testing call activity with reusable subprocess
     */
    @Test(timeout = 30000)
    public void testCallActivity() {
        kieSession.addEventListener(eventListener);
        kieSession.execute((Command<?>) getCommands().newStartProcess(processId));
        assertProcessStarted(eventListener, processId);

        assertNextNode(eventListener, "start");
        assertTriggered(eventListener, "script");
        assertChangedVariable(eventListener, "var", null, 1);
        assertLeft(eventListener, "script");

        assertTriggered(eventListener, "reusable");

        assertChangedVariable(eventListener, "inSubVar", null, 1);
        assertProcessStarted(eventListener, subprocessId);

        assertNextNode(eventListener, "rs-start");
        assertTriggered(eventListener, "rs-script");
        assertChangedVariable(eventListener, "outSubVar", null, "one");
        assertLeft(eventListener, "rs-script");
        assertNextNode(eventListener, "rs-end");
        assertProcessCompleted(eventListener, subprocessId);
        assertChangedVariable(eventListener, "var", 1, "one");
        assertLeft(eventListener, "reusable");
        assertNextNode(eventListener, "end");

        assertProcessCompleted(eventListener, processId);
    }
}
