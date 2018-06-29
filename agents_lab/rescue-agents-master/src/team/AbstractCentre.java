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
public abstract class AbstractCentre extends StandardAgent<Building> {
   /**
    * Communication strategy
    */
   protected AbstractCommunicationStrategy communicationStrategy;
   protected void postConnect() {
      communicationStrategy = new team.communicationstrategies.DummyCommunicationStrategy();
   }
}
