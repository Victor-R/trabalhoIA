package sample;

import java.util.ArrayList;
import java.util.List;

public class AmbulanceHelper {
	
	public List<listaHospital> hospital = new ArrayList<listaHospital>();
	public int cont = 0;
	
	// função para adicionar registro a lista quando não há o id do civil na lista
	protected boolean addRescue(int ownerID,int childID) {	 
		if(!someoneHasCivilian(childID)){
			hospital.add(cont,new listaHospital(ownerID,childID));	
			return true;
		}else {
			return false;
		}
	}
	
	// função para saber se o owner já está na lista
	protected int hadChild(int ownerID) { 
		for(int i=0; i < hospital.size(); i++) {
			if(hospital.get(i).getOwner() == ownerID) {				
				return i;
			}
		}
		return 0;		
	}
	
	protected void finishRescue(int ownerID,int childID) {
		if(hospital.contains(new listaHospital(ownerID,childID))) {
			hospital.remove(new listaHospital(ownerID,childID));
		}else {
			System.out.println("Não há registro na lista");
		}
	}
	
	protected boolean isOwner(int owner,int child) {			
		if(hospital.isEmpty()) {
			return false;
		}else {
			for(int i=0; i < hospital.size(); i++) {
				if(hospital.get(i).getChild() == child && hospital.get(i).getOwner() == owner) {
					return true;
				}
			}
			return false;
		}			
	}
	
	protected boolean someoneHasCivilian(int childID) {		
		for(int i=0; i < hospital.size(); i++) {
			if(hospital.get(i).getChild() == childID) {
				//System.out.println(hospital.get(i).toString());
				return true;
			}
		}
		return false;		
	}
}
