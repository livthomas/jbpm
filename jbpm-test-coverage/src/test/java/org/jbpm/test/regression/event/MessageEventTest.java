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

package org.jbpm.test.regression.event;

import org.jbpm.test.JbpmTestCase;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;

public class MessageEventTest extends JbpmTestCase {

    private static final String MULTIPLE_SIMPLE =
            "org/jbpm/test/regression/event/MessageEvent-multipleSimple.bpmn2";
    private static final String MULTIPLE_SIMPLE_ID =
            "org.jbpm.test.regression.event.MessageEvent-multipleSimple";

    private static final String MULTIPLE_SUBPROCESS =
            "org/jbpm/test/regression/event/MessageEvent-multipleSubprocess.bpmn2";
    private static final String MULTIPLE_SUBPROCESS_ID =
            "org.jbpm.test.regression.event.MessageEvent-multipleSubprocess";

    /**
     * Bug 1163864 - Multiple Intermediate Message Events with same signal id remain active at the same time in jBPM6
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1163864">Bug 1163864</a>
     */
    @Test
    public void testMultipleIntermediateMessageEventsSimpleProcess() {
        KieSession ksession = createKSession(MULTIPLE_SIMPLE);
        ProcessInstance pi = ksession.startProcess(MULTIPLE_SIMPLE_ID);

        ksession.signalEvent("Message-continue", null);

        assertProcessInstanceActive(pi.getId());
    }

    /**
     * Bug 1163864 - Multiple Intermediate Message Events with same signal id remain active at the same time in jBPM6
     *
     * @see <a href="https://bugzilla.redhat.com/show_bug.cgi?id=1163864">Bug 1163864</a>
     */
    @Test
    public void testMultipleIntermediateMessageEventsEmbeddedSubProcess() {
        KieSession ksession = createKSession(MULTIPLE_SUBPROCESS);
        ProcessInstance pi = ksession.startProcess(MULTIPLE_SUBPROCESS_ID);

        ksession.signalEvent("Message-continue", null);

        assertProcessInstanceActive(pi.getId());
    }

}
