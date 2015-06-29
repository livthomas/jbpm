package org.jbpm.test.functional.wih;


import org.assertj.core.api.Assertions;
import org.jbpm.process.workitem.webservice.WebServiceWorkItemHandler;
import org.jbpm.test.JbpmTestCase;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkflowProcessInstance;

import java.util.HashMap;
import java.util.Map;

/**
 * Web services test, basic support for invoking web services.
 *
 * BRMS-BPM-21
 */
public class WebServicesTest extends JbpmTestCase {
    private static final String WEB_SERVICE_INVOCATION = "org/jbpm/test/functional/wih/WebServices-invocation.bpmn";
    private static final String WEB_SERVICES_INVOCATION_ID = "org.jbpm.test.functional.wih.WebServices-invocation";
    private static final String WEB_SERVICES_ECHO = "http://sepro-ibek.itos.redhat.com/EchoService?wsdl";
    private KieSession kieSession;

    public WebServicesTest() {
        super(false);
    }

    @Before
    public void init() throws Exception {
        kieSession = createKSession(WEB_SERVICE_INVOCATION);
    }

    @Test(timeout = 60000)
    public void testSimplePublicWebService() throws Exception {
        kieSession.getWorkItemManager().registerWorkItemHandler("WebService", new WebServiceWorkItemHandler(kieSession));

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("url", WEB_SERVICES_ECHO);
        params.put("namespace", "http://sepro.jboss.org");
        params.put("interface", "EchoService");
        params.put("operation", "echo");
        params.put("parameters", new String[]{"Echo message"});

        ProcessInstance processInstance = kieSession.startProcess(WEB_SERVICES_INVOCATION_ID, params);
        long pid = processInstance.getId();
        WorkflowProcessInstance wpi = (WorkflowProcessInstance) processInstance;
        String result = (String) wpi.getVariable("result");
        System.out.println("result : " + result);
        Assertions.assertThat(result).isEqualTo("Echo message");
        // assert that process instance is completed
        assertProcessInstanceNotActive(pid, kieSession);
    }

}