/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.test.regression.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;

import static org.jbpm.test.tools.TrackingListenerAssert.assertTriggered;
import static org.jbpm.test.tools.TrackingListenerAssert.assertTriggeredAndLeft;

public class HumanTaskTest extends JbpmTestCase {

    private static final String BOUNDARY_TIMER = "org/jbpm/test/regression/task/HumanTask-boundaryTimer.bpmn2";
    private static final String BOUNDARY_TIMER_ID = "org.jbpm.test.regression.task.HumanTask-boundaryTimer";

    private static final String COMPLETION_ROLLBACK = "org/jbpm/test/regression/task/HumanTask-completionRollback.bpmn2";
    private static final String COMPLETION_ROLLBACK_ID = "org.jbpm.test.regression.task.HumanTask-completionRollback";

    private static final String ON_ENTRY_SCRIPT_EXCEPTION = "org/jbpm/test/regression/task/HumanTask-onEntryScriptException.bpmn2";
    private static final String ON_ENTRY_SCRIPT_EXCEPTION_ID = "org.jbpm.test.regression.task.HumanTask-onEntryScriptException";

    private static final String ON_EXIT_SCRIPT_EXCEPTION = "org/jbpm/test/regression/task/HumanTask-onExitScriptException.bpmn2";
    private static final String ON_EXIT_SCRIPT_EXCEPTION_ID = "org.jbpm.test.regression.task.HumanTask-onExitScriptException";

    private static final String ABORT_WORKITEM_TASK_STATUS = "org/jbpm/test/regression/task/HumanTask-abortWorkItemTaskStatus.bpmn2";
    private static final String ABORT_WORKITEM_TASK_STATUS_ID = "org.jbpm.test.regression.task.HumanTask-abortWorkItemTaskStatus";

    private static final String LOCALE = "org/jbpm/test/regression/task/HumanTask-locale.bpmn2";
    private static final String LOCALE_ID = "org.jbpm.test.regression.task.HumanTask-locale";

    private static final String INPUT_TRANSFORMATION = "org/jbpm/test/regression/task/HumanTask-inputTransformation.bpmn2";
    private static final String INPUT_TRANSFORMATION_ID = "org.jbpm.test.regression.task.HumanTask-inputTransformation";

    /**
     * Bug 958397 - Add a Boundary Timer to a Task
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=958397">Bug 958397</a>
     */
    @Test
    public void testBoundaryTimer() throws InterruptedException {
        createRuntimeManager(BOUNDARY_TIMER);
        KieSession ksession = getRuntimeEngine().getKieSession();
        TaskService taskService = getRuntimeEngine().getTaskService();

        TrackingProcessEventListener tpel = new TrackingProcessEventListener();
        ksession.addEventListener(tpel);
        ProcessInstance pi = ksession.startProcess(BOUNDARY_TIMER_ID);

        // wait for timer
        Thread.sleep(2000);

        assertTriggeredAndLeft(tpel, "Script1");
        assertTriggered(tpel, "End1");

        long taskId = taskService.getTasksByProcessInstanceId(pi.getId()).get(0);
        taskService.start(taskId, "john");
        taskService.complete(taskId, "john", null);

        assertTriggeredAndLeft(tpel, "Script2");
        assertTriggered(tpel, "End2");

        assertProcessInstanceCompleted(pi.getId());
    }

    /**
     * Bug 1004681 - Task completion is not rolled back when Process Engine throws a Exception
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1004681">Bug 1004681</a>
     */
    @Test
    public void testCompletionRollback() {
        createRuntimeManager(COMPLETION_ROLLBACK);
        TaskService taskService = getRuntimeEngine().getTaskService();
        KieSession ksession = getRuntimeEngine().getKieSession();

        ProcessInstance pi = ksession.startProcess(COMPLETION_ROLLBACK_ID);
        logger.info("Process with id = " + pi.getId() + " has just been started.");

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        long taskId = taskList.get(0).getId();
        taskService.start(taskId, "john");

        Task task = taskService.getTaskById(taskId);
        logger.info("Actual task status: " + task.getTaskData().getStatus());

        try {
            taskService.complete(taskId, "john", null);
            Assertions.fail("Exception should have been thrown from the process script task.");
        } catch (Exception ex) {
            // exception thrown in process script task is intentional
        }
        disposeRuntimeManager();

        createRuntimeManager(COMPLETION_ROLLBACK);
        taskService = getRuntimeEngine().getTaskService();
        Status status = taskService.getTaskById(taskId).getTaskData().getStatus();
        Assertions.assertThat(status).as("Task completion has not been rolled back!").isEqualTo(Status.InProgress);
    }

    /**
     * Bug 1120122 - Unexpected behavior when exception is thrown in the On Exit/Entry Script
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1120122">Bug 1120122</a>
     */
    @Test
    public void testOnEntryScriptException() {
        createRuntimeManager(ON_ENTRY_SCRIPT_EXCEPTION);
        KieSession ksession = getRuntimeEngine().getKieSession();
        TaskService taskService = getRuntimeEngine().getTaskService();

        long pid = ksession.startProcess(ON_ENTRY_SCRIPT_EXCEPTION_ID).getId();

        List<Long> tasks = taskService.getTasksByProcessInstanceId(pid);
        Assertions.assertThat(tasks).hasSize(1);

        Task task = taskService.getTaskById(tasks.get(0));
        Assertions.assertThat(task.getNames().get(0).getText()).isEqualTo("Human task 2");
    }

    /**
     * Bug 1120122 - Unexpected behavior when exception is thrown in the On Exit/Entry Script
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1120122">Bug 1120122</a>
     */
    @Test
    public void testOnExitScriptException() {
        createRuntimeManager(ON_EXIT_SCRIPT_EXCEPTION);
        KieSession ksession = getRuntimeEngine().getKieSession();
        TaskService taskService = getRuntimeEngine().getTaskService();

        long pid = ksession.startProcess(ON_EXIT_SCRIPT_EXCEPTION_ID).getId();

        List<Long> tasks = taskService.getTasksByProcessInstanceId(pid);
        Assertions.assertThat(tasks).hasSize(1);
        long taskId = tasks.get(0);

        taskService.start(taskId, "john");
        taskService.complete(taskId, "john", null);

        tasks = taskService.getTasksByProcessInstanceId(pid);
        Assertions.assertThat(tasks).hasSize(2);

        Task task1 = taskService.getTaskById(Math.min(tasks.get(0), tasks.get(1)));
        Assertions.assertThat(task1.getNames().get(0).getText()).isEqualTo("Human task 1");
        Assertions.assertThat(task1.getTaskData().getStatus()).isEqualTo(Status.Completed);

        Task task2 = taskService.getTaskById(Math.max(tasks.get(0), tasks.get(1)));
        Assertions.assertThat(task2.getNames().get(0).getText()).isEqualTo("Human task 2");
        Assertions.assertThat(task2.getTaskData().getStatus()).isEqualTo(Status.Reserved);
    }

    /**
     * Bug 1145046 - Task doesn't exit on abortWorkItem in one transaction with Oracle
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1145046">Bug 1145046</a>
     */
    @Test
    public void testAbortWorkItemTaskStatus() {
        for (int i = 0; i < 5; i++) {
            createRuntimeManager(Strategy.PROCESS_INSTANCE, "abortWorkItemTaskStatus" + i, ABORT_WORKITEM_TASK_STATUS);
            RuntimeEngine runtime = getRuntimeEngine();
            KieSession ksession = runtime.getKieSession();

            Map<String, Object> params = new HashMap<String, Object>();
            ProcessInstance pi = ksession.startProcess(ABORT_WORKITEM_TASK_STATUS_ID, params);

            TaskService taskService = runtime.getTaskService();
            List<Long> list = taskService.getTasksByProcessInstanceId(pi.getId());
            for (long taskId : list) {
                Task task = taskService.getTaskById(taskId);
                Assertions.assertThat(task.getTaskData().getStatus()).isEqualTo(Status.Exited);
            }
            disposeRuntimeManager();
        }
    }

    /**
     * Bug 1139496 - Creating a process instance always creates tasks with language set to en-UK
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1139496">Bug 1139496</a>
     */
    @Test
    public void testLocale() {
        KieSession ksession = createKSession(LOCALE);

        ProcessInstance pi = ksession.startProcess(LOCALE_ID);
        RuntimeEngine engine = getRuntimeEngine();
        TaskService taskService = engine.getTaskService();
        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", "ja_JP");
        TaskSummary task = taskList.get(0);
        taskService.start(task.getId(), "john");
        taskService.complete(task.getId(), "john", null);
        EntityManagerFactory emf = getEmf();

        assertProcessInstanceCompleted(pi.getId());
        String language = emf.createEntityManager()
                .createNativeQuery("SELECT language from I18NTEXT WHERE shorttext='空手'")
                .getSingleResult().toString();
        Assertions.assertThat(language).isEqualTo("ja_JP");
    }

    /**
     * Bug 1081508 - Java / MVEL expression is ignored for dataInputAssociation (input parameter mapping)
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1081508">Bug 1081508</a>
     */
    @Test
    public void testInputTransformation() {
        KieSession ksession = createKSession(INPUT_TRANSFORMATION);

        ProcessInstance pi = ksession.startProcess(INPUT_TRANSFORMATION_ID);
        RuntimeEngine engine = getRuntimeEngine();
        TaskService taskService = engine.getTaskService();

        List<TaskSummary> taskList = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        Long taskId = taskList.get(0).getId();

        taskService.start(taskId, "john");

        Map<String, Object> taskById = taskService.getTaskContent(taskId);
        Assertions.assertThat(taskById).containsEntry("Input", "Transformed String");

        taskService.complete(taskId, "john", null);

        assertProcessInstanceCompleted(pi.getId());
    }

}
