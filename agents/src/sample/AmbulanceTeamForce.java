package sample;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;


import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;

/**
   A sample ambulance team agent.
 */
public class AmbulanceTeamForce extends AbstractSampleAgent<AmbulanceTeam> {
	
	private enum State{
		AVAIABLE,
		WALK,
		RESCUE,
		LOAD,
	}
	
    private Collection<EntityID> unexploredBuildings;
    private State state = State.AVAIABLE;
    private AmbulanceTeam me;
    @Override
    public String toString() {
        return "Agente Médico";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        System.out.println("Ambulance Team Conectado com sucesso");
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING);
        unexploredBuildings = new HashSet<EntityID>(buildingIDs);
        me = (AmbulanceTeam) me();
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
    	
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);            
        }
        updateUnexploredBuildings(changed); 
        System.out.println(me.getID()+" Moving to "+ me.getDirectionProperty().getValue());
        
        // Se o agente morrer remova o seu id da lista do hospital
        if(me.getHP() == 0) {
        	listaHospital.rmResponsability(me.getID().getValue());
        }
        
        //listaHospital.printResponsibilities();
        
        // Verifica se está levando algum civil para o refúgio
        if (someoneOnBoard()) {
            // Verifica se está no refúgio
            if (location() instanceof Refuge) {
                // Deixa o civil do refugio
            	Logger.info("Deixando civil no refúgio");
                sendUnload(time);
                state = State.LOAD;
                System.out.println("Estado atual do " + me.toString() + " " + state);
                return;
            }
            else {
                // Move para o refúgio
                List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
                if (path != null) {
                    Logger.info("Movendo para o refúgio");
                    sendMove(time, path);
                    state = State.WALK;
                    System.out.println("Estado atual do " + me.toString() + " " + state);
                    return;
                }
                // O que fazer agora ?
                Logger.debug("Não foi possível encontra um caminho até o refúgio");
            }
        }
        // Procura entre os civis mais próximos para ver se consegue ajudar
        for (Human next : getTargets()) {        	
            if (next.getPosition().equals(location().getID())) {
                // Verifica se o civil está no mesmo local e se precisa de ajuda
            	
            		// se alguém está cuidando siga em frente            		
            	if(listaHospital.isOwner(me.getID().getValue(),next.getID().getValue())) {
            		if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
                        // Carrega civil
                        Logger.info("Carregando civil " + next);
                        sendLoad(time, next.getID());
                        state = State.LOAD;
                        System.out.println("Estado atual do " + me.toString() + " " + state);
                        return;
                    }
                    
                    if (next.getBuriedness() > 0) {
                        // Resgatando Civil
                        Logger.info("Resgatando civil " + next);
                        sendRescue(time, next.getID());
                        state = State.RESCUE;
                        System.out.println("Estado atual do " + me.toString() + " " + state);
                        return;
                    }
            	}
                
            }else {
            	
            	if(listaHospital.isOwner(me.getID().getValue(),next.getID().getValue()) && next.getHP() > 0) {
            		// Tenta se mover até o civil
            		System.out.println("Ambulancia "+me.getID().getValue()+" indo para "+ next.getID().getValue());
                    List<EntityID> path = search.breadthFirstSearch(me().getPosition(), next.getPosition());
                    state = State.AVAIABLE;
                    
                    if (path != null) {                
                        Logger.info("Movendo até o civil");                    
                        sendMove(time, path);
                        contador=0;
                        state = State.WALK;
                        System.out.println("Estado atual do " + me.toString() + " " + state);
                        return;
                    }
            	}else {
            		if(!listaHospital.someoneHasThisResponsability(next.getID().getValue())) {
            			if(next.getHP()>0) {
            				listaHospital.addResponsability(me.getID().getValue(), next.getID().getValue());
                			return;
            			}            			           			            			            			
            		}
            	}                
            }
        }
        // Nenhum civil próximo        
        List<EntityID> path = search.breadthFirstSearch(me().getPosition(), unexploredBuildings); 
        updateUnexploredBuildings(changed);
        if (path != null && contador<4) {
            Logger.info("Procurando prédios");            
            sendMove(time, path);
            state = State.WALK;
            for(int i=0;i<path.size();i++) {
            	if(me.getPosition().getValue() == path.get(i).getValue()) {
            		contador=0;
            	}
            }
            contador++;
            System.out.println("Estado atual do " + me.toString() + " " + state);
            lastpath = path;
            return;
        }
        
        Logger.info("Movendo aleatóriamente");
        sendMove(time, randomWalk());
        
        state = State.AVAIABLE;        
        System.out.println("Estado atual do " + me.toString() + " " + state);
        
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

    private boolean someoneOnBoard() {
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
            if (((Human)next).getPosition().equals(getID())) {
                Logger.debug(next + " está sendo carregado");
                return true;
            }
        }
        return false;
    }

    private List<Human> getTargets() {
        List<Human> targets = new ArrayList<Human>();
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
            Human h = (Human)next;
            if (h == me()) {
                continue;
            }
            if (h.isHPDefined()
                && h.isBuriednessDefined()
                && h.isDamageDefined()
                && h.isPositionDefined()
                && h.getHP() > 0
                && (h.getBuriedness() > 0 || h.getDamage() > 0)) {
                targets.add(h);
            }
        }
        Collections.sort(targets, new DistanceSorter(location(), model));
        return targets;
    }

    private void updateUnexploredBuildings(ChangeSet changed) {
        for (EntityID next : changed.getChangedEntities()) {
            unexploredBuildings.remove(next);
        }
    }
}
