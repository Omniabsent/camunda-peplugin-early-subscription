package hpi.bpt.earlysubscription.camunda.engineplugin;

public class SubscriptionDefinition {
	public String eventQuery, subscriptionTime, bpmnMessageId, bpmnMessageName;
	public BufferPolicies bufferPolicies;

	public class BufferPolicies {
		public String lifespanPolicy, sizePolicy, orderPolicy, consumptionPolicy;
	}
}
