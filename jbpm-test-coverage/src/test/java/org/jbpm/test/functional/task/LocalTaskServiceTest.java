package org.jbpm.test.functional.task;

import org.jbpm.services.task.exception.PermissionDeniedException;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.services.task.wih.util.LocalHTWorkItemHandlerUtil;
import org.jbpm.test.JbpmTestCase;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;

import java.util.List;

public class LocalTaskServiceTest extends JbpmTestCase {
    private static final String HUMAN_TASK = "org/jbpm/test/functional/common/Common-humanTask.bpmn2";
    private static final String HUMAN_TASK_ID = "org.jbpm.test.functional.common.Common-humanTask";
    private static final String USER_GROUP_RES = "classpath:/usergroups.properties";
    private static final String BUSINESS_ADMINISTRATOR = "Administrator"; // member of Administrators group from properties

    private KieSession kieSession;
    private TaskService taskService;

    public LocalTaskServiceTest() {
        super(true, true);
    }

    @Before
    public void init() throws Exception {
        createRuntimeManager(HUMAN_TASK);
        RuntimeEngine re = getRuntimeEngine();
        kieSession = re.getKieSession();
        taskService = LocalHTWorkItemHandlerUtil.registerLocalHTWorkItemHandler(kieSession, getEmf(),
                new JBossUserGroupCallbackImpl(USER_GROUP_RES));
    }

    @Test
    public void executeTaskCompleteTest() {
        ProcessInstance processInstance = kieSession.startProcess(HUMAN_TASK_ID);

        assertProcessInstanceActive(processInstance.getId());
        assertNodeTriggered(processInstance.getId(), "start", "user task");

        List<TaskSummary> list = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        TaskSummary task = list.get(0);
        taskService.start(task.getId(), "john");
        taskService.complete(task.getId(), "john", null);

        assertNodeTriggered(processInstance.getId(), "end");
        assertProcessInstanceCompleted(processInstance.getId());
    }

    /**
     * Mary should not have permissions to delegate a task.
     */
    @Test(expected = PermissionDeniedException.class)
    public void executeTaskDelegationTest() {
        ProcessInstance processInstance = kieSession.startProcess(HUMAN_TASK_ID);

        assertProcessInstanceActive(processInstance.getId());
        assertNodeTriggered(processInstance.getId(), "start", "user task");

        List<TaskSummary> list = taskService.getTasksAssignedAsBusinessAdministrator(BUSINESS_ADMINISTRATOR, "en-UK");
        TaskSummary task = list.get(0);
        delegateTask(task, "mary", "doctor");
    }

    @Test
    public void executeTaskDelegationByBusinessAdministratorTest() {
        ProcessInstance processInstance = kieSession.startProcess(HUMAN_TASK_ID);

        assertProcessInstanceActive(processInstance.getId());
        assertNodeTriggered(processInstance.getId(), "start", "user task");

        List<TaskSummary> list = taskService.getTasksAssignedAsBusinessAdministrator(BUSINESS_ADMINISTRATOR, "en-UK");
        TaskSummary task = list.get(0);
        delegateTask(task, BUSINESS_ADMINISTRATOR, "doctor");

        assertNodeTriggered(processInstance.getId(), "end");
        assertProcessInstanceCompleted(processInstance.getId());
    }

    private void delegateTask(final TaskSummary task, final String businessAdministrator, final String delegateTo) {
        taskService.delegate(task.getId(), businessAdministrator, delegateTo);
        taskService.start(task.getId(), delegateTo);
        taskService.complete(task.getId(), delegateTo, null);
    }
}
