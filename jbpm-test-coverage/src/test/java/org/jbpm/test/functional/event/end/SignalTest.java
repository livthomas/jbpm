package org.jbpm.test.functional.event.end;

import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;

import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertLeft;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class SignalTest extends JbpmTestCase {
    private static final String END_SIGNAL_DESIGNER =
            "org/jbpm/test/functional/event/end/Signal-signalEndEvent-designer.bpmn2," +
            "org/jbpm/test/functional/event/start/Signal-signalStartEvent-designer.bpmn2";
    private static final String END_SIGNAL_HANDWRITTEN =
            "org/jbpm/test/functional/event/end/Signal-signalEndEvent-handwritten.bpmn," +
            "org/jbpm/test/functional/event/start/Signal-signalStartEvent-handwritten.bpmn2";
    private static final String END_SIGNAL_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Signal-signalEndEvent-designer";
    private static final String START_SIGNAL_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.Signal-signalStartEvent-designer";
    private static final String END_SIGNAL_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Signal-signalEndEvent-handwritten";
    private static final String START_SIGNAL_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.Signal-signalStartEvent-handwritten";

    @Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_SIGNAL_DESIGNER,
                        END_SIGNAL_DESIGNER_ID,
                        START_SIGNAL_DESIGNER_ID },
                { END_SIGNAL_HANDWRITTEN,
                        END_SIGNAL_HANDWRITTEN_ID,
                        START_SIGNAL_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private String otherProcess;

    private KieSession kieSession;

    public SignalTest(String processPath, String processId, String otherProcess) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
        this.otherProcess = otherProcess;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath.split(","));
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    @Test(timeout = 30000)
    public void testSignalEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newStartProcess(processId);
        kieSession.execute(cmd);

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessStarted(otherProcess)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(otherProcess)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertTriggered(events, "end");
        assertProcessStarted(events, otherProcess);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertNextNode(events, "end");
        assertProcessCompleted(events, otherProcess);
        assertLeft(events, "end");
        assertProcessCompleted(events, processId);

        Assertions.assertThat(events.hasNext()).isFalse();
    }
}
