package team;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;

public class SampleLayer extends StandardViewLayer {
    protected List<Area> entities;

    public SampleLayer() {
    	entities = new ArrayList<Area>();
    }

    @Override
    public Rectangle2D view(Object... objects) {
        synchronized (entities) {
            entities.clear();
            Rectangle2D result = super.view(objects);
            return result;
        }
    }

    @Override
    protected void viewObject(Object o) {
        super.viewObject(o);
        if(o instanceof Area) {
            entities.add((Area)o);
        }
        if (o instanceof WorldModel) {
            WorldModel<? extends Entity> wm = (WorldModel<? extends Entity>)o;
            for (Entity next : wm) {
                viewObject(next);
            }
        }
    }

	@Override
	public Collection<RenderedObject> render(Graphics2D g,
			ScreenTransform transform, int width, int height) {
        synchronized (entities) {
            Collection<RenderedObject> result = new ArrayList<RenderedObject>();
            for (Area next : entities) {
            	for(EntityID neighbour_id : next.getNeighbours())
            	{
            		if(neighbour_id.getValue() < next.getID().getValue())
            		{
            			Entity e = world.getEntity(neighbour_id);
            			if(e instanceof Area)
            			{
            				Area neighbour = (Area)e;
            				Line2D shape = new Line2D.Double(
            						transform.xToScreen(next.getX()), transform.yToScreen(next.getY()),
            						transform.xToScreen(neighbour.getX()), transform.yToScreen(neighbour.getY()));
            				g.setColor(Color.green);
            				g.draw(shape);
            				result.add(new RenderedObject(next, shape));
            			}
            		}
            	}
            }
            return result;
        }
	}

	@Override
	public String getName() {
		return "Sample";
	}

}
