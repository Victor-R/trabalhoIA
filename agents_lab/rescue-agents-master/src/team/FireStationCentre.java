package team;

import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;

/**
 A sample centre agent.
 */
public class FireStationCentre extends AbstractCentre {
    @Override
    public String toString() {
        return "Fire station centre";
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        communicationStrategy.communicateChanges(changed);
        communicationStrategy.handleMessages(heard);

        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channels 1 and 2
            sendSubscribe(time, 1, 2);
        }
        sendRest(time);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_STATION);
    }
}
