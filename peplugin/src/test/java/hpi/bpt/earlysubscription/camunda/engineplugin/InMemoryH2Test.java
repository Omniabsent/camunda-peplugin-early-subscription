package hpi.bpt.earlysubscription.camunda.engineplugin;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.init;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineAssertions.processEngine;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.junit.Assert.assertTrue;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.TaskService;
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
	@Deployment(resources = "subscribe-on-deployment.bpmn")
	public void testOnDeployment() {
		// check that subscriptionEngine queryRepo has 1 entry
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onDeployment");

		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		processEngine().getRuntimeService().correlateMessage("Message_OnDeployment");
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);

		assertThat(processInstance).isEnded();

		// clean up for queries with subscr. on deployment
		SubscriptionEngine.queryRepository.clear();
	}

	@Test
	@Deployment(resources = { "retail_customer.bpmn", "retail_retailer.bpmn" })
	public void testRetailProcess() {
		TaskService ts = processEngine().getTaskService();

		ProcessInstance processInstanceCustomer = processEngine().getRuntimeService()
				.startProcessInstanceByKey("flexsub.camunda.demoprojects.complex.retail.customer");

		// CUSTOMER
		assertThat(processInstanceCustomer).task("Task_0t7yqxy");
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		complete(task()); // complete task "Request Quote"
		// sends quote request
		// ts.complete(ts.createTaskQuery().active().processInstanceId(processInstanceCustomer.getId()).list().get(0).getId());

		// RETAILER
		ProcessInstance processInstanceRetailer = (ProcessInstance) processEngine().getRuntimeService()
				.createExecutionQuery().processDefinitionKey("flexsub.camunda.demoprojects.complex.retail.retailer")
				.list().get(0);
		assertThat(processInstanceRetailer).task("Task_0l1sh5s");
		complete(task()); // complete task "Prepare Quote"
		// subscribes to delays
		// sends quote
		assertThat(processInstanceRetailer).task("IntermediateThrowEvent_1ua0m5w"); // catching!

		// CUSTOMER
		assertThat(processInstanceCustomer).task("Task_0bqa34h");
		complete(task()); // complete task "Accept Quote"
		assertThat(processInstanceCustomer).task("Task_1isz5s4");
		complete(task()); // complete task "Initiate Payment"
		assertThat(processInstanceCustomer).isEnded();

		// RETAILER
		assertThat(processInstanceRetailer).task("Task_1y257tw");
		processEngine().getRuntimeService().correlateMessage("Message_ShipmentDelay");
		// now 2 active tasks
		assertTrue(ts.createTaskQuery().active().processInstanceId(processInstanceRetailer.getId()).list().size() == 2);
		// complete "inform about late shipment"
		ts.complete(ts.createTaskQuery().active().taskDefinitionKey("Task_1jjispb")
				.processInstanceId(processInstanceRetailer.getId()).list().get(0).getId());

		complete(task()); // complete task "Prepare shipping"

		assertThat(processInstanceRetailer).task("Task_1to2mqi");
		complete(task()); // complete task "ship"
		assertThat(processInstanceRetailer).isEnded();
	}

	@Test
	@Deployment(resources = "subscribe-on-deployment_receiveTask.bpmn")
	public void testReceiveTask() {
		// check that subscriptionEngine queryRepo has 1 entry
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.receiveTask");

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
	@Deployment(resources = "eurotunnel.bpmn")
	public void testBoundaryEvent() {
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("flexsub.camunda.demoprojects.complex.eurotunnel");

		assertThat(processInstance).task("Task_15uit6h");
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);
		processEngine().getRuntimeService().correlateMessage("Eurotunnel Delay");

		// moves on to "Cross via ferry" and removes subscription
		assertThat(processInstance).task("Task_0l0xvg5");
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
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
	@Deployment(resources = "subscribe-on-task.bpmn")
	public void testSubscribeOnTaskTwoInstances() {

		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);

		ProcessInstance processInstance = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onTask");
		ProcessInstance processInstance2 = processEngine().getRuntimeService()
				.startProcessInstanceByKey("earlysubscription.camunda.engineplugin.tests.onTask");

		assertThat(processInstance).task("Task_1um2wc3");
		assertTrue(SubscriptionEngine.queryRepository.size() == 0);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		complete(task()); // complete the user task proceed to subscr task

		assertThat(processInstance).task("Task_0ay21b8");
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		complete(task());

		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		// go ahead with instance 2
		assertThat(processInstance2).task("Task_1um2wc3");
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);
		complete(task()); // complete the user task proceed to subscr task

		assertThat(processInstance2).task("Task_0ay21b8");
		assertTrue(SubscriptionEngine.queryRepository.size() == 2);
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);

		// finish instance1
		processEngine().getRuntimeService().correlateMessage("Message_SubscribeOnTask");
		assertThat(processInstance).isEnded();
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 0);
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);

		// finish instance2
		assertThat(processInstance2).task("Task_0ay21b8");
		complete(task());
		assertTrue(SubscriptionEngine.subscriptionRepository.size() == 1);
		assertTrue(SubscriptionEngine.queryRepository.size() == 1);
		processEngine().getRuntimeService().correlateMessage("Message_SubscribeOnTask");
		assertThat(processInstance2).isEnded();
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
