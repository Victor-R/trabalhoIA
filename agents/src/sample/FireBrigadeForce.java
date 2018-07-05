package sample;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.List;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.FireBrigade;


public class FireBrigadeForce extends AbstractSampleAgent<FireBrigade> {
	
	private enum State{
		AVAIABLE,
		WALK,
		EXTINGUISH,
		WATER
	}
	
    private static final String MAX_WATER_KEY = "fire.tank.maximum";
    private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

    private int maxWater;
    private int maxDistance;
    private int maxPower;
    
    private State state = State.AVAIABLE;
    private ArrayList<Building> buildingDetected = new ArrayList<Building>();

    @Override
    public String toString() {
        return "Agente Bombeiro";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION);
        maxWater = config.getIntValue(MAX_WATER_KEY);
        maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
        maxPower = config.getIntValue(MAX_POWER_KEY);
        System.out.println("Bombeiro conectado com sucesso id: " + me().getID());
        Logger.info("Bombeiro conectado com sucesso: id = " + me().getID() 
        		+ " distância máxima de extinção de fogo"
        		+ " = " + maxDistance + ", potencia máxima = " + maxPower 
        		+ ", capacidade máxima do tanque = " + maxWater);
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
        FireBrigade me = me();
        // Verifica se está enchendo a água
        if (me.isWaterDefined() && me.getWater() < maxWater && location() instanceof Refuge) {
            Logger.info("Enchendo a água no local: " + location());
            sendRest(time);
            state = State.WATER;
            System.out.println("Estado atual do " + me.toString() + " " + state);
            return;
        }
        // Verifica se está sem água
        if (me.isWaterDefined() && me.getWater() == 0) {
            // Procura o refúgio mais próximo
            List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
            if (path != null) {
                Logger.info("Movendo para o refúgio para encher a água");
                sendMove(time, path);
                state = state.WALK;
                System.out.println("Estado atual do " + me.toString() + " " + state);
                return;
            }
            else {
                Logger.debug("Não foi possíve encontra um caminho até o refúgio");
                System.out.println("Não foi possivel encontrar um caminho até o refúgio");
                path = randomWalk();
                Logger.info("Movendo aleatóriamente");
                sendMove(time, path);
                state = state.AVAIABLE;
                System.out.println("Estado atual do " + me.toString() + " " + state);
                return;
            }
        }
        // Procura todas as construções que estão pegando fogo
        Collection<EntityID> all = getBurningBuildings();
        // É possível apagar esse fogo?
        for (EntityID next : all) {
            if (model.getDistance(getID(), next) <= maxDistance || me().getPosition() != next) {
                Logger.info("Apagando " + next);
                sendExtinguish(time, next, maxPower);
                try {
                	sendSpeak(time, 1, ("Apagando " + next).getBytes("UTF-8"));
                }
                catch (UnsupportedEncodingException e) {
        			Logger.error("UnsupportedEnconding " + e.getMessage());
                }
                Building b = (Building)model.getEntity(next);
                buildingDetected.remove(b);
                state = state.EXTINGUISH;
                System.out.println("Estado atual do " + me.toString() + " " + state);
                return;
            }
        }
        // Gera um caminho até o incendio
        for (EntityID next : all) {
            List<EntityID> path = planPathToFire(next);
            if (path != null) {
                Logger.info("Movendo para o incendio");
                sendMove(time, path);
                state = state.WALK;
                System.out.println("Estado atual do " + me.toString() + " " + state);
                return;
            }
        }
        List<EntityID> path = null;
        Logger.debug("Não foi possível planejar um caminho até o incendio");
        System.out.println("Não foi possível planejar um caminho até o incendio");
        path = randomWalk();
        Logger.info("Movendo aleatóriamente");
        sendMove(time, path);
        state = state.AVAIABLE;
        System.out.println("Estado atual do " + me.toString() + " " + state);
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }

    // Função que retorna as construções que estão pegando fogo
    private Collection<EntityID> getBurningBuildings() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<Building>();
        
        for (StandardEntity next : e) {
            if (next instanceof Building) {
                Building b = (Building)next;
                if (b.isOnFire() && this.getBuildingDetected().contains(b) == false) {
                	this.getBuildingDetected().add(b);
                    result.add(b);
                }
            }
        }
        // Ordena pela distância
        Collections.sort(result, new DistanceSorter(location(), model));
        return objectsToIDs(result);
    }

    private List<EntityID> planPathToFire(EntityID target) {
        // Tenta pegar tudo que está no alcance
        Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
        if (targets.isEmpty()) {
            return null;
        }
        return search.breadthFirstSearch(me().getPosition(), objectsToIDs(targets));
    }
    
    public Double myWaterQuantity()
	{
		Integer water = me().getWater();
		return water.doubleValue() / this.maxWater;
	}
    
    public ArrayList<Building> getBuildingDetected() {
		return buildingDetected;
	}

	public void setBuildingDetected(ArrayList<Building> buildingDetected) {
		this.buildingDetected = buildingDetected;
	}
}
