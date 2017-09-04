package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.HashMap;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;

/**
 * 
 * @author Dennis
 *
 * 
 *
 *
 */
public class SubscriptionEngine {

	private final static Logger LOGGER = Logger.getLogger(SubscriptionEngine.class.getName());

	public static final String CONST_SUBTIME_DEPLOYMENT = "process-deployment";
	public static final String CONST_SUBTIME_INSTANTIATION = "process-instantiation";
	public static final String CONST_SUBTIME_MANUAL = "manual";
	public static final String CONST_SUBTIME_ELEMENTREACHED = "element-reached";
	public static final String CONST_SUBTIME_DEFAULT = CONST_SUBTIME_ELEMENTREACHED;

	// the query repo maps internal query Ids to the cep query ids
	public static final HashMap<String, String> queryRepository = new HashMap<String, String>();
	// the subscr. repo maps internal subscr. Ids to the cep subscription ids
	public static final HashMap<String, String> subscriptionRepository = new HashMap<String, String>();

	public static void registerQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String intQueryId = getInternalQueryId(sd, dex);
		LOGGER.info("Registering Query with internal id " + intQueryId);

		// do register
		String extQueryId = "" + Math.random() * 100000;

		// store uuid
		queryRepository.put(intQueryId, extQueryId);
		LOGGER.info("stored query (internal)" + intQueryId + " as external id " + extQueryId);
	}

	public static void subscribeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Subscribing to Query with internal id " + qId);
		String extQueryId = queryRepository.get(qId);

		// do subscr
		String extSubscriptionId = "" + Math.random() * 100000;

		// store uuid
		subscriptionRepository.put(getInternalSubscriptionId(dex), extSubscriptionId);

	}

	public static void unsubscribeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Unsubscribing from Query " + qId);
		String extSubscriptionId = subscriptionRepository.get(getInternalSubscriptionId(dex));

		// do unsubscr

		// remove from repo
		subscriptionRepository.remove(getInternalSubscriptionId(dex));
	}

	public static void removeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Removing Query " + qId);
		String extQueryId = queryRepository.get(qId);
		// do remove

		// remove from repo
		queryRepository.remove(qId);
	}

	private static String getInternalSubscriptionId(DelegateExecution dex) {
		return dex.getActivityInstanceId();
	}

	private static String getInternalQueryId(SubscriptionDefinition sd, DelegateExecution dex) {
		switch (sd.subscriptionTime) {
		case SubscriptionEngine.CONST_SUBTIME_DEPLOYMENT:
			// TODO: this should actually be bound to processDefId
			return sd.bpmnMessageId + "||" + sd.eventQuery;
		case SubscriptionEngine.CONST_SUBTIME_INSTANTIATION:
			return dex.getProcessInstanceId() + "||" + sd.bpmnMessageId;
		case SubscriptionEngine.CONST_SUBTIME_MANUAL:
			return sd.bpmnMessageId + "||" + sd.eventQuery;
		default:
			return dex.getActivityInstanceId();
		}
	}
}
