package sample;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.Constants;
import rescuecore2.log.Logger;

import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;

/**
   Abstract base class for Agents.
   @param <E> The subclass of StandardEntity this agent wants to control.
 */
public abstract class AbstractSampleAgent<E extends StandardEntity> extends StandardAgent<E> {
    private static final int RANDOM_WALK_LENGTH = 50;
    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();

    /**
       The search algorithm -> Bread first search(busca em largura)
    */
    protected SampleSearch search;

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
       Cache of refuge IDs.
    */
    protected List<EntityID> refugeIDs;

    private Map<EntityID, Set<EntityID>> neighbours;

    /**
       Construct an AbstractSampleAgent.
    */
    protected AbstractSampleAgent() {
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        // Cria um array list para as prédios, ruas e refúgios
        buildingIDs = new ArrayList<EntityID>();
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        
        // Adiciona nos arraylist os respectivos ids do mapa
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
        }
        
        // Cria um grafo do mapa e implementa a busca em largura no mapa
        search = new SampleSearch(model);
        // Pega todos os vizinhos do mapa
        neighbours = search.getGraph();
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Communcation model: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Using speak model" : "Using say model");
    }

    /**
       Construct a random walk starting from this agent's current location to a random building.
       @return A random walk.
    */
    protected List<EntityID> randomWalk() {
    	// Cria um ArrayList baseado no RANDOM_WALK_LENGTH	
        List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH); 
        // Cria um ArrayList das Entitys que já visitou
        Set<EntityID> seen = new HashSet<EntityID>();
        // Posição atual do agente
        EntityID current = ((Human)me()).getPosition();
        
        for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
        	// Adiciona a posição atual no resultado e nos visitados
            result.add(current);
            seen.add(current);
            // Cria um ArrayList dos vizinhos do lugar atual e os embaralha aleatoriamente
            List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
            Collections.shuffle(possible, random);
            boolean found = false;
            // Percorre as possibilidades
            for (EntityID next : possible) {
            	// Caso já tenha visitado, ignora
                if (seen.contains(next)) {
                    continue;
                }
                // Caso contrário define essa possibilidade como o próximo movimento
                current = next;
                found = true;
                break;
            }
            // Caso nada tenha sido encontrado, está numa rua sem saida e não faz nada
            if (!found) {
                break;
            }
        }        
        return result;
        
    }
}
