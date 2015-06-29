package org.jbpm.test.functional.gateway;

import org.drools.core.time.SessionPseudoClock;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.jbpm.test.tools.TrackingListenerAssert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.command.CommandFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Event-based gateway execution test. branches: condition event, signal event,
 * message event, timer event (default after 1 sec)
 * */
@RunWith(Parameterized.class)
public class EventBasedGatewayTest extends JbpmTestCase {
    private static final String EVENT_BASED_GATEWAY_DESIGNER =
            "org/jbpm/test/functional/gateway/EventBasedGateway-designer.bpmn";
    private static final String EVENT_BASED_GATEWAY_HANDWRITTEN =
            "org/jbpm/test/functional/gateway/EventBasedGateway-handwritten.bpmn";
    private static final String EVENT_BASED_GATEWAY_DESIGNER_ID =
            "org.jbpm.test.functional.gateway.EventBasedGateway-designer";
    private static final String EVENT_BASED_GATEWAY_HANDWRITTEN_ID =
            "org.jbpm.test.functional.gateway.EventBasedGateway-handwritten";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { EVENT_BASED_GATEWAY_DESIGNER, EVENT_BASED_GATEWAY_DESIGNER_ID },
                { EVENT_BASED_GATEWAY_HANDWRITTEN, EVENT_BASED_GATEWAY_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;

    public EventBasedGatewayTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
    }

    /**
     * Conditional event branch
     */
    @Test(timeout = 30000)
    public void testConditional() {
        TrackingProcessEventListener tpel = new TrackingProcessEventListener();
        kieSession.addEventListener(tpel);

        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(CommandFactory.newInsert(5));
        commands.add(CommandFactory.newStartProcess(processId));
        kieSession.execute(CommandFactory.newBatchExecution(commands));

        TrackingListenerAssert.assertProcessStarted(tpel, processId);
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "start");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "fork");
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "cond");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "join");
        TrackingListenerAssert.assertTriggered(tpel, "end");
        TrackingListenerAssert.assertProcessCompleted(tpel, processId);
    }

    /**
     * Signal event branch
     */
    @Test(timeout = 30000)
    public void testSignal() {
        TrackingProcessEventListener tpel = new TrackingProcessEventListener();
        kieSession.addEventListener(tpel);

        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(CommandFactory.newStartProcess(processId));
        commands.add(CommandFactory.newSignalEvent("sigkill", null));
        kieSession.execute(CommandFactory.newBatchExecution(commands));

        TrackingListenerAssert.assertProcessStarted(tpel, processId);
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "start");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "fork");
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "sig");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "join");
        TrackingListenerAssert.assertTriggered(tpel, "end");
        TrackingListenerAssert.assertProcessCompleted(tpel, processId);
    }

    @Test(timeout = 30000)
    public void testMessage() {
        TrackingProcessEventListener tpel = new TrackingProcessEventListener();
        kieSession.addEventListener(tpel);

        ProcessInstance pi = (ProcessInstance) kieSession.execute((Command<?>) CommandFactory.newStartProcess(processId));
        TrackingListenerAssert.assertProcessStarted(tpel, processId);
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "start");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "fork");
        TrackingListenerAssert.assertTriggered(tpel, "msg");

        kieSession.execute((Command<?>) CommandFactory.newSignalEvent(pi.getId(), "Message-message1", null));

        TrackingListenerAssert.assertLeft(tpel, "msg");
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "join");
        TrackingListenerAssert.assertTriggered(tpel, "end");
        TrackingListenerAssert.assertProcessCompleted(tpel, processId);
    }

    /**
     * No branch is selected, timer is triggered after 1 sec
     */
    @Test(timeout = 30000)
    public void testTimer() throws InterruptedException {
        Assume.assumeFalse(kieSession.getSessionClock() instanceof SessionPseudoClock);
        TrackingProcessEventListener tpel = new TrackingProcessEventListener();
        kieSession.addEventListener(tpel);

        kieSession.execute((Command<?>) CommandFactory.newStartProcess(processId));
        TrackingListenerAssert.assertProcessStarted(tpel, processId);
        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "start");
        TrackingListenerAssert.assertTriggered(tpel, "fork");
        TrackingListenerAssert.assertLeft(tpel, "fork", 4);
        TrackingListenerAssert.assertTriggered(tpel, "cond");
        TrackingListenerAssert.assertTriggered(tpel, "msg");
        TrackingListenerAssert.assertTriggered(tpel, "sig");
        TrackingListenerAssert.assertTriggered(tpel, "timer");
        Thread.sleep(1500);
        TrackingListenerAssert.assertLeft(tpel, "timer");

        TrackingListenerAssert.assertTriggeredAndLeft(tpel, "join");
        TrackingListenerAssert.assertTriggered(tpel, "end");
        TrackingListenerAssert.assertProcessCompleted(tpel, processId);
    }
}
