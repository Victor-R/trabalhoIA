package sample;

import java.util.List;

import firesimulator.world.FireBrigade;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.messages.components.ChangeSetComponent;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;

/**
   A sample police force agent.
 */
public class SamplePoliceForce extends AbstractSampleAgent<PoliceForce> {
    private static final String DISTANCE_KEY = "clear.repair.distance";

    private int distance;
    private State state = State.PATRULHAR;
	int x = 0;
	int i = 0;
    private String msgFinal;
    private String[] resultado;
    private String entityChamado;
    private String posicaoChamado;
    private EntityID entityCivil;
    private EntityID entityIncendio;
	//private Eventos polEvents = new Eventos('b');
    private boolean civil = false;
    private boolean incendio = false;
    private Blockade escombroAtual;
    private int id;
    private boolean limpar = true;
    private EntityID eventoFila;
    
    public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
    private enum State{
    	PATRULHAR,
    	RETIRAR_ESCOMBRO,
    	EVENTO_EXTERNO,
    	ATENDER_CHAMADO,
    	CHAMADO_FILA
    }
    
    @Override
    public String toString() {
        return "Sample police force";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        System.out.println("FORÇA POLICIAL CONECTADA!");
        distance = config.getIntValue(DISTANCE_KEY);
        
        int[] path0 = {279, 976, 975, 257, 270, 268};
        paths.put(new Integer(11), path0);
        int[] path1 = {268, 256, 296, 297, 257, 273, 969, 279};
        paths.put(new Integer(12), path1);
        int[] path2 = {976, 279, 268, 256, 273, 330, 968};
        paths.put(new Integer(13), path2);
        int[] path3 = {975, 275, 297, 296, 256, 270};
        paths.put(new Integer(14), path3);
        int[] path4 = {279, 976, 975, 257, 270, 268};
        paths.put(new Integer(15), path4);
        int[] path5 = {279, 976, 975, 257, 270, 268};
        paths.put(new Integer(16), path5);
        int[] path6 = {268, 256, 296, 297, 257, 273, 969, 279};
        paths.put(new Integer(17), path6);
        int[] path7 = {976, 279, 268, 256, 273, 330, 968};
        paths.put(new Integer(18), path7);
        int[] path8 = {975, 275, 297, 296, 256, 270};
        paths.put(new Integer(19), path8);
        int[] path9 = {279, 976, 975, 257, 270, 268};
        paths.put(new Integer(20), path9);
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, 1);
        }	            
        
        for (Command next : heard)
        {
        	Logger.debug("Heard" + next);
        	AKSpeak msg = (AKSpeak) next;
        	byte[] msgRaw = msg.getContent();
        	msgFinal = new String (msgRaw);
        	//System.out.println("(A) MENSAGEM RECEBIDA: " + msgFinal);
        	Logger.info("(P) MENSAGEM RECEBIDA: " + msgFinal);
        	resultado = msgFinal.split(" ");
        }
        
        //System.out.println("--------(P-" + this.id + ")ESTADO: " + state);
        
        //System.out.println("(P) ME.BURIEDNESS " + me().getBuriedness());
        
        List<EntityID> caminho;
        
        if(resultado != null && resultado.length == 4)
        {
        	//System.err.println(resultado[1] + " " + resultado[2]);
	        if(resultado[1].equals("3"))
	        {
	        	if(state == State.RETIRAR_ESCOMBRO || !(polEvents.isEmpty()))
	        	{
	        		EntityID escombro = new EntityID(Integer.parseInt(resultado[3]));
	        		//System.out.println("----------(P-" + this.id + ")ADICIONEI UM EVENTO " + resultado[3]);
	        		polEvents.adicionarEvento(model, escombro);
	        		resultado = null;
	        	}
	        	else
	        	{
	        		//System.out.println("(P-" + this.id + ")NAO ADICIONEI NA FILA, INDO ATENDER " + resultado[2]);
	        		entityChamado = resultado[2];
	        		posicaoChamado = resultado[3];
	        		state = State.ATENDER_CHAMADO;
	        		resultado = null;
	        	}
	        }
	  //CONFERE SE ISSO TA FUNCIONANDO DIREITO TBM EM TODOS AGENTES
	        /*else if(resultado.length == 1)
	        {
	        	if(resultado[0].equals("Ouch") || resultado[0].equals("Help")) // ouvir um civil pedindo socorro
	        	{
	        		System.out.println("(P-" + this.id + ")HÁ UM CIVIL PEDINDO SOCORRO POR PERTO!");
	        		entityCivil = new EntityID(0);
	        		civil = true;
	        		state = State.EVENTO_EXTERNO;
	        		resultado = null;
	        	}
	        }*/
        }
        
        //PERCEBER ESCOMBRO
        Blockade target = getTargetBlockade();
		if(target != null)
		{
			//sendClear(time, target.getID());
			escombroAtual = target;
			state = State.RETIRAR_ESCOMBRO;
			if(Crenca.existeIdCrenca(target.getID(), crencas))
			{
				//System.out.println("(P - "+ this.id + ") atendi target " + target.getID());
				Crenca.removerCrenca(target.getID(), crencas);
			}
		}
        
    	//AÇÕES FEITAS PELO RAIO DE PERCEPÇÃO DE AGENTE, UTILIZADO NO CÓDIGO DO RCRSCS(MODIFICADO)
		StandardEntity entity;
		for(EntityID id : changed.getChangedEntities())
		{
			entity = this.model.getEntity(id);
			if(entity instanceof Building){
				Building building = (Building) entity;
				if(building.isFierynessDefined() && building.isBrokennessDefined())
				{
					if(building.getFieryness() > 1)
					{
			    		//VERIFICO SE JA ESTÁ NA CRENÇA. SE JA ESTIVER, NEM ENTRO AQUI E NEM FAÇO NADA
			    		if(!Crenca.existeIdCrenca(building.getID(), crencas))
			    		{
							//System.out.println("(P-" + this.id + ") 2 " + building.getID() + " colocado como crença");
				    		incendio = true;
				    		entityIncendio = building.getID();
				    		Crenca crencaIncendio = new Crenca(building.getID(), building.getID(), 3);
				    		crencas.add(crencaIncendio);
				    		state = State.EVENTO_EXTERNO;
			    		}
					}
				}
			}else if(entity instanceof Civilian){
				Civilian victim = (Civilian) entity;
				if(victim.isPositionDefined() && victim.isHPDefined() && victim.isBuriednessDefined() && victim.isDamageDefined())
				{
					//VERIFICO SE JA ESTÁ NA CRENÇA. SE JA ESTIVER, NEM ENTRO AQUI E NEM FAÇO NADA
					if(!Crenca.existeIdCrenca(victim.getID(), crencas))
					{
						//System.out.println("(P-" + this.id + ") 1 " + victim.getID() + " colocado como crença");
						civil = true;
						entityCivil = victim.getPosition();
			    		Crenca crencaCivil = new Crenca(victim.getID(), victim.getPosition(), 1);
			    		crencas.add(crencaCivil);
						state = State.EVENTO_EXTERNO;
					}
				}
			}
		}
		
        if(location() instanceof Building)
        {
        	Building aux = (Building) location();
        	try{
        		if(aux.isOnFire())
	        		state = State.PATRULHAR;
        	}catch(NullPointerException np)
        	{
        		System.out.println("(EXCEPTION)(P) B NULO!! " + np.getMessage());
        	}
        }
		
        //System.out.println("--------(P-" + this.id + ")APOS PERCEPCAO ESTADO: " + state);
        
        switch(state)
        {
        	case PATRULHAR:
        		if(polEvents.isEmpty())
        		{
        			patrulhar(time);
        	        break;        	        
        		}
        		else
	        	{
        			if(polEvents.proximoEvento() != null)
        			{
	        			if(Crenca.existePosicaoCrenca(polEvents.proximoEvento(), crencas))
	        			{
		        			//VOLTAR A PRINTAR DEPOISSystem.out.println("(P-" + this.id + ") Desenfileirando evento " + polEvents.proximoEvento());
		        			eventoFila = polEvents.executarEvento();
	        				state = State.CHAMADO_FILA;
	        				break;
	        			}
		        		else
		        		{
		        			//System.out.println("---(P-" + this.id + ") BLOQUEIO NAO EXISTE MAIS");
		        			polEvents.executarEvento();// TIRAR O ELEMENTO INVALIDO DA FILA
		        			state = State.PATRULHAR;
		        			break;
		        		}
        			}
	        	}
        	case RETIRAR_ESCOMBRO:
        		if(escombroAtual == null)
        		{
        			if(eventoFila == null)
        				state = State.PATRULHAR;
        			else
        			{
        				System.out.println("---(P-" + this.id + ") VOLTANDO PARA O CHAMADO DA FILA " + eventoFila);
        				state = State.CHAMADO_FILA;
        			}
        		}
        		else
        		{
        			sendClear(time, escombroAtual.getID());
        			Crenca.removerCrenca(escombroAtual.getID(), crencas);
        			escombroAtual = null;
        		}
        		break;
        	case EVENTO_EXTERNO:
	        	if(civil)
	        	{
	        		//VOLTAR A PRINTAR DEPOISSystem.out.println("(P-" + this.id + ") PROTOCOLO 1 " + entityCivil + " " + me().getPosition());
	        		sendSpeak(time, 1, ("(P-" + me().getID() + ") 1 " + entityCivil + " " + me().getPosition()).getBytes());
	        		civil = false;
	        		state = State.PATRULHAR;
	        		break;
	        	}
	        	if(incendio)
	        	{
	        		//VOLTAR A PRINTAR DEPOISSystem.out.println("(P-" + this.id + ") PROTOCOLO 2 " + entityIncendio + " " + me().getPosition());
	        		sendSpeak(time, 1, ("(P-" + me().getID() + ") 2 " + entityIncendio + " " + me().getPosition()).getBytes());
	        		incendio = false;
	        		state = State.PATRULHAR;
	        		break;
	        	}
	        	break;
        	case ATENDER_CHAMADO:
        		List<EntityID> path = null;
        		EntityID eCh = new EntityID(Integer.parseInt(entityChamado));
        		EntityID ePo = new EntityID(Integer.parseInt(posicaoChamado));
    			if(Crenca.existeIdCrenca(eCh, crencas)) // Verifica, pois pode acontecer de outro agente ja ter atendido
    				path = search.breadthFirstSearch(me().getPosition(), ePo); //Cria caminho para o entity da posicao recebida
    			else
    			{
    				state = State.PATRULHAR;
    				break;
    			}
        		if(path != null)
        		{
        			sendMove(time, path);
        			if(me().getPosition().getValue() == ePo.getValue())
        			{
        				//VOLTAR A PRINTAR DEPOISSystem.out.println("(P"+ this.id + ")>>>ATENDI CHAMADO!!!!!!");
        				Crenca.removerCrenca(eCh, crencas);
        				state = State.PATRULHAR;
        			}
        			break;
        		}
        		else
        		{
        			//VOLTAR A PRINTAR DEPOISSystem.out.println("(P-" + this.id + ")NÃO FOI POSSÍVEL ATENDER O CHAMADO!");
        			resultado = null;
        			Crenca.removerCrenca(eCh, crencas);
        			state = State.PATRULHAR;
        			break;
        		}
        		
        	case CHAMADO_FILA:
        		if(Crenca.existePosicaoCrenca(eventoFila, crencas))
        		{
	        		caminho = search.breadthFirstSearch(me().getPosition(), eventoFila);
	        		if(caminho != null)
	        		{
	        			//System.out.println("---(P-" + this.id + ") CAMINHANDO PARA EVENTO DA FILA");
		        		sendMove(time, caminho);
		        		if(me().getPosition().getValue() == eventoFila.getValue())
		        		{
		        			//System.out.println("CHEGUEI NO EVENTO DA FILA");
		        			//VOLTAR A PRINTAR DEPOISSystem.out.println("---(P-" + this.id + ") ATENDI EVENTO DA FILA " + eventoFila);
		        			state = State.PATRULHAR;
		        			eventoFila = null;
		        			break;
		        		}
	        		}
	        		else
	        		{
	        			//VOLTAR A PRINTAR DEPOISSystem.out.println("---(P-" + this.id + ") CAMINHO NULO PARA EVENTO DA FILA");
	        			state = State.PATRULHAR;
	        			break;
	        		}
        		}
        		else
        		{
        			//VOLTAR A PRINTAR DEPOISSystem.out.println("---(P-" + this.id + ") EVENTO DA FILA JA FOI ATENDIDO");
        			state = State.PATRULHAR;
        		}
        		break;
        }
    }
    
	@Override
	public void patrulhar(int time) {
    	int[] vector = paths.get(new Integer(id));
    	List<EntityID> path;
		path = search.breadthFirstSearch(me().getPosition(),new EntityID(vector[i]));
        if(me().getPosition().getValue() == vector[i])
        {
        	i++;
        	if(i > vector.length - 1)
        		i = 0;
        	return;
        }
        sendMove(time, path);
	}
    
    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
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
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        int x = me().getX();
        int y = me().getY();
        for (EntityID next : ids) {
            Blockade b = (Blockade)model.getEntity(next);
            double d = findDistanceTo(b, x, y);
            //            Logger.debug("Distance to " + b + " = " + d);
            if (maxDistance < 0 || d < maxDistance) {
                //                Logger.debug("In range");
                return b;
            }
        }
        //        Logger.debug("No blockades in range");
        return null;
    }

    private int findDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int)best;
    }

    /**
       Get the blockade that is nearest this agent.
       @return The EntityID of the nearest blockade, or null if there are no blockades in the agents current location.
    */
    
    public EntityID getNearestBlockade() {
        return getNearestBlockade((Area)location(), me().getX(), me().getY());
    }
    

    /**
       Get the blockade that is nearest a point.
       @param area The area to check.
       @param x The X coordinate to look up.
       @param y The X coordinate to look up.
       @return The EntityID of the nearest blockade, or null if there are no blockades in this area.
    */
    
    public EntityID getNearestBlockade(Area area, int x, int y) {
        double bestDistance = 0;
        EntityID best = null;
        Logger.debug("Finding nearest blockade");
        if (area.isBlockadesDefined()) {
            for (EntityID blockadeID : area.getBlockades()) {
                Logger.debug("Checking " + blockadeID);
                StandardEntity entity = model.getEntity(blockadeID);
                Logger.debug("Found " + entity);
                if (entity == null) {
                    continue;
                }
                Pair<Integer, Integer> location = entity.getLocation(model);
                Logger.debug("Location: " + location);
                if (location == null) {
                    continue;
                }
                double dx = location.first() - x;
                double dy = location.second() - y;
                double distance = Math.hypot(dx, dy);
                if (best == null || distance < bestDistance) {
                    bestDistance = distance;
                    best = entity.getID();
                }
            }
        }
        Logger.debug("Nearest blockade: " + best);
        return best;
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
    
    private Collection<EntityID> getBurningBuildings() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<Building>();
        for (StandardEntity next : e) {
            if (next instanceof Building) {
                Building b = (Building)next;
                //b.get
                if (b.isOnFire()) {
                    result.add(b);
                }
            }
        }
        // Sort by distance
        Collections.sort(result, new DistanceSorter(location(), model)); //??
        return objectsToIDs(result);
    }
    
    public EntityID getNearestFireBuilding() {
        return getNearestFireBuilding((Area)location(), me().getX(), me().getY());
    }
    
    public EntityID getNearestFireBuilding(Area a, int x, int y)
    {
        EntityID best = null;
        double bestDistance = 0;
        Collection<EntityID> f = getBurningBuildings();
        //List<EntityID> neighbours = a.getNeighbours(); ---> NAO DA PRA USAR PQ NEIGHBOURS RETORNA ROADS
        for (EntityID next : f)
        {
        	Building b = (Building) model.getEntity(next);
        	if(b == null)
        		continue;
        	Pair<Integer, Integer> location = b.getLocation(model);
        	if(location == null)
        		continue;
        	double dx = location.first() - x;
        	double dy = location.second() - y;
        	double distance = Math.hypot(dx, dy);
        	if(distance >= 2900)
        		continue;
            if (best == null || distance < bestDistance) 
            {
                bestDistance = distance;
                best = b.getID();
            }
        }
        return best;
    }
}