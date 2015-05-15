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

package org.jbpm.test.regression.subprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;
import org.jbpm.test.JbpmTestCase;
import org.jbpm.test.listener.TrackingProcessEventListener;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.command.KieCommands;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;

public class EmbeddedSubprocessTest extends JbpmTestCase {

    private static final String INVALID_SUBPROCESS =
            "org/jbpm/test/regression/subprocess/EmbeddedSubprocess-invalidSubprocess.bpmn2";

    private static final String INVALID_SUBPROCESS2 =
            "org/jbpm/test/regression/subprocess/EmbeddedSubprocess-invalidSubprocess2.bpmn2";

    public static final String TERMINATING_END_EVENT =
            "org/jbpm/test/regression/subprocess/EmbeddedSubprocess-terminatingEndEvent.bpmn2";
    public static final String TERMINATING_END_EVENT_ID =
            "org.jbpm.test.regression.subprocess.EmbeddedSubprocess-terminatingEndEvent";

    public static final String TASK_COMPENSATION =
            "org/jbpm/test/regression/subprocess/EmbeddedSubprocess-taskCompensation.bpmn2";
    public static final String TASK_COMPENSATION_ID =
            "org.jbpm.test.regression.subprocess.EmbeddedSubprocess-taskCompensation";


    /**
     * Bug 1139591 - Able to create task nodes without start node and end node in embedded subprocess
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1139591">Bug 1139591</a>
     */
    @Test
    public void testInvalidSubprocess() {
        try {
            createKSession(INVALID_SUBPROCESS);
            Assertions.fail("Process definition is invalid. KieSession should not have been created.");
        } catch (IllegalArgumentException ex) {
            // expected behaviour
        }
    }

    /**
     * Bug 1150226 - jBPM6 allows BPMN2 processes which has no normal outgoing connection from embedded sub-process
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1150226">Bug 1150226</a>
     */
    @Test
    public void testInvalidSubprocess2() {
        try {
            createKSession(INVALID_SUBPROCESS2);
            Assertions.fail("Process definition is invalid. KieSession should not have been created.");
        } catch (IllegalArgumentException ex) {
            // expected behaviour
            ex.printStackTrace();
        }
    }

    /**
     * Bug 851286 - JBPM-3371 Terminate end event in subprocess must not terminate parent process
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=851286">Bug 851286</a>
     */
    @Test
    public void testTerminatingEndEvent() {
        KieSession ksession = createKSession(TERMINATING_END_EVENT);
        TrackingProcessEventListener processEvents = new TrackingProcessEventListener();
        ksession.addEventListener(processEvents);
        List<Command<?>> commands = new ArrayList<Command<?>>();
        commands.add(getCommands().newStartProcess(TERMINATING_END_EVENT_ID));
        ksession.execute(getCommands().newBatchExecution(commands, null));
        Assertions.assertThat(processEvents.wasNodeTriggered("main-script")).isTrue();
        Assertions.assertThat(processEvents.wasNodeTriggered("main-end")).isTrue();
    }

    protected static final KieCommands getCommands() {
        return KieServices.Factory.get().getCommands();
    }

    /**
     * Bug 1191768 - Process with subprocess marked for compensation fails to deploy on BPMS 6.1.0.ER4
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1191768">Bug 1191768</a>
     */
    @Test
    public void testTaskCompensation() throws Exception {
        KieSession kieSession = createKSession(TASK_COMPENSATION);
        kieSession.getWorkItemManager().registerWorkItemHandler("Human Task", new SystemOutWorkItemHandler());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("compensation", "True");
        ProcessInstance processInstance = kieSession.startProcess(TASK_COMPENSATION_ID, params);
        long pid = processInstance.getId();
        assertProcessInstanceCompleted(pid);
        List<? extends VariableInstanceLog> log = getLogService().findVariableInstances(processInstance.getId(),
                "compensation");
        Assertions.assertThat(log.get(log.size() - 1).getValue()).isEqualTo("compensation");
    }

}
