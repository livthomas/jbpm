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

import java.io.FileInputStream;
import java.util.Properties;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.kie.api.runtime.KieSession;

public abstract class JbpmTestCase extends JbpmJUnitBaseTestCase {

    public JbpmTestCase() {
        this(true);
    }

    public JbpmTestCase(boolean persistence) {
        this(persistence, persistence);
    }

    public JbpmTestCase(boolean setupDataSource, boolean sessionPersistence) {
        this(setupDataSource, sessionPersistence, "org.jbpm.persistence.jpa");
    }

    public JbpmTestCase(boolean setupDataSource, boolean sessionPersistence, String persistenceUnit) {
        super(setupDataSource, sessionPersistence, persistenceUnit);
    }

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            System.out.println(" >>> " + description.getMethodName() + " <<< ");
        }

        @Override
        protected void finished(Description description) {
            System.out.println();
        }

    };

    @Override
    protected PoolingDataSource setupPoolingDataSource() {
        if (!"remote".equals(System.getProperty("db"))) {
            return super.setupPoolingDataSource();
        }

        PoolingDataSource pds = new PoolingDataSource();
        pds.setUniqueName("jdbc/jbpm-ds");
        pds.setMinPoolSize(5);
        pds.setMaxPoolSize(15);
        pds.setAllowLocalTransactions(true);

        Properties dbProps = new Properties();
        String dbPropsPath = "allocated.db.properties";
        if (System.getenv("WORKSPACE") != null) {
            dbPropsPath = System.getenv("WORKSPACE") + "/" + dbPropsPath;
        }
        if (System.getProperty("WORKSPACE") != null) {
            dbPropsPath = System.getenv("WORKSPACE") + "/" + dbPropsPath;
        }

        try {
            dbProps.load(new FileInputStream(dbPropsPath));
            String dbLabel = dbProps.getProperty("db.primary_label");
            System.out.println("Tests will use " + dbLabel);

            pds.setClassName(dbProps.getProperty("datasource.class.xa"));

            Properties pdsp = pds.getDriverProperties();
            pdsp.put("user", dbProps.getProperty("db.username"));
            pdsp.put("password", dbProps.getProperty("db.password"));
            pdsp.put("serverName", dbProps.getProperty("db.hostname"));
            pdsp.put("portNumber", dbProps.getProperty("db.port"));
            pdsp.put("databaseName", dbProps.getProperty("db.name"));

            // DB-specific settings
            if (dbLabel.startsWith("db2")) {
                pdsp.put("driverType", 4);
            } else if (dbLabel.startsWith("oracle")) {
                pdsp.put("driverType", "thin");
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        pds.init();

        return pds;
    }

    public KieSession createKSession(String... process) {
        createRuntimeManager(process);
        return getRuntimeEngine().getKieSession();
    }

    public void assertProcessInstanceNeverRun(long processId) {
        Assertions.assertThat(getLogService().findProcessInstance(processId)).as("Process has been running").isNull();
    }

}

