package org.jbpm.test.functional.task;

import org.assertj.core.api.Assertions;
import org.jbpm.runtime.manager.impl.task.SynchronizedTaskService;
import org.jbpm.test.JbpmTestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.query.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HumanTaskQueryFilterTest extends JbpmTestCase {

    private static final Logger logger = LoggerFactory.getLogger(HumanTaskQueryFilterTest.class);
    private static final String CONF_HUMAN_TASK =
            "org/jbpm/test/functional/task/HumanTaskQueryFilter-configurableHumanTask.bpmn2";
    private static final String CONF_HUMAN_TASK_ID =
            "org.jbpm.test.functional.task.HumanTaskQueryFilter-configurableHumanTask";


    private KieSession kieSession;
    private SynchronizedTaskService taskService;
    private List<ProcessInstance> instanceList;

    public HumanTaskQueryFilterTest() {
        super(true, true);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        createRuntimeManager(CONF_HUMAN_TASK);
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        kieSession = runtimeEngine.getKieSession();
        taskService = (SynchronizedTaskService) runtimeEngine.getTaskService();
        instanceList = new ArrayList<ProcessInstance>();
    }

    @Test
    public void testFirstResult() {
        startHumanTaskProcess(6, "john's task", "john");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null,
                new QueryFilter(2, 2, "t.name", true));
        logger.info("### Potential owner task list: " + taskList);
        Assertions.assertThat(taskList).hasSize(2);
        for (int i = 0; i < taskList.size(); i++) {
            Assertions.assertThat(taskList.get(i).getName()).isEqualTo("john's task " + (i + 1 + 2));
        }

        abortHumanTaskProcess(6);
    }

    @Test
    public void testMaxResults() {
        startHumanTaskProcess(4, "john's task", "john");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null,
                new QueryFilter(0, 2, "t.id", true));
        Assertions.assertThat(taskList).hasSize(2);
        logger.info("### Potential owner task list: " + taskList);

        taskList = taskService.getTasksOwned("john", null, new QueryFilter(0, 1, false, null, "en-UK", null));
        Assertions.assertThat(taskList).hasSize(1);
        logger.info("### Owned task list: " + taskList);

        abortHumanTaskProcess(4);
    }

    @Test
    public void testDescendingOrder() {
        startHumanTaskProcess(3, "john's task", "john");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null,
                new QueryFilter(0, 0, "t.name", false));
        logger.info("### Potential owner task list: " + taskList);
        Assertions.assertThat(taskList).hasSize(3);

        for (int i = 0; i < taskList.size(); i++) {
            logger.info("### Task Name: " + taskList.get(i).getName());
            Assertions.assertThat(taskList.get(i).getName()).isEqualTo("john's task " + (3 - i));
        }

        abortHumanTaskProcess(3);
    }

    @Test
    public void testAscendingOrder() {
        startHumanTaskProcess(3, "john's task", "john");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null,
                new QueryFilter(0, 0, "t.name", true));
        logger.info("### Potential owner task list: " + taskList);
        Assertions.assertThat(taskList).hasSize(3);

        for (int i = 0; i < taskList.size(); i++) {
            logger.info("### Task Name: " + taskList.get(i).getName());
            Assertions.assertThat(taskList.get(i).getName()).isEqualTo("john's task " + (i + 1));
        }

        abortHumanTaskProcess(3);
    }

    /**
     * BZ-1132145 - QueryFilter: single result parameter is not used.
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1132145"/>
     */
    @Test
    @Ignore
    public void testSingleResult() {
        startHumanTaskProcess(4, "john's task", "john");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null,
                new QueryFilter(0, 0, true));
        logger.info("### Potential owner task list: " + taskList);
        Assertions.assertThat(taskList).hasSize(1);

        abortHumanTaskProcess(4);
    }

    /**
     * BZ-1132157 - QueryFilter: NPE when Filter Params are provided but params are null.
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1132157"/>
     * TODO - Need to pass in parameters which do make sense.
     */
    @Test
    @Ignore
    public void testFilterParams() {
        startHumanTaskProcess(10, "john's task", "john");

        QueryFilter queryFilter = new QueryFilter("x=1,y=2", null, "t.name", true);

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null, queryFilter);
        logger.info("### Potential owner task list: " + taskList);
        Assertions.assertThat(taskList).hasSize(1);

        abortHumanTaskProcess(10);
    }

    /**
     * BZ-1132444 - CommandBasedTaskService ignoring language parameter.
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1132444"/>
     * CLOSED: For performance reasons language parameter is not used at all when performing queries.
     */
    @Test
    @Ignore
    public void testLanguage() {
        startHumanTaskProcess(1, "GB english man's task", "john", "en-GB");
        startHumanTaskProcess(3, "US english man's task", "john", "en-US");

        QueryFilter queryFilter = new QueryFilter(0, 0, false, null, "en-US", null);
        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", null, null, queryFilter);
        Assertions.assertThat(taskList).hasSize(3);

        for (TaskSummary ts : taskList) {
            Assertions.assertThat(ts.getName()).startsWith("US");
            Assertions.assertThat(ts.getActualOwnerId()).isEqualTo("john");
        }

        abortHumanTaskProcess(4);
    }

    private void startHumanTaskProcess(int instanceCount, String taskName, String assigneeName) {
        startHumanTaskProcess(instanceCount, taskName, assigneeName, "en-UK");
    }

    private void startHumanTaskProcess(int instanceCount, String taskName, String assigneeName, String localeName) {
        for (int i = 0; i < instanceCount; i++) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("assigneeName", assigneeName);
            parameters.put("taskName", taskName + " " + (i + 1));
            parameters.put("localeName", localeName);
            instanceList.add(kieSession.startProcess(CONF_HUMAN_TASK_ID, parameters));
        }
    }

    private void abortHumanTaskProcess(int instanceCount) {
        for (int i = 0; i < instanceCount; i++) {
            kieSession.abortProcessInstance(instanceList.get(i).getId());
        }
        instanceList = instanceList.subList(instanceCount, instanceList.size());
    }

}