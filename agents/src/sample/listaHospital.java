package sample;

public class listaHospital {
	private int OwnerID;
	private int ChildID;
	
	public listaHospital(int Owner,int Child){
		OwnerID = Owner;
		ChildID = Child;
	}
	
	public int getOwner() {
		return OwnerID;
	}
	
	public int getChild() {
		return ChildID;
	}
}
