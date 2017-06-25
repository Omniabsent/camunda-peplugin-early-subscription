package hpi.bpt.earlysubscription.camunda.engineplugin;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.task.Task;

public class MySampleTaskListener implements TaskListener {

  private final Logger LOGGER = Logger.getLogger(MySampleTaskListener.class.getName());

  @Override
  public void notify(DelegateTask task) {
    LOGGER.info("Event '" + task.getEventName() + "' received by Task Listener for Task:"
        + " activityId=" + task.getTaskDefinitionKey()
        + ", name='" + task.getName() + "'"
        + ", taskId=" + task.getId()
        + ", assignee='" + task.getAssignee() + "'"
        + ", candidateGroups='" + task.getCandidates() + "'");
  }

}
