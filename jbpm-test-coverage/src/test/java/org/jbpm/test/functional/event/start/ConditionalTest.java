package org.jbpm.test.functional.event.start;

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;

@RunWith(Parameterized.class)
public class ConditionalTest extends JbpmTestCase {
    private static final String START_CONDITIONAL_DESIGNER =
            "org/jbpm/test/functional/event/start/Conditional-conditionalStartEvent-designer.bpmn2";
    private static final String START_CONDITIONAL_HANDWRITTEN =
            "org/jbpm/test/functional/event/start/Conditional-conditionalStartEvent-handwritten.bpmn";
    private static final String START_CONDITIONAL_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.Conditional-conditionalStartEvent-designer";
    private static final String START_CONDITIONAL_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.Conditional-conditionalStartEvent-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { START_CONDITIONAL_DESIGNER,
                        START_CONDITIONAL_DESIGNER_ID, "startDesigner"},
                { START_CONDITIONAL_HANDWRITTEN,
                        START_CONDITIONAL_HANDWRITTEN_ID, "startHandwritten"}
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private String condition;

    private KieSession kieSession;

    public ConditionalTest(String processPath, String processId, String condition) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
        this.condition = condition;
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

    @Test(timeout = 30000)
    public void testConditionalStartEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        List<Command<?>> cmds = new ArrayList<Command<?>>();
        cmds.add(getCommands().newInsert(condition));
        cmds.add(getCommands().newFireAllRules());
        kieSession.execute(getCommands().newBatchExecution(cmds, null));

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertNextNode(events, "end");
        assertProcessCompleted(events, processId);

        Assertions.assertThat(events.hasNext()).isFalse();
    }
}
