package org.jbpm.test.functional.gateway;

import org.drools.core.command.runtime.process.StartProcessCommand;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.IterableProcessEventListener;
import org.jbpm.test.tools.IterableListenerAssert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.runtime.KieSession;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Exclusive gateway test. priorities, default gate, conditions (XPath, Java,
 * MVEL)
 */
@RunWith(Parameterized.class)
public class ExclusiveGatewayTest extends JbpmTestCase {
    private static final String EXCLUSIVE_GATEWAY_DESIGNER =
            "org/jbpm/test/functional/gateway/ExclusiveGateway-designer.bpmn";
    private static final String EXCLUSIVE_GATEWAY_HANDWRITTEN =
            "org/jbpm/test/functional/gateway/ExclusiveGateway-handwritten.bpmn";
    private static final String EXCLUSIVE_GATEWAY_ECLIPSE =
            "org/jbpm/test/functional/gateway/ExclusiveGateway-eclipse.bpmn";
    private static final String EXCLUSIVE_GATEWAY_DESIGNER_ID =
            "org.jbpm.test.functional.gateway.ExclusiveGateway-designer";
    private static final String EXCLUSIVE_GATEWAY_HANDWRITTEN_ID =
            "org.jbpm.test.functional.gateway.ExclusiveGateway-handwritten";
    private static final String EXCLUSIVE_GATEWAY_ECLIPSE_ID =
            "org.jbpm.test.functional.gateway.ExclusiveGateway-eclipse";

    @Parameterized.Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] {
                { EXCLUSIVE_GATEWAY_ECLIPSE, EXCLUSIVE_GATEWAY_ECLIPSE_ID },
                { EXCLUSIVE_GATEWAY_DESIGNER, EXCLUSIVE_GATEWAY_DESIGNER_ID },
                { EXCLUSIVE_GATEWAY_HANDWRITTEN, EXCLUSIVE_GATEWAY_HANDWRITTEN_ID  }
        };
        return Arrays.asList(data);
    }

    private String processPath;
    private String processId;

    private KieSession kieSession;
    private IterableProcessEventListener iterableListener;

    public ExclusiveGatewayTest(String processPath, String processId) {
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
     * Exclusive gateway test; only one gate has condition expression == true.
     * 10 > "5" > 1 => second gate should be taken
     */
    @Test(timeout = 30000)
    public void testExclusive1() {
        Assume.assumeFalse(processId.contains("ExclusiveGateway-eclipse"));
        kieSession.addEventListener(iterableListener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", 5);
        Element el = createTestElement("sample", "value", "test");
        params.put("element", el);
        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(processId);
        spc.setParameters(params);
        kieSession.execute(spc);

        IterableListenerAssert.assertMultipleVariablesChanged(iterableListener, "element", "x");

        IterableListenerAssert.assertProcessStarted(iterableListener, processId);
        IterableListenerAssert.assertNextNode(iterableListener, "start");
        IterableListenerAssert.assertNextNode(iterableListener, "insertScript");
        IterableListenerAssert.assertNextNode(iterableListener, "fork1");
        IterableListenerAssert.assertNextNode(iterableListener, "script2");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "fork2");
        IterableListenerAssert.assertNextNode(iterableListener, "end1");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }

    /**
     * Exclusive gateway test; two gates have condition expression == true,
     * lower priority number is chosen. "15" > 10 > 1 => gate is chosen
     * according to priority
     */
    @Test(timeout = 30000)
    public void testExclusive2() {
        Assume.assumeFalse(processId.contains("ExclusiveGateway-eclipse"));
        kieSession.addEventListener(iterableListener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", 15);
        Element el = createTestElement("sample", "value", "test");
        params.put("element", el);
        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(processId);
        spc.setParameters(params);
        kieSession.execute(spc);

        IterableListenerAssert.assertMultipleVariablesChanged(iterableListener, "element", "x");
        IterableListenerAssert.assertProcessStarted(iterableListener, processId);
        IterableListenerAssert.assertNextNode(iterableListener, "start");
        IterableListenerAssert.assertNextNode(iterableListener, "insertScript");
        IterableListenerAssert.assertNextNode(iterableListener, "fork1");
        IterableListenerAssert.assertNextNode(iterableListener, "script1");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "fork2");
        IterableListenerAssert.assertNextNode(iterableListener, "end1");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }

    /**
     * Exclusive gateway test; no condition is satisfied, default gate should be
     * taken.
     */
    @Test(timeout = 30000)
    public void testExclusive3() {
        Assume.assumeFalse(processId.contains("ExclusiveGateway-eclipse"));
        kieSession.addEventListener(iterableListener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", -1);
        Element el = createTestElement("sample", "value", "test");
        params.put("element", el);
        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(processId);
        spc.setParameters(params);
        kieSession.execute(spc);

        IterableListenerAssert.assertMultipleVariablesChanged(iterableListener, "element", "x");
        IterableListenerAssert.assertProcessStarted(iterableListener, processId);
        IterableListenerAssert.assertNextNode(iterableListener, "start");
        IterableListenerAssert.assertNextNode(iterableListener, "insertScript");
        IterableListenerAssert.assertNextNode(iterableListener, "fork1");
        IterableListenerAssert.assertNextNode(iterableListener, "script3");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "fork2");
        IterableListenerAssert.assertNextNode(iterableListener, "end1");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }

    /**
     * Simpler exclusive gateway scenario for eclipse editor. Only one gate has
     * condition expression == true.
     *
     * Eclipse editor does not support default gate, XPath condition expression
     */
    @Test(timeout = 30000)
    public void testExclusiveEclipse1() {
        Assume.assumeTrue(processId.contains("ExclusiveGateway-eclipse"));
        kieSession.addEventListener(iterableListener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("x", 5);

        StartProcessCommand spc = new StartProcessCommand();
        spc.setProcessId(processId);
        spc.setParameters(params);
        kieSession.execute(spc);

        IterableListenerAssert.assertChangedVariable(iterableListener, "x", null, 5);
        IterableListenerAssert.assertProcessStarted(iterableListener, processId);
        IterableListenerAssert.assertNextNode(iterableListener, "start");
        IterableListenerAssert.assertNextNode(iterableListener, "insertScript");
        IterableListenerAssert.assertNextNode(iterableListener, "fork");
        IterableListenerAssert.assertNextNode(iterableListener, "script2");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "end");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }

    /**
     * Simpler exclusive gateway scenario for eclipse editor. Two gates have
     * condition expression == true, lower priority number is chosen.
     *
     * Eclipse editor does not support default gate, XPath condition expression
     */
    @Test(timeout = 30000)
    public void testExclusiveEclipse2() {
        Assume.assumeTrue(processId.contains("ExclusiveGateway-eclipse"));
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
        IterableListenerAssert.assertNextNode(iterableListener, "insertScript");
        IterableListenerAssert.assertNextNode(iterableListener, "fork");
        IterableListenerAssert.assertNextNode(iterableListener, "script1");
        IterableListenerAssert.assertNextNode(iterableListener, "join");
        IterableListenerAssert.assertNextNode(iterableListener, "end");
        IterableListenerAssert.assertProcessCompleted(iterableListener, processId);
    }

    /**
     * Creates testing element with attribute.
     *
     * @param name
     *            name
     * @param attribute
     *            attribute name
     * @param attrValue
     *            attribute value
     */
    private Element createTestElement(String name, String attribute, String attrValue) {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        Attr attr = doc.createAttribute(attribute);
        attr.setValue(attrValue);

        Element element = doc.createElement(name);
        element.setAttributeNode(attr);

        return element;
    }

}
