package ujaen.spslidar.entities;

import lombok.Data;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
public abstract class AbstractDatablock {

    protected int id;
    protected GeorefBox georefBox;
    protected long numberOfPoints;
    protected String UTMZone;
    protected String lazFileAssociated;
    protected List<Integer> children = new ArrayList<>();
    protected GeorefBox UTMZoneLocalGrid;
    protected int depth;
    private ObjectId objectId;



}
