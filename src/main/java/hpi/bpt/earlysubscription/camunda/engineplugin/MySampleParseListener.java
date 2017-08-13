package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.List;
import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

import hpi.bpt.earlysubscription.camunda.engineplugin.executionlisteners.SubscriptionListener;

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

		// get the <extensionElements ...> element from the service task
		Element extensionElement = serviceTaskElement.element("extensionElements");
		if (extensionElement != null) {

			// get the <camunda:properties ...> element from the service task
			Element propertiesElement = extensionElement.element("properties");
			if (propertiesElement != null) {

				// get list of <camunda:property ...> elements from the service
				// task
				List<Element> propertyList = propertiesElement.elements("property");
				for (Element property : propertyList) {

					// get the name and the value of the extension property
					// element
					String name = property.attribute("name");
					String value = property.attribute("value");

					if (name.equals("messageId")) {
						String messageId = value;
						LOGGER.info("Service Task should subscribe to message id " + messageId);

						activity.addListener(ExecutionListener.EVENTNAME_END, new SubscriptionListener(value));
					}

					// ProgressLoggingExecutionListener
					// progressLoggingExecutionListener = new
					// ProgressLoggingExecutionListener(
					// value);
				}
			}
		}

	}

	@Override
	public void parseIntermediateCatchEvent(Element intermediateEventElement, ScopeImpl scope, ActivityImpl activity) {
		// start: attach subscribeListener
		// end: attach unsubscr. listener

		// CANNOT get the query value from the referenced message element

	}

	public void parseRootElement(Element rootElement, List<ProcessDefinitionEntity> processDefinitions) {
		// intercept after root element finished parsing (entire process
		// available)

		// for each cep message start event: do direct subscription now

		// for each boundary message event
		// and for each receive task
		// and for each intermediate message catch event
		// if referenced message has flex-sub-extensions:
		// (1) find activity and add listeners | use "findActivity?"
		// (2) if subscr. on depl: issue createBuffer (this could also be done
		// simply for each Message? watch out for startEvent)

		// can I use a query concept on Element?

	}

	// parseBoundaryEvent(Element boundaryEventElement, ScopeImpl scopeElement,
	// ActivityImpl nestedActivity)

	// parseReceiveTask(Element receiveTaskElement, ScopeImpl scope,
	// ActivityImpl activity)

	// parseEndEvent: deleteQuery if not <subscr on depl>
}
