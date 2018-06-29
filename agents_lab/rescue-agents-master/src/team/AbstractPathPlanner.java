package team;

import rescuecore2.worldmodel.EntityID;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AbstractPathPlanner {
    public List<EntityID> plan(EntityID start, EntityID... goals) {
        return plan(start, Arrays.asList(goals));
    }
    public abstract List<EntityID> plan(EntityID start, Collection<EntityID> goals);
}
