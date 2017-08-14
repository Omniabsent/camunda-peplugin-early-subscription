package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class CreateBufferListener implements ExecutionListener {

	private final Logger LOGGER = Logger.getLogger(SubscribeListener.class.getName());

	@Override
	public void notify(DelegateExecution arg0) throws Exception {
		// do createBuffer call

		LOGGER.info("my eventQuery is: " + "???");
		// execution.getProcessEngineServices()
	}
}
