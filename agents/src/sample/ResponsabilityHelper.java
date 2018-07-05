package sample;

import java.util.ArrayList;
import java.util.List;

import rescuecore2.worldmodel.EntityID;

public class ResponsabilityHelper {
	
	public List<Responsability> responsibilities = new ArrayList<Responsability>();
	
	// função para adicionar registro a lista quando não há o id do civil/bloco na lista
	public boolean addResponsability(int ownerID,int childID) {	 
		if(!someoneHasThisResponsability(childID)){
			responsibilities.add(new Responsability(ownerID,childID));	
			return true;
		}else {
			return false;
		}
	}
	
	public void rmResponsability(int ownerID) {
		for(int i=0; i<responsibilities.size(); i++) {
			if(responsibilities.get(i).getOwner() == ownerID) {
				responsibilities.remove(i);
				//responsibilities.get(i).setOwner(0);
				//responsibilities.get(i).setChild(0);
			}
		}
	}
	
	public void rmResponsability(int ownerID,int childID) {
		for(int i=0; i < responsibilities.size(); i++) {
			if(responsibilities.get(i).getOwner() == ownerID && responsibilities.get(i).getChild() == childID) {
				responsibilities.remove(i);
				return;
			}
		}
		System.out.println("Não existe esse registro na lsita");
	}
	
	public void printResponsibilities() {
		System.out.println("{");
		for(int i=0; i<responsibilities.size(); i++) {
			System.out.println("Responsável: " + responsibilities.get(i).getOwner()
					+ " - Objeto: " + responsibilities.get(i).getChild());
		}
		System.out.println("}");
	}
	// função para saber se o owner já está na lista
	public int hadChild(int ownerID) { 
		for(int i=0; i < responsibilities.size(); i++) {
			if(responsibilities.get(i).getOwner() == ownerID) {				
				return i;
			}
		}
		return 0;		
	}	

	
	public boolean isOwner(int owner,int child) {			
		if(responsibilities.isEmpty()) {
			return false;
		}else {
			for(int i=0; i < responsibilities.size(); i++) {
				if(responsibilities.get(i).getChild() == child && responsibilities.get(i).getOwner() == owner) {
					return true;
				}
			}
			return false;
		}			
	}
	
	public boolean someoneHasThisResponsability(int childID) {		
		for(int i=0; i < responsibilities.size(); i++) {
			if(responsibilities.get(i).getChild() == childID) 
				return true;
		}
		return false;		
	}
	
	protected List<EntityID> getAllChilds(int owner){
		List<EntityID> childs = new ArrayList<EntityID>();
		for(int i=0; i < responsibilities.size(); i++) {
			if(responsibilities.get(i).getOwner() == owner)
				childs.add(new EntityID(responsibilities.get(i).getChild()));
		}
		return childs;
	}
}
