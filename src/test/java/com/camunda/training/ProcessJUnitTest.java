package com.camunda.training;

import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

    @BeforeEach
    public void setUp() {
        TwitterService twitterService = Mockito.mock(TwitterService.class);
        Mocks.register("createTweetDelegate", new CreateTweetDelegate(twitterService));
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testHappyPath() {

        Map<String, Object> variables = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables);

        assertThat(processInstance).isWaitingAt("Activity_TweetBewerten");

        Task task = task("Activity_TweetBewerten");
        assertThat(task).isNotNull();

        Map<String, Object> taskVariables = Map.of(ProcessInstanceConstants.VAR_APPROVED, true);
        taskService().complete(task.getId(), taskVariables);

        managementService().executeJob(job().getId());

        assertThat(processInstance).isEnded().hasPassed("Event_TweetVeroffentlicht");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testRejectionPath() {
        Map<String, Object> variables = Map.of(
                ProcessInstanceConstants.VAR_CONTENT, "Abgelehnter Tweet",
                ProcessInstanceConstants.VAR_APPROVED, false);


        ProcessInstance processInstance = runtimeService()
                .createProcessInstanceByKey("Process_TwitterQA")
                .setVariables(variables)
                .startAfterActivity("Activity_TweetBewerten")
                .execute();

        assertThat(processInstance)
                .isWaitingAt("Activity_MitarbeiterBenachrichtigen")
                .externalTask()
                .hasTopicName("MitarbeiterBenachrichtigen");

        complete(externalTask());

        assertThat(processInstance).isEnded().hasPassed("Event_TweetAbgelehnt");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testWithdrawTweet() {


        Map<String, Object> variables = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!");

        Map<String, Object> variables2 = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein anderer Tweet!");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables);

        ProcessInstance processInstance2 = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables2);

        assertThat(processInstance).isWaitingAt("Activity_TweetBewerten");



        runtimeService().createMessageCorrelation("Message_WithdrawTweet")
                .processInstanceVariableEquals(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!")
                .correlateWithResult();

       assertThat(processInstance).isEnded().hasPassed("EndEvent_TweetWithdrawn");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testTweetTimeout() {
        Map<String, Object> variables = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables);

        assertThat(processInstance).isWaitingAt("Activity_TweetBewerten");

        managementService().executeJob(job().getId());

        assertThat(processInstance).isEnded().hasPassed("Event_ReviewTimedOut");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testSignalReceived() {
        Map<String, Object> variables = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!");
        Map<String, Object> variables2 = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein anderer Tweet!");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables);

        ProcessInstance processInstance2 = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables2);

        assertThat(processInstance).isWaitingAt("Activity_TweetBewerten");
        assertThat(processInstance2).isWaitingAt("Activity_TweetBewerten");

        runtimeService().createSignalEvent("Signal_BadNewsReceived").send();

        assertThat(processInstance).isEnded().hasPassed("EndEvent_CancelEverything");
        assertThat(processInstance2).isEnded().hasPassed("EndEvent_CancelEverything");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testSuperUserTweet() {
        ProcessInstance processInstance = runtimeService()
                .createMessageCorrelation("Message_SuperUserTweet")
                .setVariables(Map.of(ProcessInstanceConstants.VAR_CONTENT, "Superuser Tweet!"))
                .correlateStartMessage();

        assertThat(processInstance).isStarted().isWaitingAt("Activity_TweetVeroeffentlichen");
        assertThat(processInstance).variables()
                .containsEntry(ProcessInstanceConstants.VAR_CONTENT, "Superuser Tweet!");
    }

    @Test
    @Deployment(resources = "TwitterQA.bpmn")
    public void testDuplicateStatusError() {

        Mocks.register("createTweetDelegate", (JavaDelegate) execution -> {
            throw new BpmnError("TWT_DUPLICATE", "Status is a duplicate");
        });

        Map<String, Object> variables = Map.of(ProcessInstanceConstants.VAR_CONTENT, "Mein Tweet!");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQA", variables);

        assertThat(processInstance).isWaitingAt("Activity_TweetBewerten");

        Task task = task("Activity_TweetBewerten");
        assertThat(task).isNotNull();

        Map<String, Object> taskVariables = Map.of(ProcessInstanceConstants.VAR_APPROVED, true);
        taskService().complete(task.getId(), taskVariables);

        managementService().executeJob(job().getId());

        assertThat(processInstance).isWaitingAt("Activity_AdaptTweet");
    }

    @Test
    @Deployment(resources = "TweetApproval.dmn")
    public void testTweetFromJakob() {
        DmnDecisionResult result =
        decisionService()
                .evaluateDecisionByKey("Decision_TweetApproval")
                .variables(Map.of(
                        "email", "jakob.freund@camunda.com",
                        "content", "Camunda ist schon ganz okay"))
                .evaluate();

        boolean isApproved = result.getSingleResult().getSingleEntry();

        assertThat(isApproved);
    }

    @Test
    @Deployment(resources = {"TwitterQAAutomated.bpmn", "TweetApproval.dmn"})
    public void testAutomatedHappyPath() {

        Map<String, Object> variables = Map.of(
                ProcessInstanceConstants.VAR_CONTENT, "camunda rocks",
                ProcessInstanceConstants.VAR_EMAIL, "tim.zoeller@example.com");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("Process_TwitterQAAutomated", variables);

        assertThat(processInstance).isStarted().isWaitingAt("Activity_TweetVeroeffentlichen");

        managementService().executeJob(job().getId());

        assertThat(processInstance).isEnded().hasPassed("Event_TweetVeroffentlicht");
    }


}