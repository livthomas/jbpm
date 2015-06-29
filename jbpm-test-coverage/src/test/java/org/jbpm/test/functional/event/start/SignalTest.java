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

import java.util.Arrays;
import java.util.Collection;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;

@RunWith(Parameterized.class)
public class SignalTest extends JbpmTestCase {
    private static final String START_SIGNAL_DESIGNER =
            "org/jbpm/test/functional/event/start/Signal-signalStartEvent-designer.bpmn2";
    private static final String START_SIGNAL_HANDWRITTEN =
            "org/jbpm/test/functional/event/start/Signal-signalStartEvent-handwritten.bpmn2";
    private static final String START_SIGNAL_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.Signal-signalStartEvent-designer";
    private static final String START_SIGNAL_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.Signal-signalStartEvent-handwritten";


    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { START_SIGNAL_DESIGNER, START_SIGNAL_DESIGNER_ID,
                        "startDesigner" },
                { START_SIGNAL_HANDWRITTEN, START_SIGNAL_HANDWRITTEN_ID,
                        "startHandwritten" }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private String signalType;

    private KieSession kieSession;

    public SignalTest(String processPath, String processId, String signalType) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
        this.signalType = signalType;
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
    public void testSignalStartEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newSignalEvent(signalType, null);
        kieSession.execute(cmd);

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

