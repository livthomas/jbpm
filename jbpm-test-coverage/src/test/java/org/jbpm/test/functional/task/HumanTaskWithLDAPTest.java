package org.jbpm.test.functional.task;


import org.assertj.core.api.Assertions;
import org.jbpm.services.task.identity.LDAPUserGroupCallbackImpl;
import org.jbpm.test.LdapJbpmTestCase;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.Status;

public class HumanTaskWithLDAPTest extends LdapJbpmTestCase {
    private static final String LDAP_HUMAN_TASK = "org/jbpm/test/functional/task/HumanTask-ldap.bpmn2";
    private static final String LDAP_HUMAN_TASK_ID = "org.jbpm.test.functional.task.HumanTask-ldap";
    private static final String LDAP_TASK_LDIF = "src/test/resources/org/jbpm/test/functional/task/HumanTask-task.ldif";

    private KieSession kieSession;
    private TaskService taskService;

    public HumanTaskWithLDAPTest() {
        super(LDAP_TASK_LDIF);
    }

    @Before
    public void init() {
        UserGroupCallback userGroupCallback = new LDAPUserGroupCallbackImpl(createUserGroupCallbackProperties());
        createRuntimeManager(userGroupCallback, LDAP_HUMAN_TASK);
        kieSession = getRuntimeEngine().getKieSession();
        taskService = getRuntimeEngine().getTaskService();
    }

    @Test
    public void testCompleteTask() {
        long pid = kieSession.startProcess(LDAP_HUMAN_TASK_ID).getId();

        long taskId = taskService.getTasksByProcessInstanceId(pid).get(0);

        taskService.start(taskId, "john");
        taskService.complete(taskId, "john", null);

        Status status = taskService.getTaskById(taskId).getTaskData().getStatus();
        Assertions.assertThat(status).isEqualTo(Status.Completed);
    }



}