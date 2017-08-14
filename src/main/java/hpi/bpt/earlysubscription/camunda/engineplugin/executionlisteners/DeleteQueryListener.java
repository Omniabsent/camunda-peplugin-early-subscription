package hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;

public class DeleteQueryListener implements ExecutionListener {
	private String eventQuery;

	private final Logger LOGGER = Logger.getLogger(DeleteQueryListener.class.getName());

	public DeleteQueryListener(String eventQuery) {
		this.eventQuery = eventQuery;
	}

	@Override
	public void notify(DelegateExecution arg0) throws Exception {
		// issue subscription to the buffer

		LOGGER.info("my eventQuery is: " + eventQuery);

	}

}
