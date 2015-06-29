package org.jbpm.test.functional.subprocess;

import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.DebugProcessEventListener;
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
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import static org.jbpm.test.tools.IterableListenerAssert.assertChangedVariable;
import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertChangedMultipleInstancesVariable;

/**
 * Testing multiple-instances subprocess execution.
 *
 * Bug 802721 -  CommandFactory.newStartProcess does not pass parameters to process instance
 * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=802721"/>
 * - general problem on KnowledgeAPI, discovered in this test
 */
@RunWith(Parameterized.class)
public class MultipleInstancesTest extends JbpmTestCase {
    private static final String MULTIPLE_SUBPROCESS_DESIGNER = "org/jbpm/test/functional/subprocess/MultipleInstancesSubprocess-designer.bpmn";
    private static final String MULTIPLE_SUBPROCESS_ECLIPSE = "org/jbpm/test/functional/subprocess/MultipleInstancesSubprocess-eclipse.bpmn";
    private static final String MULTIPLE_SUBPROCESS_HANDWRITTEN = "org/jbpm/test/functional/subprocess/MultipleInstancesSubprocess-handwritten.bpmn";
    private static final String MULTIPLE_SUBPROCESS_DESIGNER_ID = "org.jbpm.test.functional.subprocess.MultipleInstancesSubprocess-designer";
    private static final String MULTIPLE_SUBPROCESS_ECLIPSE_ID = "org.jbpm.test.functional.subprocess.MultipleInstancesSubprocess-eclipse";
    private static final String MULTIPLE_SUBPROCESS_HANDWRITTEN_ID = "org.jbpm.test.functional.subprocess.MultipleInstancesSubprocess-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { MULTIPLE_SUBPROCESS_ECLIPSE, MULTIPLE_SUBPROCESS_ECLIPSE_ID },
                { MULTIPLE_SUBPROCESS_DESIGNER, MULTIPLE_SUBPROCESS_DESIGNER_ID },
                { MULTIPLE_SUBPROCESS_HANDWRITTEN, MULTIPLE_SUBPROCESS_HANDWRITTEN_ID } };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;
    private IterableProcessEventListener eventListener;

    public MultipleInstancesTest(String processPath, String processId) {
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

    /**
     * Testing multiple-instances subprocess
     */
    @Test(timeout = 30000)
    public void testMultipleInstances() {
        kieSession.addEventListener(eventListener);
        kieSession.addEventListener(new DebugProcessEventListener());
        List<String> items = new ArrayList<String>();
        items.add("breakfast");
        items.add("lunch");
        items.add("dinner");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("list", items);

        kieSession.execute((Command<?>) getCommands().newStartProcess(processId, params));
        assertChangedVariable(eventListener, "list", null, items);
        assertProcessStarted(eventListener, processId);
        assertNextNode(eventListener, "start");
        assertNextNode(eventListener, "script");

        assertTriggered(eventListener, "multipleInstances");
        // collection is passed to multiple-instances node
        for (String str : items) {
            assertChangedMultipleInstancesVariable(eventListener, "listItem", null, str);
        }
        // multiple-instances node is processed for every collection item
        for (String str : items) {
            assertNextNode(eventListener, "innerStart");
            assertTriggered(eventListener, "innerScript");
            assertChangedMultipleInstancesVariable(eventListener, "listItem", str, str + "-eaten");
            assertLeft(eventListener, "innerScript");
            assertNextNode(eventListener, "innerEnd");
        }
        assertLeft(eventListener, "multipleInstances");
        assertNextNode(eventListener, "end");
        assertProcessCompleted(eventListener, processId);
    }



}

