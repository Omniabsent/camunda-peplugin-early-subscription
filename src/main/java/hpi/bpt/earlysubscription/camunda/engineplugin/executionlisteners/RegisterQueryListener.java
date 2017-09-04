package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

import hpi.bpt.earlysubscription.camunda.engineplugin.SubscriptionDefinition;
import hpi.bpt.earlysubscription.camunda.engineplugin.SubscriptionEngine;

public class RegisterQueryListener implements ExecutionListener {

	private final Logger LOGGER = Logger.getLogger(SubscribeListener.class.getName());

	private SubscriptionDefinition subscriptionDefinition;

	public RegisterQueryListener(SubscriptionDefinition sd) {
		subscriptionDefinition = sd;
	}

	@Override
	public void notify(DelegateExecution dex) throws Exception {
		SubscriptionEngine.registerQuery(subscriptionDefinition, dex);

		LOGGER.info("my eventQuery is: " + "???");
		// execution.getProcessEngineServices()

	}
}
