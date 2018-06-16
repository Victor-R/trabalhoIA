package team.obstacledetectors;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import team.AbstractObstacleDetector;

public class DummyObstacleDetector extends team.AbstractObstacleDetector {
    StandardWorldModel model;

    public DummyObstacleDetector(StandardWorldModel m)
    {
        super(m);
        model = m;
    }

    public Boolean isPassable(Area _source, Area _destination)
    {
        return _source.getEdgeTo(_destination.getID()) != null;
    }
}
