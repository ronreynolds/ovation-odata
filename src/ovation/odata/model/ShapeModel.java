package ovation.odata.model;

import java.util.HashMap;

import org.core4j.Func1;

import com.google.common.collect.Maps;

import ovation.IShape;
import ovation.odata.model.OvationModelBase.CollectionName;
import ovation.odata.model.OvationModelBase.PropertyName;

public class ShapeModel extends ExtendedPropertyModel<IShape> {  // can't extend OvationModelBase<IShape> because IShape doesn't extend EntityBase
    static final HashMap<String,Class<?>> _propertyTypeMap   = Maps.newHashMap();
    static final HashMap<String,Class<?>> _collectionTypeMap = Maps.newHashMap();
    static { OvationModelBase.addIShape(_propertyTypeMap, _collectionTypeMap); }

    public ShapeModel() { 
        super(_propertyTypeMap, _collectionTypeMap, "key");  // FIXME - total hack (must have have a key to be a top-level entity)
        // setAllGetter(); - default returns nothing
        setIdGetter(new Func1<IShape,String>() {
            public String apply(IShape record) {
                return record.toString();   // doesn't have an ID actually
            }       
        });
    }
    
    public String       entityName()                            { return "_Shapes"; }
    public Iterable<?>  getCollectionValue(Object o, String c)  { return OvationModelBase.getCollection((IShape)o, CollectionName.valueOf(c)); }
    public Object       getPropertyValue(Object o, String p)    { if ("key".equals(p)) return String.valueOf(o); return OvationModelBase.getProperty((IShape)o, PropertyName.valueOf(p)); } // FIXME - total hack
    public Class<IShape> getEntityType()                        { return IShape.class; }
    public String       getTypeName()                           { return "Shape"; }
}