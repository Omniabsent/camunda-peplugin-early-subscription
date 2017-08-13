package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;

public class MyPostDeployer implements Deployer {

	@Override
	public void deploy(DeploymentEntity deployment) {
		// subscribe to message start events with eventQuery
		// subscribe to all messages with subscriptionTime = on deployment

		List<ProcessDefinition> pdefs = deployment.getDeployedProcessDefinitions();

		for (ProcessDefinition pdef : pdefs) {
			String definitionId = pdef.getId();
			System.out.println("DefinitionId: " + definitionId);

			ProcessEngine processEngine = ProcessEngines.getProcessEngines().values().iterator().next();
			RepositoryService repositoryService = processEngine.getRepositoryService();

			List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery().list();
			// .getBpmnModelInstance("earlysubscription.camunda.engineplugin");
			System.out.println("Definitions available: " + defs.size());
		}

	}

}
