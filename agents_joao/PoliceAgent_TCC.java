package newAgents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jfree.util.Rotation;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.messages.MessageComponent;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.SampleSearch;

public class PoliceAgent extends AbstractAgent<PoliceForce>{
	public PoliceAgent() {
		rnd = new Random(System.currentTimeMillis());
	}	
	
	private enum State{
		READY,
    	PATROL,
    	REMOVING_BLOCKADE,
    }
	private State state = State.READY;
	private int distance;
	private static final String DISTANCE_KEY = "clear.repair.distance";
	private PoliceForce me;
	private EntityID currentBlockade;
	
    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        System.out.println("FORÇA POLICIAL CONECTADA!");
        distance = config.getIntValue(DISTANCE_KEY);
        me = (PoliceForce) me();
        search = new SampleSearch(model);
    }
	
     /**
      * A percepção de bloqueio por meio do changed não estava sendo muito eficiente, pois o agente
      * precisava criar um caminho até o bloqueio da sua percepção, e este caminho estava dando sempre nulo,
      * e este método "getTargetBLockade()" está funcionando, como funcionava em versões anteriores  
      * do agente.
      */
	@Override
	protected HashMap<StandardEntityURN, List<EntityID>> percept(ChangeSet perceptions) {
		List<EntityID> blocks = new ArrayList<EntityID>();
		List<EntityID> roadsToPatrol = new ArrayList<EntityID>();
		
		Blockade target = getTargetBlockade();
		if(target != null) {
			messages.add(new MessageProtocol(1, "A2C", 'P', me.getID(), 1, 
					(target.getID() + " " + target.getRepairCost())));
			blocks.add(target.getID());
		}
		
		for(EntityID changed : perceptions.getChangedEntities())
			if(model.getEntity(changed).getStandardURN().equals(StandardEntityURN.ROAD)) {
				roadsToPatrol.add(changed);
			}
		
		HashMap <StandardEntityURN, List <EntityID>> selectedPerceptions = new HashMap <StandardEntityURN, List <EntityID>>();
		selectedPerceptions.put(StandardEntityURN.BLOCKADE, blocks);
		selectedPerceptions.put(StandardEntityURN.ROAD, roadsToPatrol);
		return selectedPerceptions;
	}

	@Override
	protected void heardMessage(int time, Collection<Command> heard) {
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY))
            sendSubscribe(time, 1);
    	
        for (Command next : heard) {
        	Logger.debug("Heard" + next);
        	AKSpeak msg = (AKSpeak) next;
        	byte[] msgRaw = msg.getContent();
        	msgFinal = new String (msgRaw);
        	messageSplited = msgFinal.split(" ");
        }
        //if(msgFinal != null)
        	//System.out.println("->(P) MESSAGE RECEIVED: " + msgFinal);
    	/*if(messageResult != null) {
        	for(int i = 0; i < messageResult.length; i++)
        		System.out.print("MESSAGE(SPLIT) RECEIVED: " + messageResult[i] + "\t");
        	System.out.println();
        	if(messageResult.length == 1) {
        		if(messageResult[0] == "Help" || messageResult[0] == "Ouch") {
        			System.out.println(this.getID() + "CIVILIAN ASKING FOR HELP AROUND HERE!!");
        			// CRIAR UM PROTOCOLO NAS PRÓXIMAS FASES
        		}
        	}
        }*/
	}

	/**
	 * Se o agente não estiver no estado READY ou PATROL, então ele está em algum estado
	 * que é sua principal ação, então o primeiro if já retorna da função, mas se não,
	 * ele verifica se há bloqueios a serem limpos, se não há, então ele verifica 
	 * as estradas, nesta ordem.
	 * @param possibleGoals é o HashMap que guarda os possíveis objetivos que o agente irá cumprir, 
	 * e foi preenchido pelo método de percepção do agente.
	 */
	@Override
	protected void deliberate(HashMap<StandardEntityURN, List<EntityID>> possibleGoals) {
		if(state != State.READY && state != State.PATROL)
			return;
		
		if(possibleGoals.get(StandardEntityURN.BLOCKADE).size() > 0) {
			setGoal(StandardEntityURN.BLOCKADE, possibleGoals, State.REMOVING_BLOCKADE);
			return;
		}
		
		if(possibleGoals.get(StandardEntityURN.ROAD).size() > 0) {
			setGoal(StandardEntityURN.ROAD, possibleGoals, State.PATROL);
			return;
		}	
	}

	/**
	 * O estado REMOVING_BLOCKADE utiliza a variável global
	 * "currentBlockade" que é definida no método deliberate() (por meio do método setGoal)
	 * e assim que é feito um "sendClear() com esse bloqueio atual, ele recebe null, para um próximo
	 * bloqueio ser inserido nesta mesma variável e ser limpo também. <br>
	 * O estado PATROL faz uma patrulha nas vias que o agente encontra em seu método de percepção, mas 
	 * só faz esta patrulha se não houver bloqueio a ser retirado.
	 */
	@Override
	protected void act(int time) {
		List <EntityID> path = new ArrayList<EntityID>();
		switch(state) {
			case REMOVING_BLOCKADE:
				if(currentBlockade == null)
					state = State.READY;
				else
					sendClear(time, currentBlockade);
					currentBlockade = null;
				break;
			case PATROL:
				path = search.breadthFirstSearch(location().getID(), goal);
				if(me.getPosition().getValue() == goal.getValue())
					state = State.READY;
				else
					sendMove(time, path);
				break;
		}
		
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		messages.add(new MessageProtocol(1, "A2C", 'P', me.getID(), 0, me.getPosition().toString())); // Código 0 ao Centro
		
		MessageProtocol m = MessageProtocol.setMessagesQueue(messages); // PEGA A PRIMEIRA MENSAGEM POR PRIORIDADE E RETORNA AO OBJETO m
		if (m != null)
			sendSpeak(time, m.getChannel(), (m.getEntireMessage()).getBytes());
		
		heardMessage(time, heard);
		//System.out.println("(P)STATE---> " + state);
		HashMap <StandardEntityURN, List <EntityID>> goals = percept(changed);
		deliberate(goals);
		act(time);		
		System.out.println();
		
	}
	
	/**
	 * Esse método define qual vai ser o EntityID do objetivo do agente:<br>
	 * <li>Caso for um bloqueio, o método define o "currentBlockade", que é o EntityID do bloqueio que irá ser limpo;
	 * <li>Caso for uma via, o método define o "goal", que é o EntityID da via que vai ser criado o caminho.
	 * @param urn é o URN de qual tipo de EntityID, no model atual, para saber qual objetivo será traçado
	 * @param hm é o HashMap que contém os EntityIDs dos objetivos, obtidos pela percepção do agente
	 * @param s é o estado que vai ser definido, ao ser definido o objetivo
	 */
	private void setGoal(StandardEntityURN urn, HashMap<StandardEntityURN, List<EntityID>> hm, State s) {
		if(s.equals(State.REMOVING_BLOCKADE)) {
			currentBlockade = hm.get(urn).get(rnd.nextInt(hm.get(urn).size()));
			state = s;
		}
		else {
			goal = hm.get(urn).get(rnd.nextInt(hm.get(urn).size()));
			state = s;
		}
	}
	
    private Blockade getTargetBlockade() {
        Logger.debug("Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("Looking in current location");
        Blockade result = getTargetBlockade(location, distance);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = getTargetBlockade(location, distance);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Blockade getTargetBlockade(Area area, int maxDistance) {
        if (area == null || !area.isBlockadesDefined())
            return null;
        List<EntityID> ids = area.getBlockades();
        int x = me().getX();
        int y = me().getY();
        for (EntityID next : ids) {
            Blockade b = (Blockade)model.getEntity(next);
            double d = findDistanceTo(b, x, y);
            if (maxDistance < 0 || d < maxDistance)
                return b;
        }
        return null;
    }

    private int findDistanceTo(Blockade b, int x, int y) {
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            if (d < best)
                best = d;

        }
        return (int)best;
    }
}
