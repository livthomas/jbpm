package org.jbpm.test.functional.gateway;

import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.jbpm.test.tools.TrackingListenerAssert;
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

/**
 * Parallel gateway execution test. 2x parallel fork, 1x join
 */
@RunWith(Parameterized.class)
public class ParallelGatewayTest extends JbpmTestCase {
    private static final String PARALLEL_GATEWAY_DESIGNER =
            "org/jbpm/test/functional/gateway/ParallelGateway-designer.bpmn";
    private static final String PARALLEL_GATEWAY_HANDWRITTEN =
            "org/jbpm/test/functional/gateway/ParallelGateway-handwritten.bpmn";
    private static final String PARALLEL_GATEWAY_ECLIPSE =
            "org/jbpm/test/functional/gateway/ParallelGateway-eclipse.bpmn";
    private static final String PARALLEL_GATEWAY_DESIGNER_ID =
            "org.jbpm.test.functional.gateway.ParallelGateway-designer";
    private static final String PARALLEL_GATEWAY_HANDWRITTEN_ID =
            "org.jbpm.test.functional.gateway.ParallelGateway-handwritten";
    private static final String PARALLEL_GATEWAY_ECLIPSE_ID =
            "org.jbpm.test.functional.gateway.ParallelGateway-eclipse";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { PARALLEL_GATEWAY_ECLIPSE, PARALLEL_GATEWAY_ECLIPSE_ID },
                { PARALLEL_GATEWAY_DESIGNER, PARALLEL_GATEWAY_DESIGNER_ID },
                { PARALLEL_GATEWAY_HANDWRITTEN, PARALLEL_GATEWAY_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;
    private TrackingProcessEventListener trackingListener;

    public ParallelGatewayTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
        trackingListener = new TrackingProcessEventListener();
    }

    protected static KieServices getServices() {
        return KieServices.Factory.get();
    }

    protected static KieCommands getCommands() {
        return getServices().getCommands();
    }

    /**
     * Simple parallel gateway test.
     */
    @Test(timeout = 30000)
    public void testParallel() {
        kieSession.addEventListener(trackingListener);
        kieSession.execute((Command<?>) getCommands().newStartProcess(processId));

        TrackingListenerAssert.assertProcessStarted(trackingListener, processId);
        TrackingListenerAssert.assertTriggeredAndLeft(trackingListener, "start");

        TrackingListenerAssert.assertTriggered(trackingListener, "fork1", 1);
        TrackingListenerAssert.assertLeft(trackingListener, "fork1", 2);

        TrackingListenerAssert.assertTriggeredAndLeft(trackingListener, "script1");

        TrackingListenerAssert.assertTriggered(trackingListener, "fork2");
        TrackingListenerAssert.assertLeft(trackingListener, "fork2", 2);

        TrackingListenerAssert.assertTriggeredAndLeft(trackingListener, "script2");

        TrackingListenerAssert.assertTriggered(trackingListener, "join", 3);
        TrackingListenerAssert.assertLeft(trackingListener, "join", 1);

        TrackingListenerAssert.assertTriggered(trackingListener, "end");
        TrackingListenerAssert.assertProcessCompleted(trackingListener, processId);
    }

}
