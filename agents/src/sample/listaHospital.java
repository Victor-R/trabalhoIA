package sample;

public class listaHospital {
	private int OwnerID;
	private int ChildID;
	
	public listaHospital(int Owner,int Child){
		this.OwnerID = Owner;
		this.ChildID = Child;
	}
	
	public int getOwner() {
		return this.OwnerID;
	}
	
	public int getChild() {
		return this.ChildID;
	}
	
	@Override
	public String toString() {
		return "OwnerId = " + this.OwnerID + " ChildId = " + this.ChildID;
	}
}