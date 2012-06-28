package ovation.odata.model;

import java.util.HashMap;

import org.core4j.Func;
import org.core4j.Func1;

import ovation.CoordinateSystem;

import com.google.common.collect.Maps;

public class CoordinateSystemModel extends OvationModelBase<CoordinateSystem> {
    static final HashMap<String,Class<?>> _propertyTypeMap   = Maps.newHashMap();
    static final HashMap<String,Class<?>> _collectionTypeMap = Maps.newHashMap();
    static { addCoordinateSystem(_propertyTypeMap, _collectionTypeMap); }

    public CoordinateSystemModel() { 
        super(_propertyTypeMap, _collectionTypeMap);
        setAllGetter(new Func<Iterable<CoordinateSystem>>() { 
            public Iterable<CoordinateSystem> apply() { 
                final Iterable<CoordinateSystem> queryIter = executeQueryInfo();
                if (queryIter != null) {
                    return queryIter;
                }
                return executeQuery(GET_ALL_PQL); 
            } 
        });
        setIdGetter(new Func1<CoordinateSystem,String>() {
            public String apply(CoordinateSystem record) {
                return record.getUuid();
            }       
        });
    }
    
    public String       entityName()                            { return "CoordinateSystems"; }
    public Iterable<?>  getCollectionValue(Object o, String c)  { return getCollection((CoordinateSystem)o, CollectionName.valueOf(c)); }
    public Object       getPropertyValue(Object o, String p)    { return getProperty((CoordinateSystem)o, PropertyName.valueOf(p)); }
    public Class<CoordinateSystem> getEntityType()              { return CoordinateSystem.class; }
    public String       getTypeName()                           { return "CoordinateSystem"; }
}