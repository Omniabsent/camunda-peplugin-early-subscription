package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class SubscribeListener implements ExecutionListener {
	private String eventQuery;

	private final Logger LOGGER = Logger.getLogger(SubscribeListener.class.getName());

	public SubscribeListener(String eventQuery) {
		this.eventQuery = eventQuery;
	}

	@Override
	public void notify(DelegateExecution arg0) throws Exception {
		// issue subscription to the buffer

		LOGGER.info("my eventQuery is: " + eventQuery);

	}

}
