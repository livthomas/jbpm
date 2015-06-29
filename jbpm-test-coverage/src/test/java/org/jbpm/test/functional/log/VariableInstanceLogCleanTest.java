package org.jbpm.test.functional.log;

import org.assertj.core.api.Assertions;
import org.jbpm.test.domain.Person;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.test.JbpmTestCase;
import org.junit.Test;
import org.junit.Ignore;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.audit.VariableInstanceLog;
import org.kie.api.runtime.process.ProcessInstance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableInstanceLogCleanTest extends JbpmTestCase {
    private static final String DATA_OBJECT = "org/jbpm/test/functional/log/VariableInstanceLogClean-dataObject.bpmn";
    private static final String DATA_OBJECT_ID = "org.jbpm.test.functional.log.VariableInstanceLogClean-dataObject";

    private KieSession kieSession;
    private List<ProcessInstance> processInstanceList = new ArrayList<ProcessInstance>();
    private JPAAuditLogService auditService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        auditService = new JPAAuditLogService(getEmf());
        auditService.clear();
    }

    @Override
    public void tearDown() throws Exception {
        try {
            abortProcess(kieSession, processInstanceList);
            auditService.clear();
            auditService.dispose();
        } finally {
            super.tearDown();
        }
    }

    /**
     * TBD - test is failing on the last line - probably a test error
     */
    @Ignore
    @Test
    public void deleteDataObjectLogsByDateRange() throws InterruptedException {
        kieSession = createKSession(DATA_OBJECT);

        Person person = new Person("Marge");
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("person", person);

        Date start = new Date();
        processInstanceList.addAll(startProcess(kieSession, DATA_OBJECT_ID, paramMap, 1));
        Date mid = new Date();
        processInstanceList.addAll(startProcess(kieSession, DATA_OBJECT_ID, paramMap, 2));
        Date end = new Date();
        processInstanceList.addAll(startProcess(kieSession, DATA_OBJECT_ID, paramMap, 1));

        // Delete by date range but only from a part of the instance created in the date range.
        int resultCount = auditService.variableInstanceLogDelete()
                .dateRangeStart(start)
                .dateRangeEnd(mid)
                .build()
                .execute();
        Assertions.assertThat(resultCount).isEqualTo(1);

        // Assert remaining logs - We expect 2 logs to be present - One from instance 3 and one from instance 4
        List<VariableInstanceLog> variableList = auditService.variableInstanceLogQuery()
                .dateRangeStart(mid)
                .dateRangeEnd(end)
                .variableId("person")
                .buildQuery()
                .getResultList();
        Assertions.assertThat(variableList.size()).isEqualTo(2);
        Assertions.assertThat(variableList.get(0).getDate()).isBefore(mid);
        Assertions.assertThat(variableList.get(1).getDate()).isAfter(end);
    }

    /**
     * BZ-TBD - what is the difference between 'queryVariableInstanceLogs' and
     * 'variableInstanceLogQuery'
     * BZ-TBD - Assertion will fail on line 71 where 3 results will be found instead
     * of 2!
     */
    @Ignore
    @Test
    public void deleteDataObjectLogsByDate() {
        kieSession = createKSession(DATA_OBJECT);

        Person person = new Person("Homer");

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("person", person);

        processInstanceList = startProcess(kieSession, DATA_OBJECT_ID, paramMap, 3);
        Assertions.assertThat(processInstanceList).hasSize(3);

        // retrieve person variable of process instance 2 and 3
        List<VariableInstanceLog> variableList = auditService.variableInstanceLogQuery()
                .processInstanceId(processInstanceList.get(1).getId(), processInstanceList.get(2).getId())
                .variableId("person")
                .buildQuery()
                .getResultList();
        Assertions.assertThat(variableList).hasSize(2);

        // delete the variable log which belongs to instance 2 and 3
        int resultCount = auditService.variableInstanceLogDelete()
                .date(variableList.get(0).getDate(), variableList.get(1).getDate())
                .build()
                .execute();
        Assertions.assertThat(resultCount).isEqualTo(2);

        // check what has remained in the database - we expect to find only the person variable
        // belonging to instance 1 as the others where deleted in the previous ste
        variableList = auditService.variableInstanceLogQuery()
                .variableId("person")
                .buildQuery()
                .getResultList();
        Assertions.assertThat(variableList).hasSize(1);
        Assertions.assertThat(variableList.get(0).getValue()).contains("name='Homer'");
    }


    private void abortProcess(KieSession kieSession, List<ProcessInstance> processInstanceList) {
        for (ProcessInstance processInstance : processInstanceList) {
            abortProcess(kieSession, processInstance.getId());
        }
    }

    private void abortProcess(KieSession kieSession, long pid) {
        ProcessInstance processInstance = kieSession.getProcessInstance(pid);
        if (processInstance != null && processInstance.getState() == ProcessInstance.STATE_ACTIVE) {
            kieSession.abortProcessInstance(pid);
        }
    }

    private List<ProcessInstance> startProcess(KieSession kieSession, String processId, int count) {
        List<ProcessInstance> piList = new ArrayList<ProcessInstance>();
        for (int i = 0; i < count; i++) {
            ProcessInstance pi = kieSession.startProcess(processId);
            if (pi != null) {
                piList.add(pi);
            }
        }
        return piList;
    }

    public List<ProcessInstance> startProcess(KieSession kieSession, String processId, Map<String, Object> parameters, int count) {
        List<ProcessInstance> processInstanceList = new ArrayList<ProcessInstance>();
        for (int i = 0; i < count; i++) {
            ProcessInstance processInstance = kieSession.startProcess(processId, parameters);
            processInstanceList.add(processInstance);
        }
        return processInstanceList;
    }
}