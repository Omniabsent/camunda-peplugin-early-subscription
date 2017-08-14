package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;

public class FlexibleEventSubscriptionPEP implements ProcessEnginePlugin {

	@Override
	public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
		List<BpmnParseListener> postParseListeners = processEngineConfiguration.getCustomPostBPMNParseListeners();

		if (postParseListeners == null) {
			postParseListeners = new ArrayList<BpmnParseListener>();
			processEngineConfiguration.setCustomPostBPMNParseListeners(postParseListeners);
		}
		postParseListeners.add(new FlexibleSubscriptionParseListener());

	}

	@Override
	public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

	}

	@Override
	public void postProcessEngineBuild(ProcessEngine processEngine) {

	}

}
