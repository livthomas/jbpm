package org.jbpm.test.functional.event.start;

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.KieServices;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TimerTest extends JbpmTestCase {
    private static final String START_TIMER_DESIGNER =
            "org/jbpm/test/functional/event/start/Timer-timerStartEvent-designer.bpmn2";
    private static final String START_TIMER_HANDWRITTEN =
            "org/jbpm/test/functional/event/start/Timer-timerStartEvent-handwritten.bpmn";
    private static final String START_TIMER_DESIGNER_ID =
            "org.jbpm.test.functional.event.start.Timer-timerStartEvent-designer";
    private static final String START_TIMER_HANDWRITTEN_ID =
            "org.jbpm.test.functional.event.start.Timer-timerStartEvent-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { START_TIMER_DESIGNER, START_TIMER_DESIGNER_ID },
                { START_TIMER_HANDWRITTEN, START_TIMER_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public TimerTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
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
    public void testRecurringTimerStartEvent() throws Exception {
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(process);
        kieSession.fireAllRules();
        Thread.sleep(1500);

        Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();
        process.clear();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            Assertions.assertThat(process.wasProcessStarted(processId)).isTrue();
            Assertions.assertThat(process.wasProcessCompleted(processId)).isTrue();
            process.clear();
        }
    }

    @Test(timeout = 30000)
    public void testDelayingTimerStartEvent() throws Exception {
        TrackingProcessEventListener process = new TrackingProcessEventListener();
        kieSession.addEventListener(process);
        kieSession.fireAllRules();
        Thread.sleep(1500);
        kieSession.fireAllRules();

        Assertions.assertThat(process.wasProcessStarted(processId + "2")).isTrue();
        Assertions.assertThat(process.wasProcessCompleted(processId + "2")).isTrue();
        process.clear();
    }

}
