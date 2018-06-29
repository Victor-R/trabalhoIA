package team;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public abstract class AbstractObstacleDetector {
    StandardWorldModel model;
    protected AbstractObstacleDetector(StandardWorldModel m)
    {
        model = m;
    }
    public Boolean isPassable(EntityID _source, EntityID _destination)
    {
        Entity source       = model.getEntity(_source);
        Entity destination  = model.getEntity(_destination);
        return isPassable(source, destination);
    }
    public Boolean isPassable(Entity _source, Entity _destination)
    {
        if(_source instanceof Area && _destination instanceof  Area)
        {
            return isPassable((Area)_source, (Area)_destination);
        }
        return false;
    }
    public abstract Boolean isPassable(Area _source, Area _destination);
}
