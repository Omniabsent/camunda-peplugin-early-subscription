package hpi.bpt.earlysubscription.camunda.engineplugin;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.init;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.processEngine;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.junit.Assert.assertTrue;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test case starting an in-memory database-backed Process Engine.
 */
public class InMemoryH2Test {

	@ClassRule
	@Rule
	public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

	private static final String PROCESS_DEFINITION_KEY = "earlysubscription.camunda.engineplugin";

	static {
		LogFactory.useSlf4jLogging(); // MyBatis
	}

	@Before
	public void setup() {
		init(rule.getProcessEngine());
	}

	@Test
	@Deployment(resources = "message-without-extension.bpmn")
	public void testDeploymentMessageWithoutExtension() {
		// do nothing, just test the deployment
	}

	@Test
	@Deployment(resources = "process.bpmn")
	public void testBigProcess() {

	}

	@Test
	@Deployment(resources = "subscribe-on-deployment.bpmn")
	public void testOnDeployment() throws InterruptedException {
		// check that subscriptionEngine queryRepo has 1 entry
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onDeployment");

		Thread.sleep(2500); // wait for the async subscription to finish
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		processEngine().getRuntimeService().correlateMessage("Message_OnDeployment");
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);

		assertThat(processInstance).isEnded();

		// clean up for queries with subscr. on deployment
		SubscriptionEngine.queryRepository.clear();
	}

	@Test
	@Deployment(resources = "subscribe-on-instantiation.bpmn")
	public void testOnInstantiation() {

		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onInstantiation");

		assertThat(processInstance).task("Task_1um2wc3");
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		complete(task()); // complete the user task

		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		processEngine().getRuntimeService().correlateMessage("Message_OnInstantiation");
		assertThat(processInstance).isEnded();
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);

	}

	@Test
	@Deployment(resources = "subscribe-on-task.bpmn")
	public void testSubscribeOnTask() {

		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onTask");

		assertThat(processInstance).task("Task_1um2wc3");
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		complete(task()); // complete the user task proceed to subscr task

		assertThat(processInstance).task("Task_0ay21b8");
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		complete(task());

		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);
		processEngine().getRuntimeService().correlateMessage("Message_SubscribeOnTask");
		assertThat(processInstance).isEnded();
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);

	}

	@Test
	@Deployment(resources = "subscribe-when-reached.bpmn")
	public void testWhenReached() {

		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.whenReached");

		assertThat(processInstance).task("Task_1um2wc3");
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		complete(task()); // complete the user task

		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		processEngine().getRuntimeService().correlateMessage("Message_WhenReached");
		assertThat(processInstance).isEnded();
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);

	}

}
