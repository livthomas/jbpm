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
public class NoneTest extends JbpmTestCase {

    private static final String START_NONE_DESIGNER =
            "org/jbpm/test/functional/event/start/None-noneStartEvent-designer.bpmn2";
    private static final String START_NONE_ECLIPSE =
            "org/jbpm/test/functional/event/start/None-noneStartEvent-eclipse.bpmn";
    private static final String START_NONE_HANDWRITTEN =
            "org/jbpm/test/functional/event/start/None-noneStartEvent-handwritten.bpmn";
    private static final String START_NONE_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.None-noneStartEvent-designer";
    private static final String START_NONE_ECLIPSE_ID =
            "org.jbpm.test.functional.event.start.None-noneStartEvent-eclipse";
    private static final String START_NONE_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.None-noneStartEvent-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { START_NONE_ECLIPSE, START_NONE_ECLIPSE_ID },
                { START_NONE_DESIGNER, START_NONE_DESIGNER_ID },
                { START_NONE_HANDWRITTEN, START_NONE_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public NoneTest(String processPath, String processId) {
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
    public void testNoneStartEvent() {
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
