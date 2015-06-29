package org.jbpm.test.functional.event.end;

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
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class EscalationTest extends JbpmTestCase {
    private static final String END_ESCALATION_DESIGNER =
            "org/jbpm/test/functional/event/end/Escalation-escalationEndEvent-designer.bpmn2";
    private static final String END_ESCALATION_ECLIPSE =
            "org/jbpm/test/functional/event/end/Escalation-escalationEndEvent-eclipse.bpmn";
    private static final String END_ESCALATION_HANDWRITTEN =
            "org/jbpm/test/functional/event/end/Escalation-escalationEndEvent-handwritten.bpmn";
    private static final String END_ESCALATION_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Escalation-escalationEndEvent-designer";
    private static final String END_ESCALATION_ECLIPSE_ID =
            "org.jbpm.test.functional.event.end.Escalation-escalationEndEvent-eclipse";
    private static final String END_ESCALATION_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Escalation-escalationEndEvent-handwritten";

    @Parameterized.Parameters(name = "[{index}] {1}")
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_ESCALATION_ECLIPSE, END_ESCALATION_ECLIPSE_ID },
                { END_ESCALATION_DESIGNER, END_ESCALATION_DESIGNER_ID },
                { END_ESCALATION_HANDWRITTEN, END_ESCALATION_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public EscalationTest(String processPath, String processId) {
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
     * Bug 1015221 - Generated process which has an escalation definition uses the 'ID' attribute
     * not the 'errorCode' to identify it in an event.
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1015221" />
     */
    @Test(timeout = 30000)
    public void testEscalationEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newStartProcess(processId);
        kieSession.execute(cmd);

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isFalse();
        Assertions.assertThat(process.wasProcessAborted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertTriggered(events, "end");
        assertProcessCompleted(events, processId);
        assertFalse(events.hasNext());
    }
}