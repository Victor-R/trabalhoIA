package team;

import rescuecore2.messages.Command;
import rescuecore2.worldmodel.ChangeSet;

import java.util.Collection;

public abstract class AbstractCommunicationStrategy {
    /**
     * This function is called to broadcast observation
     */
    public abstract void communicateChanges(ChangeSet changed);

    /**
     * This function is called with all the messages that are received by the agent
     */
    public abstract void handleMessages(Collection<Command> heard);
}
