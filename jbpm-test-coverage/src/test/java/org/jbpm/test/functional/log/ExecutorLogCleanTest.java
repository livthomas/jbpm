package org.jbpm.test.functional.log;

import org.assertj.core.api.Assertions;
import org.jbpm.executor.impl.jpa.ExecutorJPAAuditService;
import org.jbpm.executor.impl.wih.AsyncWorkItemHandler;
import org.jbpm.test.JbpmAsyncJobTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.executor.api.ErrorInfo;
import org.kie.internal.executor.api.STATUS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutorLogCleanTest extends JbpmAsyncJobTestCase {
    private static final String ASYNC_DATA_EXEC =  "org/jbpm/test/functional/common/Common-asyncDataExecutor.bpmn2";
    private static final String ASYNC_DATA_EXEC_ID = "org.jbpm.test.functional.common.Common-asyncDataExecutor";
    private static final String USER_COMMAND_ID = "org.jbpm.test.jobexecutor.UserCommand";
    private static final String USER_FAILING_COMMAND_ID =  "org.jbpm.test.jobexecutor.UserFailingCommand";
    private static final int EXECUTOR_RETRIES = 1;

    private ExecutorJPAAuditService auditService;

    public ExecutorLogCleanTest() {
        super(EXECUTOR_RETRIES);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        auditService = new ExecutorJPAAuditService(getEmf());
        auditService.clear();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            auditService.clear();
            auditService.dispose();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void deleteInfoLogsByStatus() throws Exception {
        KieSession kieSession = createKSession(ASYNC_DATA_EXEC);
        WorkItemManager wim = kieSession.getWorkItemManager();
        wim.registerWorkItemHandler("async", new AsyncWorkItemHandler(getExecutorService()));

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("command", USER_COMMAND_ID);
        ProcessInstance pi = kieSession.startProcess(ASYNC_DATA_EXEC_ID, pm);

        // Wait for the job to complete
        Thread.sleep(10 * 1000);

        // Assert comletion of the job
        Assertions.assertThat(getExecutorService().getCompletedRequests()).hasSize(1);

        // Delete a record
        int resultCount = auditService.requestInfoLogDeleteBuilder()
                .status(STATUS.DONE)
                .build()
                .execute();
        Assertions.assertThat(resultCount).isEqualTo(1);

        // Assert remaining records
        Assertions.assertThat(getExecutorService().getCompletedRequests()).hasSize(0);
    }

    /**
     * Bug 1188702 - AuditService: data parameters handled internally as timestamp.
     *
     * @see  <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1188702"/>
     */
    @Test
    public void deleteErrorLogsByDate() throws Exception {
        KieSession ksession = createKSession(ASYNC_DATA_EXEC);
        WorkItemManager wim = ksession.getWorkItemManager();
        wim.registerWorkItemHandler("async", new AsyncWorkItemHandler(getExecutorService()));

        Map<String, Object> pm = new HashMap<String, Object>();
        pm.put("command", USER_FAILING_COMMAND_ID);
        ProcessInstance pi = ksession.startProcess(ASYNC_DATA_EXEC_ID, pm);

        // Wait for the all retries to fail
        Thread.sleep(10 * 1000);

        // Assert comletion of the job
        List<ErrorInfo> errorList = getExecutorService().getAllErrors();
        Assertions.assertThat(errorList).hasSize(2);

        // Delete a record
        int resultCount = auditService.errorInfoLogDeleteBuilder()
                .date(errorList.get(0).getTime())
                .build()
                .execute();
        Assertions.assertThat(resultCount).isEqualTo(1);

        // Assert remaining records
        Assertions.assertThat(getExecutorService().getAllErrors()).hasSize(1);

        // Abort running process instance
        ksession.abortProcessInstance(pi.getId());
    }
}
