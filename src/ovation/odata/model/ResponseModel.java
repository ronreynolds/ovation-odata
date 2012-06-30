package ovation.odata.model;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.core4j.Func;
import org.core4j.Func1;
import org.odata4j.core.OAtomStreamEntity;

import ovation.Response;
import ovation.odata.service.servlet.MediaServlet;
import ovation.odata.util.TypeDataMap;
import ovation.odata.util.TypeDataMap.TypeData;

import com.google.common.collect.Maps;

/**
 * an adapter of Response to OData4J's streaming interface - this class isn't ready - the design feels wrong
 * (creating a decorator around an IEntityBase instance to add OAtomStreamEntity as a base type so that
 * Odata4j will recognize that it supports streaming - current thinking is to change how InMemoryProducer
 * handles streams and not make it so tightly coupled to the entity (a stream factory or annotation perhaps)
 * 
 * @author Ron
 * /
class StreamingResponse extends IEntityBaseAdapter implements OAtomStreamEntity {
    StreamingResponse(Response res) { super(res); }
    /**
     * @return ??
     * /
    @Override
    public String getAtomEntitySource() {
        ResponseModel._log.info("getAtomEntitySource() called from ", new Throwable());
        return null;
    }
    /**
     * @return ??
     * /
    @Override
    public String getAtomEntityType() {
        ResponseModel._log.info("getAtomEntityType() called from ", new Throwable());
        return null;
    }
}

/**
 * presents Response data to the OData4J framework
 * @author Ron
 */
public class ResponseModel extends OvationModelBase<Response> {
    static final Logger _log = Logger.getLogger(ResponseModel.class);

    static final HashMap<String,Class<?>> _propertyTypeMap      = Maps.newHashMap();
    static final HashMap<String,Class<?>> _collectionTypeMap = Maps.newHashMap();
    
    static { addResponse(_propertyTypeMap, _collectionTypeMap); }
    
    public ResponseModel()                  { super(_propertyTypeMap, _collectionTypeMap); }
    public String           getTypeName()   { return "Response"; }
    public String           entityName()    { return "Responses"; }
    public Class<Response>  getEntityType() { return Response.class; }
    
    public Iterable<?>  getCollectionValue(Object target, String collectionName){ return getCollection((Response)target, CollectionName.valueOf(collectionName)); }
    public Object       getPropertyValue(Object target, String propertyName)    { return getProperty((Response)target,   PropertyName.valueOf(propertyName)); }
    
    public boolean              hasStream()      { return true; }
    public OAtomStreamEntity    getStreamHandler(final Response obj) {
        // if we don't want to allow streams to numeric types we can return null here
        final String uti = obj.getUTI();
        if (TypeDataMap.NUMERIC_DATA_UTI.equals(uti)) return null;

        final TypeData typeData = TypeDataMap.getUTIData(uti);
        
        return new OAtomStreamEntity() {
            public String getAtomEntitySource() { 
                return MediaServlet.generateUrl(obj); 
            }
            public String getAtomEntityType()   { 
                return (typeData != null) ? typeData.getMimeType() : null;
            }
        }; 
    }
    
    public Func<Iterable<Response>> allGetter() {
        return new Func<Iterable<Response>>() { 
            public Iterable<Response> apply() { 
                final Iterable<Response> queryIter = executeQueryInfo();
                if (queryIter != null) {
                    // wrap Responses in StreamingResponse objects
                    return queryIter;
                }
                return executeQuery(GET_ALL_PQL); 
            } 
        };
    }

    public Func1<Response,String> idGetter() {
        return new Func1<Response,String>() {
            public String apply(Response record) {
                return record.getUuid();
            }        
        };
    }
}