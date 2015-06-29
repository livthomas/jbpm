package org.jbpm.test.functional.task;

import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.command.Command;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.command.CommandFactory;

import java.util.*;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;

/**
 * Business rules task test. testing execution of rules with specified rule-flow
 * group.
 */
@RunWith(Parameterized.class)
public class RuleTaskTest extends JbpmTestCase {
    private static final String RULE_TASK_DESIGNER =
            "org/jbpm/test/functional/task/RuleTask-designer.bpmn";
    private static final String RULE_TASK_ECLIPSE =
            "org/jbpm/test/functional/task/RuleTask-eclipse.bpmn";
    private static final String RULE_TASK_HANDWRITTEN =
            "org/jbpm/test/functional/task/RuleTask-handwritten.bpmn";
    private static final String RULE_TASK_DESIGNER_ID =
            "org.jbpm.test.functional.task.RuleTask-designer";
    private static final String RULE_TASK_ECLIPSE_ID =
            "org.jbpm.test.functional.task.RuleTask-eclipse";
    private static final String RULE_TASK_HANDWRITTEN_ID =
            "org.jbpm.test.functional.task.RuleTask-handwritten";
    private static final String RULE = "org/jbpm/test/functional/task/RuleTask-rule.drl";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { RULE_TASK_ECLIPSE, RULE_TASK_ECLIPSE_ID },
                { RULE_TASK_DESIGNER, RULE_TASK_DESIGNER_ID },
                { RULE_TASK_HANDWRITTEN, RULE_TASK_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private KieSession kieSession;

    public RuleTaskTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        Map<String, ResourceType> res = new HashMap<String, ResourceType>();
        res.put(processPath, ResourceType.BPMN2);
        res.put(RULE, ResourceType.DRL);
        kieSession = createKSession(res);
    }

    @Test(timeout = 30000)
    public void testRuleTask() {
        List<String> executedRules = new ArrayList<String>();
        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(CommandFactory.newSetGlobal("executed", executedRules));
        commands.add(CommandFactory.newStartProcess(processId));
        commands.add(CommandFactory.newFireAllRules());

        IterableProcessEventListener listener = new IterableProcessEventListener();
        kieSession.addEventListener(listener);
        kieSession.execute(CommandFactory.newBatchExecution(commands));

        assertProcessStarted(listener, processId);
        assertNextNode(listener, "start");
        assertNextNode(listener, "rules");
        assertNextNode(listener, "end");
        assertProcessCompleted(listener, processId);

        assertEquals(3, executedRules.size());
        String[] expected = new String[] { "firstRule", "secondRule", "thirdRule" };

        for (String expectedRuleName : expected) {
            assertTrue(executedRules.contains(expectedRuleName));
        }
    }

    @Test(timeout = 30000)
    public void testRuleTaskInsertFact() {
        List<String> executedRules = new ArrayList<String>();
        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(CommandFactory.newSetGlobal("executed", executedRules));
        commands.add(CommandFactory.newStartProcess(processId));
        commands.add(CommandFactory.newInsert(6));
        commands.add(CommandFactory.newFireAllRules());

        IterableProcessEventListener listener = new IterableProcessEventListener();
        kieSession.addEventListener(listener);
        kieSession.execute(CommandFactory.newBatchExecution(commands));

        assertProcessStarted(listener, processId);
        assertNextNode(listener, "start");
        assertNextNode(listener, "rules");
        assertNextNode(listener, "end");
        assertProcessCompleted(listener, processId);

        assertEquals(4, executedRules.size());
        String[] expected = new String[] { "firstRule", "secondRule", "thirdRule", "fifthRule" };

        for (String expectedRuleName : expected) {
            assertTrue(executedRules.contains(expectedRuleName));
        }
    }

    public KieSession createKSession(Map<String, ResourceType> res) {
        createRuntimeManager(res);
        return getRuntimeEngine().getKieSession();
    }
}