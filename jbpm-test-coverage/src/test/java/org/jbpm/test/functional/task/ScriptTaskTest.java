package org.jbpm.test.functional.task;

import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.domain.Person;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;

import java.util.*;

import static org.jbpm.test.tools.IterableListenerAssert.assertMultipleVariablesChanged;
import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertChangedVariable;

/**
 * Testing script task - both Java and MVEL language.
 *
 */
@RunWith(Parameterized.class)
public class ScriptTaskTest extends JbpmTestCase {
    private static final String SCRIPT_TASK_DESIGNER =
            "org/jbpm/test/functional/task/ScriptTask-designer.bpmn";
    private static final String SCRIPT_TASK_ECLIPSE =
            "org/jbpm/test/functional/task/ScriptTask-eclipse.bpmn";
    private static final String SCRIPT_TASK_HANDWRITTEN =
            "org/jbpm/test/functional/task/ScriptTask-handwritten.bpmn";
    private static final String SCRIPT_TASK_DESIGNER_ID =
            "org.jbpm.test.functional.task.ScriptTask-designer";
    private static final String SCRIPT_TASK_ECLIPSE_ID =
            "org.jbpm.test.functional.task.ScriptTask-eclipse";
    private static final String SCRIPT_TASK_HANDWRITTEN_ID =
            "org.jbpm.test.functional.task.ScriptTask-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { SCRIPT_TASK_ECLIPSE, SCRIPT_TASK_ECLIPSE_ID },
                { SCRIPT_TASK_DESIGNER, SCRIPT_TASK_DESIGNER_ID },
                { SCRIPT_TASK_HANDWRITTEN, SCRIPT_TASK_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public ScriptTaskTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    /**
     * Object and collection access
     */
    @Test(timeout = 30000)
    public void testScriptTask() {
        IterableProcessEventListener ipel = new IterableProcessEventListener();
        kieSession.addEventListener(ipel);

        Map<String, Object> params = new HashMap<String, Object>();
        Person p = new Person("Vandrovec");
        params.put("person", p);
        List<Person> personList = new ArrayList<Person>();
        personList.add(new Person("Birsky"));
        personList.add(new Person("Korcasko"));
        params.put("personList", personList);

        kieSession.execute((Command<?>) getCommands().newStartProcess(processId, params));

        assertMultipleVariablesChanged(ipel, "person", "personList");

        assertProcessStarted(ipel, processId);
        assertNextNode(ipel, "start");
        assertTriggered(ipel, "scriptJava");
        assertChangedVariable(ipel, "output", null, "BirskyKorcaskoVandrovec");
        assertLeft(ipel, "scriptJava");
        assertTriggered(ipel, "scriptMvel");
        assertChangedVariable(ipel, "output", "BirskyKorcaskoVandrovec", "VandrovecBirskyKorcasko");
        assertLeft(ipel, "scriptMvel");
        assertNextNode(ipel, "end");
        assertProcessCompleted(ipel, processId);
    }

}


