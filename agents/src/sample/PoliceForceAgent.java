package sample;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.SampleSearch;

public class PoliceForceAgent extends AbstractSampleAgent<PoliceForce>{
	
	private enum State{
		AVAIABLE,
    	WALK,
    	CLEAR,
    }
	
    private static final String DISTANCE_KEY = "clear.repair.distance";
    private int distance;
    private Random rnd;
	private PoliceForce me;
	private EntityID currentBlockade;
	private State state = State.AVAIABLE;
	private EntityID goal;
	private ArrayList<String> msgs = new ArrayList<String>();
    
	public PoliceForceAgent() {
		rnd = new Random(System.currentTimeMillis());
	}
	
    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        System.out.println("Policial Conectado com sucesso");
        distance = config.getIntValue(DISTANCE_KEY);
        me = (PoliceForce) me();
        search = new SampleSearch(model);
    }
    
	protected HashMap<StandardEntityURN, List<EntityID>> percept(ChangeSet perceptions) {
		List<EntityID> blocks = new ArrayList<EntityID>();
		List<EntityID> roadsToVerify = new ArrayList<EntityID>();
		
		Blockade target = getTargetBlockade();
		if(target != null) {
			//messages.add(new MessageProtocol(1, "A2C", 'P', me.getID(), 1, 
			//		(target.getID() + " " + target.getRepairCost())));
			blocks.add(target.getID());
		}
		
		for(EntityID changed : perceptions.getChangedEntities())
			if(model.getEntity(changed).getStandardURN().equals(StandardEntityURN.ROAD))
				roadsToVerify.add(changed);
		
		HashMap <StandardEntityURN, List <EntityID>> selectedPerceptions = new HashMap <StandardEntityURN, List <EntityID>>();
		selectedPerceptions.put(StandardEntityURN.BLOCKADE, blocks);
		selectedPerceptions.put(StandardEntityURN.ROAD, roadsToVerify);
		return selectedPerceptions;
	}
	
	protected void heardMessage(int time, Collection<Command> heard) {
    	if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY))
            sendSubscribe(time, 1);
    	
    	msgs.clear();
    	
        for (Command next : heard) {
        	try {
        		Logger.debug("Heard" + next);
        		byte[] content = ((AKSpeak) next).getContent();
        		String txt = new String(content, "UTF-8");
        		msgs.add(txt);
        		System.out.println(txt);
        	}catch(UnsupportedEncodingException uex) {
        		Logger.error(uex.getMessage());
        	}
        	//msgFinal = new String (msgRaw);
        	//messageSplited = msgFinal.split(" ");
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
	
	protected void deliberate(HashMap<StandardEntityURN, List<EntityID>> possibleGoals) {
		if(state != State.AVAIABLE && state != State.WALK)
			return;
		
		if(possibleGoals.get(StandardEntityURN.BLOCKADE).size() > 0) {
			setGoal(StandardEntityURN.BLOCKADE, possibleGoals, State.CLEAR);
			return;
		}
		
		if(possibleGoals.get(StandardEntityURN.ROAD).size() > 0) {
			setGoal(StandardEntityURN.ROAD, possibleGoals, State.WALK);
			return;
		}	
	}

	protected void act(int time) {
		List <EntityID> path = new ArrayList<EntityID>();
		switch(state) {
			case CLEAR:
				if(currentBlockade == null)
					state = State.AVAIABLE;
				else
					sendClear(time, currentBlockade);
					currentBlockade = null;
				break;
			case WALK:
				path = search.breadthFirstSearch(location().getID(), goal);
				if(me.getPosition().getValue() == goal.getValue())
					state = State.AVAIABLE;
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
		//messages.add(new MessageProtocol(1, "A2C", 'P', me.getID(), 0, me.getPosition().toString())); // Código 0 ao Centro
		
		//MessageProtocol m = MessageProtocol.setMessagesQueue(messages); // PEGA A PRIMEIRA MENSAGEM POR PRIORIDADE E RETORNA AO OBJETO m
		//if (m != null)
		//	sendSpeak(time, m.getChannel(), (m.getEntireMessage()).getBytes());
		
		heardMessage(time, heard);
		System.out.println("(P)STATE---> " + state);
		HashMap <StandardEntityURN, List <EntityID>> goals = percept(changed);
		deliberate(goals);
		
		String pos = String.valueOf(me().getPosition().getValue());
		try {
			sendSpeak(time, 1, ("P" + pos).getBytes("UFT-8"));
			System.out.println("Enviado");
		} catch (UnsupportedEncodingException e) {
			Logger.error(e.getMessage());
		}
		
		act(time);		
		System.out.println();
	}
	
	private void setGoal(StandardEntityURN urn, HashMap<StandardEntityURN, List<EntityID>> hm, State s) {
		if(s.equals(State.CLEAR)) {
			currentBlockade = hm.get(urn).get(rnd.nextInt(hm.get(urn).size()));
			state = s;
		}
		else {
			goal = hm.get(urn).get(rnd.nextInt(hm.get(urn).size()));
			System.out.println(goal);
			state = s;
		}
	}
	
    private Blockade getTargetBlockade() {
        Logger.debug("Procurando pelo bloqueio");
        Area location = (Area)location();
        Logger.debug("Procurando na loc atual");
        Blockade result = getTargetBlockade(location, distance);
        if (result != null) {
            return result;
        }
        Logger.debug("Procurando nos vizinhos");
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