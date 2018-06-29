package team.communicationstrategies;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;

import java.util.Collection;

public class DummyCommunicationStrategy extends team.AbstractCommunicationStrategy {
    public void communicateChanges(ChangeSet changed)
    {

    }
    public void handleMessages(Collection<Command> heard)
    {
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                AKSpeak speak = (AKSpeak)next;
                Logger.debug("Received " + speak.getContent().toString() + " from agent " + speak.getAgentID());
            }
        }

    }

}
