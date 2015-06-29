package org.jbpm.test.functional.event.end;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.drools.core.command.runtime.process.RegisterWorkItemHandlerCommand;
import org.drools.core.process.instance.WorkItemHandler;
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
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;

@RunWith(Parameterized.class)
public class MessageTest extends JbpmTestCase {
    private static final String END_MESSAGE_DESIGNER =
            "org/jbpm/test/functional/event/end/Message-endMessageEvent-designer.bpmn2";
    private static final String END_MESSAGE_HANDWRITTEN =
            "org/jbpm/test/functional/event/end/Message-endMessageEvent-handwritten.bpmn";
    private static final String END_MESSAGE_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Message-endMessageEvent-designer";
    private static final String END_MESSAGE_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Message-endMessageEvent-handwritten";

    @Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_MESSAGE_DESIGNER, END_MESSAGE_DESIGNER_ID },
                { END_MESSAGE_HANDWRITTEN, END_MESSAGE_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public MessageTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
    }

    protected static  KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    @Test(timeout = 30000)
    public void testMessageEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        RecordingHandler handler = new RecordingHandler();

        List<Command<?>> cmds = new ArrayList<Command<?>>();
        cmds.add(new RegisterWorkItemHandlerCommand("Send Task", handler));
        cmds.add(getCommands().newStartProcess(processId));
        kieSession.execute(getCommands().newBatchExecution(cmds, null));


        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertNextNode(events, "end");
        assertProcessCompleted(events, processId);

        Assertions.assertThat(events.hasNext()).isFalse();
        Assertions.assertThat(handler.item).isNotNull();
    }

    private static class RecordingHandler implements WorkItemHandler {
        private WorkItem item = null;

        @Override
        public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
            if (item != null) {
                throw new IllegalStateException("Work item is already set!");
            }
            this.item = workItem;
        }

        @Override
        public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
            // nothing
        }
    }
}

