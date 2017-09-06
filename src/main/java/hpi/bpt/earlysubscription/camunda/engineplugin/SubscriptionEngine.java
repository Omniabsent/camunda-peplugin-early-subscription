package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.util.json.JSONObject;

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

	static {
		LOGGER.setLevel(Level.INFO);
	}

	public static final String CONST_SUBTIME_DEPLOYMENT = "process-deployment";
	public static final String CONST_SUBTIME_INSTANTIATION = "process-instantiation";
	public static final String CONST_SUBTIME_MANUAL = "manual";
	public static final String CONST_SUBTIME_ELEMENTREACHED = "element-reached";
	public static final String CONST_SUBTIME_DEFAULT = CONST_SUBTIME_ELEMENTREACHED;

	private static final String UNICORN_BASEURL = "http://localhost:8080/Unicorn/webapi/REST/BufferedEventQuery/";
	private static final String CAMUNDA_WEBSERVICEURL = "http://localhost:8080/engine-rest/message";

	// the query repo maps internal query Ids to the cep query ids
	public static final HashMap<String, String> queryRepository = new HashMap<String, String>();
	// the subscr. repo maps internal subscr. Ids to the cep subscription ids
	public static final HashMap<String, String> subscriptionRepository = new HashMap<String, String>();

	public static void registerQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String intQueryId = getInternalQueryId(sd, dex);
		LOGGER.info("Registering Query with internal id " + intQueryId);

		// do register
		JSONObject jsonPayload = new JSONObject();
		jsonPayload.put("eventQuery", sd.eventQuery);

		if (sd.bufferPolicies != null) {
			JSONObject jsonBufferPolicies = new JSONObject();
			jsonBufferPolicies.put("lifespan", sd.bufferPolicies.lifespanPolicy);
			jsonBufferPolicies.put("consumption", sd.bufferPolicies.consumptionPolicy);
			jsonBufferPolicies.put("order", sd.bufferPolicies.orderPolicy);
			jsonBufferPolicies.put("size", sd.bufferPolicies.sizePolicy);
			jsonPayload.put("bufferPolicies", jsonBufferPolicies);
		}

		String extQueryId = doHTTPCall("POST", jsonPayload, UNICORN_BASEURL);

		// store uuid
		queryRepository.put(intQueryId, extQueryId);
		LOGGER.info("stored query (internal)" + intQueryId + " as external id " + extQueryId);
	}

	public static void subscribeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Subscribing to Query with internal id " + qId);
		String extQueryId = queryRepository.get(qId);

		// do subscr
		JSONObject jsonPayload = new JSONObject();
		jsonPayload.put("postAddress", CAMUNDA_WEBSERVICEURL);
		jsonPayload.put("processInstanceId", dex.getProcessInstanceId());
		jsonPayload.put("messageName", sd.bpmnMessageName);

		new SubscriptionEngine().new ASyncSubscribeThread(jsonPayload, UNICORN_BASEURL + extQueryId,
				getInternalSubscriptionId(dex)).start();

	}

	private class ASyncSubscribeThread extends Thread {
		String url, internalSubscriptionId;
		JSONObject pl;

		public ASyncSubscribeThread(JSONObject jsonPayload, String url, String internalSubId) {
			super();
			this.pl = jsonPayload;
			this.url = url;
			internalSubscriptionId = internalSubId;
		}

		public void run() {
			// wait 2 seconds so that camunda starts listening for the event
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String extSubscriptionId = doHTTPCall("POST", pl, url);

			// store uuid
			subscriptionRepository.put(internalSubscriptionId, extSubscriptionId);
			LOGGER.info("stored subscription (internal) as external id " + extSubscriptionId);
		}

	}

	public static void unsubscribeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Unsubscribing from Query (internalId) " + qId);
		String extSubscriptionId = subscriptionRepository.get(getInternalSubscriptionId(dex));
		String extQueryId = queryRepository.get(qId);

		// do unsubscr
		String unsuburl = UNICORN_BASEURL + extQueryId + "/" + extSubscriptionId;
		doDELETE(unsuburl);

		// remove from repo
		subscriptionRepository.remove(getInternalSubscriptionId(dex));
	}

	public static void removeQuery(SubscriptionDefinition sd, DelegateExecution dex) {
		String qId = getInternalQueryId(sd, dex);
		LOGGER.info("Removing Query " + qId);
		String extQueryId = queryRepository.get(qId);

		// do remove
		doDELETE(UNICORN_BASEURL + extQueryId);

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

	public static String doHTTPCall(String requestMethod, JSONObject jsonPayload, String url) {
		String result = null;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setDoOutput(true);
			con.setRequestMethod(requestMethod);
			con.setRequestProperty("Content-Type", "application/json");

			String jsonBody = jsonPayload.toString();

			OutputStream os = con.getOutputStream();
			os.write(jsonBody.getBytes());
			os.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			result = response.toString();
		} catch (Exception e) {
			// e.printStackTrace();
			LOGGER.info("Error while HTTP call: " + e.getMessage());
		}
		return result;

	}

	public static void doDELETE(String url) {
		String result = null;
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("DELETE");
			con.setRequestProperty("Content-Type", "application/json");

			con.connect();

		} catch (Exception e) {
			// e.printStackTrace();
			LOGGER.info("Error while HTTP DELETE: " + e.getMessage());
		}
	}

}
