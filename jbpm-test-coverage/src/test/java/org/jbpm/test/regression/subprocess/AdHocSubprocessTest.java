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

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.junit.Test;
import qa.tools.ikeeper.annotation.BZ;

public class AdHocSubprocessTest extends JbpmTestCase {

    private static final String EMPTY_COMPLETION_CONDITION =
            "org/jbpm/test/regression/subprocess/AdHocSubprocess-emptyCompletionCondition.bpmn2";

    @Test
    @BZ("1170281")
    public void testEmptyCompletionCondition() {
        try {
            createKSession(EMPTY_COMPLETION_CONDITION);
            Assertions.fail("Process definition should be invalid");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            Assertions.assertThat(ex.getMessage()).contains("no completion condition");
        }
    }

}
