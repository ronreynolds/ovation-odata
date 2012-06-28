package ovation.odata.model;

import java.util.HashMap;

import org.core4j.Func;
import org.core4j.Func1;

import ovation.Group;

import com.google.common.collect.Maps;

public class GroupModel extends OvationModelBase<Group> {
    static final HashMap<String,Class<?>> _propertyTypeMap   = Maps.newHashMap();
    static final HashMap<String,Class<?>> _collectionTypeMap = Maps.newHashMap();
    static { addGroup(_propertyTypeMap, _collectionTypeMap); }

    public GroupModel() { 
        super(_propertyTypeMap, _collectionTypeMap);
        setAllGetter(new Func<Iterable<Group>>() { 
            public Iterable<Group> apply() { 
                final Iterable<Group> queryIter = executeQueryInfo();
                if (queryIter != null) {
                    return queryIter;
                }
                return executeQuery(GET_ALL_PQL); 
            } 
        });
        setIdGetter(new Func1<Group,String>() {
            public String apply(Group record) {
                return record.getUuid();
            }       
        });
    }
    
    public String       entityName()                            { return "Groups"; }
    public Iterable<?>  getCollectionValue(Object o, String c)  { return getCollection((Group)o, CollectionName.valueOf(c)); }
    public Object       getPropertyValue(Object o, String p)    { return getProperty((Group)o, PropertyName.valueOf(p)); }
    public Class<Group> getEntityType()                         { return Group.class; }
    public String       getTypeName()                           { return "Group"; }
}
