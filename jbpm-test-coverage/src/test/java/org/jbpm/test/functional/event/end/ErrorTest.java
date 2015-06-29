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

import static org.jbpm.test.tools.IterableListenerAssert.assertNextNode;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;
import static org.jbpm.test.tools.IterableListenerAssert.assertTriggered;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;

@RunWith(Parameterized.class)
public class ErrorTest extends JbpmTestCase {
    private static final String END_ERROR_DESIGNER =
            "org/jbpm/test/functional/event/end/Error-errorEndEvent-designer.bpmn2";
    private static final String END_ERROR_HANDWRITTEN =
            "org/jbpm/test/functional/event/end/Error-errorEndEvent-handwritten.bpmn";
    private static final String END_ERROR_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Error-errorEndEvent-designer";
    private static final String END_ERROR_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Error-errorEndEvent-handwritten";

    @Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_ERROR_DESIGNER, END_ERROR_DESIGNER_ID },
                { END_ERROR_HANDWRITTEN , END_ERROR_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kiesession;

    public ErrorTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kiesession = createKSession(processPath);
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    @Test(timeout = 30000)
    public void testErrorEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kiesession.addEventListener(events);
        kiesession.addEventListener(process);

        Command<?> cmd = getCommands().newStartProcess(processId);
        kiesession.execute(cmd);

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isFalse();
        Assertions.assertThat(process.wasProcessAborted(processId)).isTrue();

        assertProcessStarted(events, processId);
        assertNextNode(events, "start");
        assertNextNode(events, "script");
        assertTriggered(events, "end");
        assertProcessCompleted(events, processId);

        Assertions.assertThat(events.hasNext()).isFalse();
    }
}