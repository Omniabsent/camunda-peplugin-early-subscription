package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;
import org.camunda.bpm.engine.impl.util.xml.Namespace;

public class MySampleParseListener extends AbstractBpmnParseListener implements BpmnParseListener {

	private final Logger LOGGER = Logger.getLogger(MySampleParseListener.class.getName());

	@Override
	public void parseStartEvent(Element startEventElement, ScopeImpl scope, ActivityImpl startEvent) {
		// at end: execute createBuffer for all Messages with subscribe on Inst

		LOGGER.info("Parsing Start Event " + ", activtyId=" + startEvent.getId() + ", activtyName='"
				+ startEvent.getName() + "'" + ", scopeId=" + scope.getId() + ", scopeName=" + scope.getName());
	}

	@Override
	public void parseServiceTask(Element serviceTaskElement, ScopeImpl scope, ActivityImpl activity) {
		// if extension available, set delegate; this way camunda may not spawn
		// an error

		// attach createBufferListener if flexsub extension is present

	}

	@Override
	public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
		// start: attach subscribeListener
		// end: attach unsubscr. listener

		// CANNOT get the query value from the referenced message element

	}

	public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {

		// START EVENT
		// for each cep message start event: do direct subscription now
		List<Element> messages = rootElement.elements("message");
		System.out.println("number of messages found: " + messages.size());

		// for each boundary message event
		// and for each receive task
		// and for each intermediate message catch event
		// if referenced message has flex-sub-extensions:
		// (1) find activity and add listeners | use "findActivity?"
		// (2) if subscr. on depl: issue createBuffer (this could also be done
		// simply for each Message? watch out for startEvent)

		// boundary/interm message event
		// intermediateCatchEvent | boundaryEvent
		// find these elements
		List<Element> els = elementsByTagRecursive(rootElement, "intermediateCatchEvent");

		for (Element el : els) {
			// extract 'messageRef' from "messageEventDefinition"
			String mref = el.element("messageEventDefinition").attribute("messageRef");
			System.out.println("messageRef is " + mref);

			// get subscriptionTime | use constants in SubscriptionEngine
			// attach listeners for subscription and unsubscr
			// extract query

			// attach listeners for createBuffer, deleteQuery
		}

	}

	private List<Element> elementsByTagRecursive(Element rootElement, String tagName) {
		List<Element> resultElements = new ArrayList<Element>();
		resultElements.addAll(rootElement.elements(tagName));
		for (Element el : rootElement.elements()) {
			resultElements.addAll(elementsByTagRecursive(el, tagName));
		}

		return resultElements;
	}

	private String getExtensionValue(Element el, String propertyName) {
		// get the <extensionElements ...> element from the service task
		Element extensionElement = el.elementNS(new Namespace("flexsub"), "extensionElements");
		if (extensionElement != null) {

			// get the <camunda:properties ...> element from the service task
			Element propertiesElement = extensionElement.elementNS(new Namespace("flexsub"), "properties");
			if (propertiesElement != null) {

				// get list of <camunda:property ...> elements from the service
				// task
				Element prop = propertiesElement.elementNS(new Namespace("flexsub"), "propertyName");
				if (prop != null) {
					return prop.getText();
				}
			}
		}
		return null; // if nothing found
	}

}
