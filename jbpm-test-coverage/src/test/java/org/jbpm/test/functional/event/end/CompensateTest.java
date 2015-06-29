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
import org.kie.api.runtime.process.ProcessInstance;

import java.util.Arrays;
import java.util.Collection;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertChangedVariable;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;

@RunWith(Parameterized.class)
public class CompensateTest extends JbpmTestCase {
    private static final String END_COMPENSATE_DESIGNER =
            "org/jbpm/test/functional/event/end/Compensate-compensateEndEvent-designer.bpmn2";
    private static final String END_COMPENSATE_HANDWRITTEN =
            "org/jbpm/test/functional/event/end/Compensate-compensateEndEvent-handwritten.bpmn";
    private static final String END_COMPENSATE_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Compensate-compensateEndEvent-designer";
    private static final String END_COMPENSATE_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Compensate-compensateEndEvent-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_COMPENSATE_DESIGNER, END_COMPENSATE_DESIGNER_ID },
                { END_COMPENSATE_HANDWRITTEN, END_COMPENSATE_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public CompensateTest(String processPath, String processId) {
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
     * Bug 1021631 -  Compensation scope not found.
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1021631" />
     */
    @Test(timeout = 30000)
    public void testCompensateEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<ProcessInstance> cmd = getCommands().newStartProcess(processId);
        ProcessInstance pi = kieSession.execute(cmd);
        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertTriggered(events, "subprocess");
        assertNextNode(events, "sub-start");
        assertTriggered(events, "script");
        assertChangedVariable(events, "x", null, 0);
        assertLeft(events, "script");
        assertNextNode(events, "sub-end");
        assertLeft(events, "subprocess");

        assertTriggered(events, "end");
        assertLeft(events, "compensate-catch");
        assertTriggered(events, "compensate");
        assertChangedVariable(events, "x", 0, null);
        assertLeft(events, "compensate");

        Assertions.assertThat(pi.getState()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

}
