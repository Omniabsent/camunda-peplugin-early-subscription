package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

import hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners.RegisterQueryListener;
import hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners.RemoveQueryListener;
import hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners.SubscribeListener;
import hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners.UnsubscribeListener;

public class FlexibleSubscriptionParseListener extends AbstractBpmnParseListener implements BpmnParseListener {

	private final Logger LOGGER = Logger.getLogger(FlexibleSubscriptionParseListener.class.getName());

	@Override
	public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {
		LOGGER.setLevel(Level.INFO);
		LOGGER.info("\n\n------- executing parseRootElement -------\n\n");

		// processDefinitions.get(0).getActivities().get(0).addListener(ExecutionListener.EVENTNAME_START,
		// new SubscribeListener(subscriptionDefinition));

		ProcessDefinitionEntity thisProcess = processDefinitions.get(0);

		// get all messages into HashMap<String msgname, Element>
		List<Element> messageElements = rootElement.elements("message");
		System.out.println("number of messages found: " + messageElements.size());
		// HashMap<String, Element> messages = new HashMap<String, Element>();
		// for (Element el : messageElements) {
		// messages.put(el.attribute("id"), el);
		// }

		// go through messages and register necessary listeners
		for (Element el : messageElements) { // for each message:
			String messageId = el.attribute("id");
			// get subscriptionDefinition
			SubscriptionDefinition sd = getSubscriptionDefinition(el);
			// get activities that reference this message
			List<ActivityImpl> receivingActivities = findRefActivities(messageId, rootElement, thisProcess);

			if (sd != null) { // if referenced message has flex-sub-extensions:
				System.out.println("Adding flexsub execution listeners for message " + messageId + "; Subsc. time: "
						+ sd.subscriptionTime);

				switch (sd.subscriptionTime) {
				case SubscriptionEngine.CONST_SUBTIME_DEPLOYMENT:
					SubscriptionEngine.registerQuery(sd, null);
					for (ActivityImpl act : receivingActivities) {
						act.addListener(ExecutionListener.EVENTNAME_START, new SubscribeListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_END, new UnsubscribeListener(sd));
					}
					break;
				case SubscriptionEngine.CONST_SUBTIME_INSTANTIATION:
					thisProcess.addListener(ExecutionListener.EVENTNAME_START, new RegisterQueryListener(sd));
					for (ActivityImpl act : receivingActivities) {
						act.addListener(ExecutionListener.EVENTNAME_START, new SubscribeListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_END, new UnsubscribeListener(sd));
					}
					thisProcess.addListener(ExecutionListener.EVENTNAME_END, new RemoveQueryListener(sd));

					break;
				case SubscriptionEngine.CONST_SUBTIME_MANUAL:
					// if there is a subscription task for this message
					List<ActivityImpl> subscriptionTasks = findSubscriptionTasks(messageId, rootElement, thisProcess);
					if (!subscriptionTasks.isEmpty()) {
						// then do registerQuery at each of these tasks
						for (ActivityImpl act : subscriptionTasks) {
							act.addListener(ExecutionListener.EVENTNAME_START, new RegisterQueryListener(sd));
						}

						// and removeQuery at end of process
						thisProcess.addListener(ExecutionListener.EVENTNAME_END, new RemoveQueryListener(sd));
					}

					// add subscribe/unsubscribe
					for (ActivityImpl act : receivingActivities) {
						act.addListener(ExecutionListener.EVENTNAME_START, new SubscribeListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_END, new UnsubscribeListener(sd));
					}
					break;
				default:
					// in every other case registerQuery when element reached
					// removeQuery at end
					// (element-reached, missing or unknown value)
					for (ActivityImpl act : receivingActivities) {
						act.addListener(ExecutionListener.EVENTNAME_START, new RegisterQueryListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_START, new SubscribeListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_END, new UnsubscribeListener(sd));
						act.addListener(ExecutionListener.EVENTNAME_END, new RemoveQueryListener(sd));
					}
				}

			} else {
				System.out.println(
						"Skipping this message because it doesn't have the flexsub extension. id: " + messageId);
			}
		}

		// START EVENT:
		// for each cep message start event: do direct subscription now

		// UN-DEPLOYMENT:
		// considering that process deployments are rarely completely deleted in
		// camunda, we did not implement the query deletion for on un-deployment
		// could be done using a maintenance thread, that checks if all buffered
		// processes are still deployed

	}

	private List<ActivityImpl> findSubscriptionTasks(String messageId, Element rootElement,
			ProcessDefinitionEntity processDef) {
		ArrayList<ActivityImpl> activities = new ArrayList<ActivityImpl>();

		List<Element> els = elementsByTagRecursive(rootElement, "task");

		for (Element el : els) {
			String refMessageId = getCamundaPropertyValue(el, "flexsub.messageId");
			if (refMessageId != null) {
				// el subscribes to refMessageId
				String aId = el.attribute("id");
				activities.add(processDef.findActivity(aId));
			}
		}

		return activities;
	}

	private List<ActivityImpl> findRefActivities(String messageId, Element rootElement,
			ProcessDefinitionEntity processDef) {
		ArrayList<ActivityImpl> activities = new ArrayList<ActivityImpl>();

		// each boundary message event
		// each intermediate message catch event
		List<Element> els = elementsByTagRecursive(rootElement, "intermediateCatchEvent");
		els.addAll(elementsByTagRecursive(rootElement, "boundaryEvent"));

		for (Element el : els) {
			// extract 'messageRef' from "messageEventDefinition"
			String mref = el.element("messageEventDefinition").attribute("messageRef");

			if (mref.equals(messageId)) {
				String activityId = el.attribute("id");
				ActivityImpl ai = processDef.findActivity(activityId);
				activities.add(ai);
			}
		}

		// also add receiveTasks
		els = elementsByTagRecursive(rootElement, "receiveTask");
		for (Element el : els) {
			// extract 'messageRef' from "messageEventDefinition"
			String mref = el.attribute("messageRef");

			if (mref.equals(messageId)) {
				String activityId = el.attribute("id");
				ActivityImpl ai = processDef.findActivity(activityId);
				activities.add(ai);
			}
		}

		return activities;

	}

	private List<Element> elementsByTagRecursive(Element rootElement, String tagName) {
		List<Element> resultElements = new ArrayList<Element>();
		resultElements.addAll(rootElement.elements(tagName));
		for (Element el : rootElement.elements()) {
			resultElements.addAll(elementsByTagRecursive(el, tagName));
		}

		return resultElements;
	}

	/**
	 * Get the subscriptionDefinition for a given bpmn:message element
	 * 
	 * @param el
	 * @return
	 */
	private SubscriptionDefinition getSubscriptionDefinition(Element el) {

		Element sdElement = el.element("extensionElements").element("subscriptionDefinition");
		if (sdElement != null) {
			SubscriptionDefinition sd = new SubscriptionDefinition();
			// fill with data
			sd.eventQuery = sdElement.element("eventQuery").getText();
			sd.bpmnMessageId = el.attribute("id");
			sd.bpmnMessageName = el.attribute("name");

			sd.subscriptionTime = getTextIfNotNull(sdElement, "subscriptionTime");
			if (sd.subscriptionTime == null) {
				// fall back to default
				sd.subscriptionTime = SubscriptionEngine.CONST_SUBTIME_DEFAULT;
			}

			Element bPo = sdElement.element("bufferPolicies");
			if (bPo != null) {
				sd.bufferPolicies = sd.new BufferPolicies();
				sd.bufferPolicies.lifespanPolicy = getTextIfNotNull(bPo, "lifespanPolicy");
				sd.bufferPolicies.consumptionPolicy = getTextIfNotNull(bPo, "consumptionPolicy");
				sd.bufferPolicies.orderPolicy = getTextIfNotNull(bPo, "orderPolicy");
				sd.bufferPolicies.sizePolicy = getTextIfNotNull(bPo, "sizePolicy");
			}
			return sd;
		} else {
			return null;
		}

	}

	/**
	 * Avoids null-pointer exceptions when getting text from a sub-element that
	 * does not exist.
	 * 
	 * @param el
	 * @param eName
	 * @return
	 */
	private String getTextIfNotNull(Element el, String eName) {
		String result = null;
		Element subElement = el.element(eName);
		if (subElement != null) {
			result = subElement.getText();
		}
		return result;
	}

	private String getCamundaPropertyValue(Element el, String propertyName) {
		// get the <extensionElements ...> element from the service task
		Element extensionElement = el.element("extensionElements");
		if (extensionElement != null) {

			// get the <camunda:properties ...> element from the service task
			Element propertiesElement = extensionElement.element("properties");
			if (propertiesElement != null) {

				// get list of <camunda:property ...> elements from the
				List<Element> props = propertiesElement.elements("property");
				for (Element e : props) {
					if (propertyName.equals(e.attribute("name"))) {
						return e.attribute("value");
					}
				}
			}
		}
		return null; // if nothing found
	}

}
