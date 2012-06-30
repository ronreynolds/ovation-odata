package ovation.odata.model;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.core4j.Func;
import org.core4j.Func1;
import org.odata4j.core.OAtomStreamEntity;

import ovation.Resource;
import ovation.odata.service.servlet.MediaServlet;
import ovation.odata.util.TypeDataMap;
import ovation.odata.util.TypeDataMap.TypeData;

import com.google.common.collect.Maps;

/**
 * presents Resource data to the OData4J framework
a * @author Ron
 */
public class ResourceModel extends OvationModelBase<Resource> {
    static final Logger _log = Logger.getLogger(ResourceModel.class);

    static final HashMap<String,Class<?>> _propertyTypeMap      = Maps.newHashMap();
    static final HashMap<String,Class<?>> _collectionTypeMap = Maps.newHashMap();
    
    static { addResource(_propertyTypeMap, _collectionTypeMap);    }
    
    public ResourceModel()     { super(_propertyTypeMap, _collectionTypeMap); }
    
    public String               entityName()     { return "Resources"; }
    public String               getTypeName()    { return "Resource"; }
    public Class<Resource>      getEntityType()  { return Resource.class; }
    
    public boolean              hasStream()      { return true; }
    public OAtomStreamEntity    getStreamHandler(final Resource obj) {
        _log.warn("getStreamHandler(" + obj + ")");
        // if we don't want to allow streams to numeric types we can return null here
        final String uti = obj.getUti();
        // this type doesn't get a stream
        if (TypeDataMap.NUMERIC_DATA_UTI.equals(uti)) return null;
        
        final TypeData typeData = TypeDataMap.getUTIData(uti);
        if (typeData == null) {
            _log.warn("unknown uti = " + uti);
             return null;
        }
        
        final String url  = MediaServlet.generateUrl(obj);
        final String mime = typeData.getMimeType();
        if (_log.isInfoEnabled()) {
            _log.info(obj + " - mime:" + mime + ", url:" + url);
        }
        return new OAtomStreamEntity() {
            public String getAtomEntitySource() { return url; }
            public String getAtomEntityType()   { return mime; }
        }; 
    }

    
    public Iterable<?>  getCollectionValue(Object target, String collectionName){ return getCollection((Resource)target, CollectionName.valueOf(collectionName)); }
    public Object       getPropertyValue(Object target, String propertyName)    { return getProperty((Resource)target,   PropertyName.valueOf(propertyName)); }

    public Func<Iterable<Resource>> allGetter() {
        return new Func<Iterable<Resource>>() { 
            public Iterable<Resource> apply() { 
                final Iterable<Resource> queryIter = executeQueryInfo();
                if (queryIter != null) {
                    return queryIter;
                }
                return executeQuery(GET_ALL_PQL); 
            } 
        };
    }

    public Func1<Resource,String> idGetter() {
        return new Func1<Resource,String>() {
            public String apply(Resource record) {
                return record.getUuid();
            }        
        };
    }
}