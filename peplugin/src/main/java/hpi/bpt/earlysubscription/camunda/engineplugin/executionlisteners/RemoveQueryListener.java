package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

import hpi.bpt.earlysubscription.camunda.engineplugin.SubscriptionDefinition;
import hpi.bpt.earlysubscription.camunda.engineplugin.SubscriptionEngine;

public class RemoveQueryListener implements ExecutionListener {

	private final Logger LOGGER = Logger.getLogger(RemoveQueryListener.class.getName());

	private SubscriptionDefinition subscriptionDefinition;

	public RemoveQueryListener(SubscriptionDefinition sd) {
		subscriptionDefinition = sd;
	}

	@Override
	public void notify(DelegateExecution dex) throws Exception {
		SubscriptionEngine.removeQuery(subscriptionDefinition, dex);
	}

}
