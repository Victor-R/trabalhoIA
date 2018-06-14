package firesimulator.util;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import firesimulator.world.StationaryObject;

/**
 * @author tn
 *
 */
public class Geometry {

	private static Rectangle rect=new Rectangle(0,0,0,0);

	public static int percent(float x1,float y1, float width, float height,Polygon p){
		int counter=0;
		double dx=width/10;
		double dy=height/10;
		for(int i=0;i<10;i++)
			for(int j=0;j<10;j++){
				if(p.contains(dx*i+x1,dy*j+y1))counter++;
			}
		return counter;
	}
	
	public static boolean boundingTest(Polygon p,int x,int y,int w,int h){
		rect.setBounds(x,y,w,h);
		return p.intersects(rect);		
	}

	public static Point intersect(Point a, Point b, Point c, Point d){
		float[] rv=intersect(new float[]{a.x,a.y,b.x,b.y,c.x,c.y,d.x,d.y});
		if(rv==null)return null;
		return new Point((int)rv[0],(int)rv[1]);
	}

	public static float[] intersect(float[]points){
		float[] l1=getAffineFunction(points[0],points[1],points[2],points[3]);
		float[] l2=getAffineFunction(points[4],points[5],points[6],points[7]);
		float[] crossing;
		if(l1==null&&l2==null){						
			return null;
		}
		else if(l1==null&&l2!=null) {			
			crossing= intersect(l2[0],l2[1],points[0]);
		}
		else if(l1!=null&&l2==null){			
			crossing= intersect(l1[0],l1[1],points[4]);
		}
		else{						
			crossing =intersect(l1[0],l1[1],l2[0],l2[1]);
		}
		if (crossing==null){			
			return null;
		}
		if(!(inBounds(points[0],points[1],points[2],points[3],crossing[0],crossing[1])&&
		inBounds(points[4],points[5],points[6],points[7],crossing[0],crossing[1]))) return null;
		return crossing;
	}

	public static float[] getAffineFunction(float x1,float y1,float x2,float y2){
		if(x1==x2)return null;
		float m=(y1-y2)/(x1-x2);
		float b=y1-m*x1;
		return new float[]{m,b};		
	}
	
	public static float getLength(Point2D a, Point2D b){
		a.distance(b);
		return 0;
	}
	
	public static float[] intersect(float m1, float b1, float m2, float b2){
		if(m1==m2){			
			return null;
		}
		float x=(b2-b1)/(m1-m2);
		float y=m1*x+b1;		
		return new float[]{x,y};
	}
	
	public static float[] intersect(float m1, float b1, float x){
		return new float[]{x, m1*x+b1};
	}
	
	public static boolean inBounds(float bx1,float by1,float bx2, float by2, float x, float y){
		if(bx1<bx2){
			if(x<bx1||x>bx2)return false;
		}else{			
			if(x>bx1||x<bx2)return false;
		}
		if(by1<by2){
			if(y<by1||y>by2)return false;
		}else{
			if(y>by1||y<by2)return false;
		}					
		return true;	
	}

    /**
     * Returns a random point on a line
     * @param a One point defineing the line
     * @param b The other point defineing the line
     * @return A point between a and b
     */
	public static Point getRndPoint(Point a, Point b){		
		float[] mb=Geometry.getAffineFunction((float)a.x,(float)a.y,(float)b.x,(float)b.y);		
		float dx=(Math.max((float)a.x,(float)b.x)-Math.min((float)a.x,(float)b.x));
		dx*=Rnd.get01();
		dx+=Math.min((float)a.x,(float)b.x);		
		if(mb==null){
            //vertical line
            int p = Math.max(a.y,b.y)-Math.min(a.y,b.y);
            p = (int) (p*Math.random());
            p = p + Math.min(a.y,b.y);
            return new Point(a.x,p);
        }
		float y=mb[0]*dx+mb[1];
		Point rtv=new Point((int)dx,(int)y);
        if(rtv == null){
            System.currentTimeMillis();
        }
		return rtv;
	}
	
	public static Point getRndPoint(Point a, double length){		
		double angel=Rnd.get01()*2d*Math.PI;
		double x=Math.sin(angel)*length;
		double y=Math.cos(angel)*length;		
		return new Point((int)x+a.x,(int)y+a.y);
	}

	public static int dist(StationaryObject o1, StationaryObject o2){
	    double x = o1.getX()-o2.getX();
	    double y = o1.getY()-o2.getY();
	    return (int )Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
	}
}
