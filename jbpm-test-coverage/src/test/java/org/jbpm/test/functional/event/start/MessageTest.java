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
public class MessageTest extends JbpmTestCase {
    private static final String START_MESSAGE_DESIGNER =
            "org/jbpm/test/functional/event/start/Message-messageStartEvent-designer.bpmn2";
    private static final String START_MESSAGE_HANDWRITTEN =
            "org/jbpm/test/functional/event/start/Message-messageStartEvent-handwritten.bpmn";
    private static final String START_MESSAGE_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.Message-messageStartEvent-designer";
    private static final String START_MESSAGE_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.Message-messageStartEvent-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { START_MESSAGE_DESIGNER,
                        START_MESSAGE_DESIGNER_ID, "messageDesigner" },
                { START_MESSAGE_HANDWRITTEN,
                        START_MESSAGE_HANDWRITTEN_ID, "messageHandwritten" }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;
    private final String messageType;

    private KieSession kieSession;

    public MessageTest(String processPath, String processId, String messageType) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
        this.messageType = messageType;
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
    public void testMessageStartEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newSignalEvent("Message-" + messageType, null);
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
