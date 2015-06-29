package org.jbpm.test.functional.gateway;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.jbpm.test.tools.IterableListenerAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.runtime.KieSession;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Inclusive gateway tests. combination of diverging OR gateway with converging
 * XOR gateway
 *
 * Converging OR gateway is not supported!
 *
 * Converging XOR does not behave according to documentation: bz803692
 */
@RunWith(Parameterized.class)
public class InclusiveGatewayTest extends JbpmTestCase {
    private static final String INCLUSIVE_GATEWAY_DESIGNER =
            "org/jbpm/test/functional/gateway/InclusiveGateway-designer.bpmn";
    private static final String INCLUSIVE_GATEWAY_HANDWRITTEN =
            "org/jbpm/test/functional/gateway/InclusiveGateway-handwritten.bpmn";
    private static final String INCLUSIVE_GATEWAY_ECLIPSE =
            "org/jbpm/test/functional/gateway/InclusiveGateway-eclipse.bpmn";
    private static final String INCLUSIVE_GATEWAY_DESIGNER_ID =
            "org.jbpm.test.functional.gateway.InclusiveGateway-designer";
    private static final String INCLUSIVE_GATEWAY_HANDWRITTEN_ID =
            "org.jbpm.test.functional.gateway.InclusiveGateway-handwritten";
    private static final String INCLUSIVE_GATEWAY_ECLIPSE_ID =
            "org.jbpm.test.functional.gateway.InclusiveGateway-eclipse";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { INCLUSIVE_GATEWAY_ECLIPSE, INCLUSIVE_GATEWAY_ECLIPSE_ID },
                { INCLUSIVE_GATEWAY_DESIGNER, INCLUSIVE_GATEWAY_DESIGNER_ID },
                { INCLUSIVE_GATEWAY_HANDWRITTEN, INCLUSIVE_GATEWAY_HANDWRITTEN_ID }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;
    private IterableProcessEventListener iterableListener;

    public InclusiveGatewayTest(String processPath, String processId) {
        super(false);
        this.processPath = processPath;
        this.processId = processId;
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(processPath);
        iterableListener = new IterableProcessEventListener();
    }

    /**
     * Inclusive diverging gateway & exclusive converging. Two of three
     * conditions are satisfied, XOR gateway is excepted to be triggered 2x
     * (doc. 5.6.2)
     */
    @Test(timeout = 30000)
    public void testInclusive() {
        kieSession.addEventListener(iterableListener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", 15);

        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(processId);
        spc.setParameters(params);
        kieSession.execute(spc);

        IterableListenerAssert.assertChangedVariable(iterableListener, "x", null, 15);
        IterableListenerAssert.assertProcessStarted(iterableListener, processId);
        IterableListenerAssert.assertNextNode(iterableListener, "start");
        IterableListenerAssert.assertNextNode(iterableListener, "fork");
        IterableListenerAssert.assertNextNode(iterableListener, "script1");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "finalScript");
        IterableListenerAssert.assertNextNode(iterableListener, "end");
        IterableListenerAssert.assertLeft(iterableListener, "fork");
        IterableListenerAssert.assertNextNode(iterableListener, "script2");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "finalScript");
        IterableListenerAssert.assertNextNode(iterableListener, "end");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }
}
