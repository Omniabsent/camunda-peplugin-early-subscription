package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;

public class MySampleProcessEnginePlugin implements ProcessEnginePlugin {

	@Override
	public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
		List<BpmnParseListener> postParseListeners = processEngineConfiguration.getCustomPostBPMNParseListeners();

		if (postParseListeners == null) {
			postParseListeners = new ArrayList<BpmnParseListener>();
			processEngineConfiguration.setCustomPostBPMNParseListeners(postParseListeners);
		}
		postParseListeners.add(new MySampleParseListener());

		// Deployers
		List<Deployer> postDeployers = processEngineConfiguration.getCustomPostDeployers();

		if (postDeployers == null) {
			postDeployers = new ArrayList<Deployer>();
			processEngineConfiguration.setCustomPostDeployers(postDeployers);
		}

		postDeployers.add(new MyPostDeployer());

	}

	@Override
	public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

	}

	@Override
	public void postProcessEngineBuild(ProcessEngine processEngine) {

	}

}
