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
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessCompleted;
import static org.jbpm.test.tools.IterableListenerAssert.assertProcessStarted;

@RunWith(Parameterized.class)
public class TerminatingTest extends JbpmTestCase {
    private static final String END_TERMINATING_ECLIPSE =
            "org/jbpm/test/functional/event/end/Terminating-terminatingEndEvent-eclipse.bpmn";
    private static final String END_TERMINATING_HANDWRITTEN=
            "org/jbpm/test/functional/event/end/Terminating-terminatingEndEvent-handwritten.bpmn";
    private static final String END_TERMINATING_DESIGNER =
            "org/jbpm/test/functional/event/end/Terminating-terminatingEndEvent-designer.bpmn2";
    private static final String END_TERMINATING_ECLIPSE_ID =
            "org.jbpm.test.functional.event.end.Terminating-terminatingEndEvent-eclipse";
    private static final String END_TERMINATING_DESIGNER_ID =
            "org.jbpm.test.functional.event.end.Terminating-terminatingEndEvent-designer";
    private static final String END_TERMINATING_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.end.Terminating-terminatingEndEvent-handwritten";

    @Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { END_TERMINATING_ECLIPSE, END_TERMINATING_ECLIPSE_ID },
                { END_TERMINATING_DESIGNER, END_TERMINATING_DESIGNER_ID},
                { END_TERMINATING_HANDWRITTEN, END_TERMINATING_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public TerminatingTest(String processPath, String processId) {
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

    @Test(timeout = 30000)
    public void testTerminatingEndEvent() {
        IterableProcessEventListener events = new IterableProcessEventListener();
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(events);
        kieSession.addEventListener(process);

        Command<?> cmd = getCommands().newStartProcess(processId);
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

