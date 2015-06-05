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

package org.jbpm.test;

import org.assertj.core.api.Assertions;
import org.jbpm.executor.ExecutorServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.kie.internal.executor.api.ExecutorService;

public class JbpmAsyncJobTestCase extends JbpmTestCase {

    private static final int EXECUTOR_THREADS = 4;
    private static final int EXECUTOR_RETRIES = 3;
    private static final int EXECUTOR_INTERVAL = 1;

    private int executorThreads;
    private int executorRetries;
    private int executorInterval;

    private ExecutorService executorService;

    public JbpmAsyncJobTestCase() {
        this(EXECUTOR_THREADS, EXECUTOR_INTERVAL);
    }

    public JbpmAsyncJobTestCase(int executorRetries) {
        this(EXECUTOR_THREADS, executorRetries, EXECUTOR_INTERVAL);
    }

    public JbpmAsyncJobTestCase(int executorThreads, int executorInterval) {
        this(executorThreads, EXECUTOR_RETRIES, executorInterval);
    }

    public JbpmAsyncJobTestCase(int executorThreads, int executorRetries, int executorInterval) {
        super(true, true);

        this.executorThreads = executorThreads;
        this.executorRetries = executorRetries;
        this.executorInterval = executorInterval;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executorService = getExecutorService();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            executorService.clearAllRequests();
            executorService.clearAllErrors();
            executorService.destroy();
        } finally {
            super.tearDown();
        }
    }

    protected ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = ExecutorServiceFactory.newExecutorService(getEmf());
            executorService.setThreadPoolSize(executorThreads);
            executorService.setRetries(executorRetries);
            executorService.setInterval(executorInterval);
            executorService.init();

            logger.debug("Created ExecutorService with parameters: '" + executorThreads + " threads', '"
                    + executorRetries + " retries', interval '" + executorInterval + "s'");
        }
        return executorService;
    }

    public void assertNodeNotTriggered(long processId, String nodeName) {
        boolean triggered = false;
        try {
            assertNodeTriggered(processId, nodeName);
            triggered = true;
        } catch (AssertionError e) {
            // Assertion passed
        }
        if (triggered) {
            Assertions.fail("Node '" + nodeName + "' was triggered.");
        }
    }

}