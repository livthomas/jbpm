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

import java.util.List;

import org.jbpm.services.task.admin.listener.TaskCleanUpProcessEventListener;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.test.JbpmTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;

public class HumanTaskCleanUpEarlyFlushTest extends JbpmTestCase {

    private static final String SUBPROCESS_PARENT =
            "org/jbpm/test/regression/task/HumanTaskCleanUpEarlyFlush-subprocess-parent.bpmn2";
    private static final String SUBPROCESS_PARENT_ID =
            "org.jbpm.test.regression.task.HumanTaskCleanUpEarlyFlush-subprocess-parent";
    private static final String SUBPROCESS_CHILD1 =
            "org/jbpm/test/regression/task/HumanTaskCleanUpEarlyFlush-subprocess-child1.bpmn2";
    private static final String SUBPROCESS_CHILD2 =
            "org/jbpm/test/regression/task/HumanTaskCleanUpEarlyFlush-subprocess-child2.bpmn2";

    private static final String SIGNAL_SENDER =
            "org/jbpm/test/regression/task/HumanTaskCleanUpEarlyFlush-signal-sender.bpmn2";
    private static final String SIGNAL_SENDER_ID =
            "org.jbpm.test.regression.task.HumanTaskCleanUpEarlyFlush-signal-sender";
    private static final String SIGNAL_RECEIVER =
            "org/jbpm/test/regression/task/HumanTaskCleanUpEarlyFlush-signal-receiver.bpmn2";

    /**
     * Bug 1128377 - Parent process does not get notified after sub process completes
     * Bug 1177736 - Process Execution stops after subprocess if main process and subprocess contains Human task
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1128377">Bug 1128377</a>
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1177736">Bug 1177736</a>
     */
    @Test
    public void testSubprocess() {
        createRuntimeManager(SUBPROCESS_PARENT, SUBPROCESS_CHILD1, SUBPROCESS_CHILD2);

        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        TaskService taskService = engine.getTaskService();

        ksession.addEventListener(new TaskCleanUpProcessEventListener(taskService));

        ProcessInstance processInstance = ksession.startProcess(SUBPROCESS_PARENT_ID);

        for (int i = 0; i < 2; ++i) {
            List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
            assertEquals(1, tasks.size());
            long taskId = tasks.get(0).getId();
            taskService.start(taskId, "john");
            taskService.complete(taskId, "john", null);
        }

        assertProcessInstanceCompleted(processInstance.getId());
    }

    /**
     * Bug 1165466 - java.lang.IllegalArgumentException: Unknown node instance type:
     * org.jbpm.workflow.instance.node.ActionNodeInstance with intermediate signal event
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1165466">Bug 1165466</a>
     */
    private void testSignal(JbpmJUnitBaseTestCase.Strategy strategy) {
        createRuntimeManager(strategy, null, SIGNAL_SENDER, SIGNAL_RECEIVER);

        RuntimeEngine engine = getRuntimeEngine();
        KieSession ksession = engine.getKieSession();
        TaskService taskService = engine.getTaskService();

        ksession.addEventListener(new TaskCleanUpProcessEventListener(taskService));

        ksession.startProcess(SIGNAL_SENDER_ID);
    }

    @Test
    public void testSignalSingleton() {
        testSignal(JbpmJUnitBaseTestCase.Strategy.SINGLETON);
    }

    @Test
    public void testSignalPerRequest() {
        testSignal(JbpmJUnitBaseTestCase.Strategy.REQUEST);
    }

    @Test
    public void testSignalPerProcessInstance() {
        testSignal(JbpmJUnitBaseTestCase.Strategy.PROCESS_INSTANCE);
    }

}
