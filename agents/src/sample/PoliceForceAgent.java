package sample;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

//import com.sun.xml.internal.stream.Entity;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
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
	private PoliceForce me;
	private State state = State.AVAIABLE;
	private ArrayList<String> msgs = new ArrayList<String>();
	private ArrayList<Blockade> blocks = new ArrayList<>();
	private Boolean limpouRefugio = false;
	private State lastState;
	private int repeatState;
	private int repeatBlock;
	private EntityID lastBlockId;
	
	public PoliceForceAgent() {
		//rnd = new Random(System.currentTimeMillis());
	}
	
	@Override
	public String toString() {
		return "Agente Policial";
	}
	
    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        distance = config.getIntValue(DISTANCE_KEY);
        me = (PoliceForce) me();
        System.out.println("Policial Conectado com sucesso, id: " + me.getID());
        search = new SampleSearch(model);
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
        		//System.out.println(txt);
        	}catch(UnsupportedEncodingException uex) {
        		Logger.error("to aqui" + uex.getMessage());
        	}
        }
	}

	protected void act(int time) {
		System.out.println("repeat: " + repeatState + " " + me.getID());
		if(repeatBlock == 4) {
			Blockade ignore = (Blockade)model.getEntity(lastBlockId);
			blocks.add(ignore);
			System.out.println("Ignorando o bloco: " + lastBlockId.getValue());
			listaBlocos.rmResponsability(me.getID().getValue(), lastBlockId.getValue());
			repeatBlock = 0;
		}
		if(repeatState == 6 && (state == State.WALK || state == State.AVAIABLE)) {
			state = State.AVAIABLE;
			repeatState = 0;
			lastState = state;
			sendMove(time, randomWalk());
			return;
		}
			
		// Verifica se está próximo de um bloqueio
        Blockade target = getTargetBlockade();
        if (target != null) {
        	if(!listaBlocos.someoneHasThisResponsability(target.getID().getValue())) {
        		System.out.println("Assumindo o bloco: " + target.getID().getValue());
        		listaBlocos.addResponsability(me.getID().getValue(), target.getID().getValue());
        	}
        	
        	if(listaBlocos.isOwner(me.getID().getValue(), target.getID().getValue())) {
	            Logger.info("Limpando o bloqueio " + target);
	            System.out.println("Target id: " + target.getID().getValue() 
	    		+ " RepairCost: " + target.getRepairCost()
	    		+ " Apexes: " + target.getApexes()
	    		+ " ApexesProperty: " + target.getApexesProperty());
	            
	            if(lastBlockId != null && target.getID().getValue() == lastBlockId.getValue())
	            	repeatBlock++;
	            else
	            {
	            	lastBlockId = target.getID();
	            	repeatBlock = 0;
	            }
	            sendClear(time, target.getID());
	            state = state.CLEAR;
	            if(lastState == state)
	            	repeatState++;
	            else
	            	repeatState = 0;
	            lastState = state;
	            return;
        	}
        	else {
        		System.out.println("Alguém já tem esse bloco");
        	}
        }
        if(limpouRefugio == false) {
        	System.out.println("Precisa limpar refugio");
        	// Verifica qual é o bloqueio mais próximo para limpar a area até p refúgio
        	if(refugeIDs.isEmpty() == false)
        	{
        		List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
    			sendMove(time, path);
    			state = state.WALK;
    			// 	Verifica se está próximo de um bloqueio
        		Blockade target2 = getTargetBlockade();
        		if (target2 != null) {
            		Logger.info("Limpando bloqueio " + target2);
            		System.out.println("Target id: " + target2.getID() 
            		+ " Tamanho: " + target2.getRepairCost() 
            		+ " Apexes: " + target2.getApexes()
            		+ " ApexesProperty: " + target2.getApexesProperty());
            		sendClear(time, target2.getID());
            		state = state.CLEAR;
            		if(lastState == state)
                    	repeatState++;
                    else
                    	repeatState = 0;
                    lastState = state;
            		return;
        		}
        		if(lastState == state)
                	repeatState++;
                else
                	repeatState = 0;
                lastState = state;
        	}
        	limpouRefugio = true;
        }
        
        // Tenta fazer um caminho até o bloqueio mais próximo
        List<EntityID> path = search.breadthFirstSearch(me().getPosition(), getBlockedRoads());
        if (path != null) {
            Logger.info("Movendo para o bloqueio");
            System.out.println("Movendo para o bloqueio");
            Road r = (Road)model.getEntity(path.get(path.size() - 1));
            Blockade b = getTargetBlockade(r, -1);
            if(b != null)
            {
            	sendMove(time, path, b.getX(), b.getY());
            	Logger.debug("caminho: " + path);
                Logger.debug("coordenadas do objetivo: " + b.getX() + ", " + b.getY());
                state = State.WALK;
                if(lastState == state)
                	repeatState++;
                else
                	repeatState = 0;
                lastState = state;
                return;
            }
        }
        
        // Tenta fazer um caminho até o bloqueio mais próximo que ele é dono
        path = search.breadthFirstSearch(me().getPosition(), listaBlocos.getAllChilds(me.getID().getValue()));
        /*
        if (path != null) {
            Logger.info("Movendo para o bloqueio que sou dono");
            System.out.println("Movendo para o bloqueio que sou dono");
            Road r = (Road)model.getEntity(path.get(path.size() - 1));
            Blockade b = getTargetBlockade(r, -1);
            if(b != null)
            {
            	sendMove(time, path, b.getX(), b.getY());
            	Logger.debug("caminho: " + path);
                Logger.debug("coordenadas do objetivo: " + b.getX() + ", " + b.getY());
                state = State.WALK;
                if(lastState == state)
                	repeatState++;
                else
                	repeatState = 0;
                lastState = state;
                return;
            }
        }
        */
        for(EntityID id : listaBlocos.getAllChilds(me.getID().getValue())) {
        	System.out.println("Procurando nos bloqueios que sou dono");
        	Blockade b = (Blockade) model.getEntity(id);
        	if(b != null) {
        		System.out.println("Tentando encontra um caminho");
        		path = search.breadthFirstSearch(me.getPosition(), id);
        		if(path != null) {
        			System.out.println("Encontrei um caminho");
        			sendMove(time, path, b.getX(), b.getY());
                	Logger.debug("caminho: " + path);
                    Logger.debug("coordenadas do objetivo: " + b.getX() + ", " + b.getY());
                    state = State.WALK;
                    if(lastState == state)
                    	repeatState++;
                    else
                    	repeatState = 0;
                    lastState = state;
                    return;
        		}
        		else 
        			listaBlocos.rmResponsability(me.getID().getValue(), id.getValue());
        	}
        }
        
        Logger.debug("Não foi possivel encontrar um caminho até o bloqueio");
        Logger.info("Movendo aleatoriamente");
        state = state.AVAIABLE;
        if(lastState == state)
        	repeatState++;
        else
        	repeatState = 0;
        lastState = state;
        sendMove(time, randomWalk());
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_FORCE);
	}

	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {	
		heardMessage(time, heard);		
		String pos = String.valueOf(me().getPosition().getValue());
		try {
			sendSpeak(time, 1, ("P" + pos).getBytes("UTF-8"));
			System.out.println("Enviado");
		} catch (UnsupportedEncodingException e) {
			Logger.error("UnsupportedEnconding " + e.getMessage());
		}
		
		act(time);
		System.out.println("Estado atual do " + me.toString() + " " + state);
	}
	
    private List<EntityID> getBlockedRoads() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
        List<EntityID> result = new ArrayList<EntityID>();
        for (StandardEntity next : e) {
            Road r = (Road)next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                result.add(r.getID());
            }
        }
        return result;
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
            if(blocks.contains(b)) {
            	System.out.println("Esse bloco já foi visitado mais de 4x");
            	continue;
            }
            if(listaBlocos.someoneHasThisResponsability(b.getID().getValue()) && 
            		!listaBlocos.isOwner(me.getID().getValue(), b.getID().getValue())) {
            	System.out.println("Alguém já é dono desse bloco");
            	continue;
            }
            	
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