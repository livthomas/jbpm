package org.jbpm.test.functional.event;

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

import java.util.Arrays;
import java.util.Collection;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertChangedVariable;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class TimerNodeTest extends JbpmTestCase {
    private static final String TIMER_EVENT_DESIGNER =
            "org/jbpm/test/functional/event/TimerNode-designer.bpmn2";
    private static final String TIMER_EVENT_ECLIPSE =
            "org/jbpm/test/functional/event/TimerNode-eclipse.bpmn";
    private static final String TIMER_EVENT_HANDWRITTEN =
            "org/jbpm/test/functional/event/TimerNode-handwritten.bpmn";
    private static final String TIMER_EVENT_DESIGNER_ID =
            "org.jbpm.test.functional.event.TimerNode-designer";
    private static final String TIMER_EVENT_ECLIPSE_ID =
            "org.jbpm.test.functional.event.TimerNode-eclipse";
    private static final String TIMER_EVENT_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.TimerNode-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { TIMER_EVENT_ECLIPSE, TIMER_EVENT_ECLIPSE_ID },
                { TIMER_EVENT_DESIGNER, TIMER_EVENT_DESIGNER_ID },
                { TIMER_EVENT_HANDWRITTEN, TIMER_EVENT_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private IterableProcessEventListener events;
    private TrackingProcessEventListener process;

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public TimerNodeTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
        events = new IterableProcessEventListener();
        process = new TrackingProcessEventListener();
    }

    protected static final KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static final KieCommands getCommands() {
        return getServices().getCommands();
    }

    @Test(timeout = 30000)
    public void testTimer() throws Exception {
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newStartProcess(processId);
        kieSession.execute(cmd);

        Thread.sleep(2000);

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "StartProcess");
        assertTriggered(events, "initialize");
        assertChangedVariable(events, "count", null, 0);
        assertLeft(events, "initialize");
        assertTriggered(events, "Recurring timer");
        for (int i = 0; i < 3; i++) {
            assertLeft(events, "Recurring timer");
            assertTriggered(events, "increment");
            assertChangedVariable(events, "count", i, i + 1);
            assertLeft(events, "increment");
            assertNextNode(events, "Gateway");

            if (i < 2) {
                assertNextNode(events, "End");
            } else {
                assertNextNode(events, "Terminate");
            }
        }
        assertProcessCompleted(events, processId);
        assertFalse(events.hasNext());
    }

}