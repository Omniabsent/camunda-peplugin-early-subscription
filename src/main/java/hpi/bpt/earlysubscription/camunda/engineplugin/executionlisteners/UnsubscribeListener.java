package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class UnsubscribeListener implements ExecutionListener {
	private String eventQuery;

	private final Logger LOGGER = Logger.getLogger(UnsubscribeListener.class.getName());

	public UnsubscribeListener(String eventQuery) {
		this.eventQuery = eventQuery;
	}

	@Override
	public void notify(DelegateExecution arg0) throws Exception {
		// issue subscription to the buffer

		LOGGER.info("my eventQuery is: " + eventQuery);

	}

}
