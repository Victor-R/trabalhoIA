package sample;

import java.util.ArrayList;
import java.util.List;

public class AmbulanceHelper {
	
	public List<listaHospital> hospital = new ArrayList<listaHospital>();
	public int cont = 0;
	
	// função para adicionar registro a lista quando não há o id do civil na lista
	public boolean addRescue(int ownerID,int childID) {	 
		if(!someoneHasCivilian(childID)){
			hospital.add(cont,new listaHospital(ownerID,childID));	
			return true;
		}else {
			return false;
		}
	}
	
	public void rmRescue(int ownerID) {
		for(int i=0; i<hospital.size(); i++) {
			if(hospital.get(i).getOwner() == ownerID) {
				hospital.get(i).setOwner(0);
				hospital.get(i).setChild(0);
			}
		}
	}
	
	public void printHospital() {
		System.out.println("{");
		for(int i=0; i<hospital.size(); i++) {
			System.out.println("Ambulancia:"+hospital.get(i).getOwner()+" - Vitima:"+hospital.get(i).getChild());
		}
		System.out.println("}");
	}
	// função para saber se o owner já está na lista
	public int hadChild(int ownerID) { 
		for(int i=0; i < hospital.size(); i++) {
			if(hospital.get(i).getOwner() == ownerID) {				
				return i;
			}
		}
		return 0;		
	}	

	
	public boolean isOwner(int owner,int child) {			
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
	
	public boolean someoneHasCivilian(int childID) {		
		for(int i=0; i < hospital.size(); i++) {
			if(hospital.get(i).getChild() == childID) {
				//System.out.println(hospital.get(i).toString());
				return true;
			}
		}
		return false;		
	}
}
