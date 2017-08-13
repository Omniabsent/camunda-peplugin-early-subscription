package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.List;
import java.util.logging.Logger;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.repository.ProcessDefinition;

public class SubscriptionListener implements ExecutionListener {
	private String eventQuery;

	private final Logger LOGGER = Logger.getLogger(SubscriptionListener.class.getName());

	public SubscriptionListener(String eventQuery) {
		this.eventQuery = eventQuery;
	}

	@Override
	public void notify(DelegateExecution arg0) throws Exception {
		// issue subscription to the buffer

		LOGGER.info("my eventQuery is: " + eventQuery);
		// execution.getProcessEngineServices()
		ProcessEngine processEngine = ProcessEngines.getProcessEngines().values().iterator().next();
		RepositoryService repositoryService = processEngine.getRepositoryService();

		List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery().list();
		// .getBpmnModelInstance("earlysubscription.camunda.engineplugin");
		System.out.println("Definitions available: " + defs.size());
	}

}
