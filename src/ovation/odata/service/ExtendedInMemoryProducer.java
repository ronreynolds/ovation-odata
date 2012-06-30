package ovation.odata.service;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.core4j.Func;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmDecorator;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.internal.EdmDataServicesDecorator;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.exceptions.NotFoundException;
import org.odata4j.producer.inmemory.InMemoryEntityInfo;
import org.odata4j.producer.inmemory.InMemoryProducer;
import org.odata4j.producer.inmemory.InMemoryTypeMapping;

import ovation.odata.model.ExtendedPropertyModel;
import ovation.odata.util.CollectionUtils;
import ovation.odata.util.OData4JServerUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * adapts OData4J's framework to Ovation's model 
 * @author Ron
 */
public class ExtendedInMemoryProducer extends InMemoryProducer {
    public static final Logger _log = Logger.getLogger(ExtendedInMemoryProducer.class);
    private final Map<String, InMemoryEntityInfo<?>> _eis;
    private final Field _hasStream;
    private final Field _properties;

    public ExtendedInMemoryProducer(String namespace, int maxResults) {
        this(namespace, null, maxResults, null, null); 
    }
    /** this ctor seems to have some interesting pluggable parts - TODO need to look into those parts a bit more */
    @SuppressWarnings("unchecked")
    public ExtendedInMemoryProducer(String namespace, String containerName, int maxResults, EdmDecorator decorator, InMemoryTypeMapping typeMapping) {
        super(namespace, containerName, maxResults, decorator, typeMapping);

        // i need access to this to fix how streaming is handled (since it requires special-features of the underlying model object and i need to be at the handler level)
        try {
            Field eis = InMemoryProducer.class.getDeclaredField("eis");
            eis.setAccessible(true);    // remove that private keyword
            _eis = (Map<String, InMemoryEntityInfo<?>>)eis.get(this);
        } catch (Exception e) {
            throw new IllegalStateException("unable to get eis field of InMemoryProducer - odata4j may have changed - " + e, e);
        }
        
        try {
            _hasStream = InMemoryEntityInfo.class.getDeclaredField("hasStream");
            _hasStream.setAccessible(true);
            _properties = InMemoryEntityInfo.class.getDeclaredField("properties");
            _properties.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("unable to get hasStream field of InMemoryEntityInfo - odata4j may have changed - " + e, e);
        }
    }

    /**
     * @param entityClass       the Java class type of the entity being registered
     * @param propertyModel     the model instance that handles the registered type
     * @param entitySetName     the name added to the URL to identify a request for this entity type
     * @param get               Func object which returns Iterable<TEntity> to get all entities of this type
     */
    public <TEntity> void register( final Class<TEntity> entityClass, 
                                    final ExtendedPropertyModel<?> propertyModel, 
                                    final String entitySetName,
                                    final Func<Iterable<TEntity>> get) {
        final String entityTypeName = entitySetName;
        final String[] keys = propertyModel.getKeyPropertyNames();
        
        if (_log.isDebugEnabled()) {
            _log.debug("register(class:" + entityClass + ", model:" + propertyModel 
                            + ", name:" + entitySetName + ", type:" + entityTypeName 
                            + ", getAll:" + get + ", keys:" + Arrays.toString(keys));
        }
        super.register(entityClass, propertyModel, entitySetName, entityTypeName, get, keys);
        // fix the _eis entry
        InMemoryEntityInfo<?> info = _eis.get(entitySetName);
        // and we have to by-pass the fact that this field is package-private
        try {
            _hasStream.set(info, propertyModel.hasStream());
        } catch (Exception ex) {
            _log.error("failed to access hasStream within InMemoryEntityInfo - odata4j may have changed - " + ex, ex);
        }
    }

    Map<String,Map.Entry<EdmFunctionImport,ServiceOperationHandler>> _functionDescriptors = Maps.newHashMap();
    
    public void register(final EdmFunctionImport functionDesc, final ServiceOperationHandler handler) {
        _functionDescriptors.put(functionDesc.getName(), 
                new AbstractMap.SimpleImmutableEntry<EdmFunctionImport,ServiceOperationHandler>(functionDesc, handler));
    }

    /**
     * overwrite getMetadata so we can alter it without having to go through the mystery path to 
     * add EdmFunctionImport objects
     */
    public EdmDataServices getMetadata() {
        try {
            final EdmDataServices metadata = super.getMetadata();
            
            // here we decorate the metadata so we can properly get things routed to registered service-operation handlers
            EdmDataServices result = new EdmDataServicesDecorator() {
                protected EdmDataServices getDelegate() { return metadata; }    // most is passed thru to the super-class metadata
                public EdmFunctionImport findEdmFunctionImport(String functionImportName) {
                    Map.Entry<EdmFunctionImport,ServiceOperationHandler> descriptor = _functionDescriptors.get(functionImportName);
                    return descriptor != null ? descriptor.getKey() : null;
                }
            };
            return result;
        } catch (Throwable ex) {
            _log.error("failed to getMetadata()", ex);
            throw new RuntimeException(ex.toString(), ex);
        }
    }

    /**
     * Creates a new OData entity.
     * 
     * @param entitySetName  the entity-set name
     * @param entity  the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingnewEntries">[odata.org] Creating new Entries</a>
     */
    @Override
    public EntityResponse createEntity(String entitySetName, OEntity entity) {
        return super.createEntity(entitySetName, entity);
    }
    
    /**
     * Creates a new OData entity as a reference of an existing entity, implicitly linked to the existing entity by a navigation property.
     * 
     * @param entitySetName  the entity-set name of the existing entity
     * @param entityKey  the entity-key of the existing entity
     * @param navProp  the navigation property off of the existing entity
     * @param entity  the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties, and linked to the existing entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingnewEntries">[odata.org] Creating new Entries</a>
     */
    @Override
    public EntityResponse createEntity(String entitySetName, OEntityKey entityKey, String navProp, OEntity entity) {
        return super.createEntity(entitySetName, entityKey, navProp, entity);
    }
    
    /** 
     * Gets all the entities for a given top-level set matching the query information.
     * 
     * @param entitySetName  the entity-set name for entities to return
     * @param queryInfo  the additional constraints to apply to the entities
     * @return a packaged collection of entities to pass back to the client
     */
    @Override
    public EntitiesResponse getEntities(String entitySetName, QueryInfo queryInfo) {
        if (_log.isDebugEnabled()) {
            _log.debug("getEntities(set:" + entitySetName + ", queryInfo:{" + OData4JServerUtils.toString(queryInfo) + "}");
        }
        ExtendedPropertyModel.setQueryInfo(queryInfo);
        try {
            return super.getEntities(entitySetName, queryInfo);
        } finally {
            // remove from thread-local
            ExtendedPropertyModel.setQueryInfo(null);
        }
    }
    
    /** 
     * Obtains a single entity based on its type and key.
     * 
     * @param entitySetName  the entity-set name for the entity to return
     * @param entityKey  the unique entity-key within the set
     * @return the matching entity
     */
    @Override
    public EntityResponse getEntity(String entitySetName, OEntityKey entityKey, EntityQueryInfo queryInfo) { 
        if (_log.isDebugEnabled()) {
            _log.debug("getEntity(set:" + entitySetName + ", key:" + entityKey + ", queryInfo:{" + OData4JServerUtils.toString(queryInfo) + "}");
        }
        
        ExtendedPropertyModel.setQueryInfo(queryInfo);
        try {
            return super.getEntity(entitySetName, entityKey, queryInfo);
        } finally {
            // remove from thread-local
            ExtendedPropertyModel.setQueryInfo(null);
        }
    }
    
    /**
     * Modifies an existing entity using merge semantics.
     * 
     * @param entitySetName  the entity-set name
     * @param entity  the entity modifications sent from the client
     * @see <a href="http://www.odata.org/developers/protocols/operations#UpdatingEntries">[odata.org] Updating Entries</a>
     */
    @Override
    public void mergeEntity(String entitySetName, OEntity entity) {
        super.mergeEntity(entitySetName, entity);
    }

    /**
     * Modifies an existing entity using update semantics.
     * 
     * @param entitySetName  the entity-set name
     * @param entity  the entity modifications sent from the client
     * @see <a href="http://www.odata.org/developers/protocols/operations#UpdatingEntries">[odata.org] Updating Entries</a>
     */
    @Override
    public void updateEntity(String entitySetName, OEntity entity) {
        super.updateEntity(entitySetName, entity);
    }
    
    /**
     * Deletes an existing entity.
     * 
     * @param entitySetName  the entity-set name of the entity
     * @param entityKey  the entity-key of the entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#DeletingEntries">[odata.org] Deleting Entries</a>
     */
    @Override
    public void deleteEntity(String entitySetName, OEntityKey entityKey) {
        super.deleteEntity(entitySetName, entityKey);
    }
    
    /** 
     * Given a specific entity, follow one of its navigation properties, applying constraints as appropriate.
     * Return the resulting entity, entities, or property value.
     * 
     * @param entitySetName  the entity-set of the entity to start with
     * @param entityKey  the unique entity-key of the entity to start with
     * @param navProp  the navigation property to follow
     * @param queryInfo  additional constraints to apply to the result
     * @return the resulting entity, entities, or property value
     */
    @Override
    public EntitiesResponse getNavProperty(String entitySetName, OEntityKey entityKey, String navProp, QueryInfo queryInfo) {
        ExtendedPropertyModel.setQueryInfo(queryInfo);
        try {
            final List<OEntity> entities = Lists.newArrayList();
            
            // work-around for a OData4J bug whereby URLs like this: http://win7-32:8080/ovodata/Ovodata.svc/&query=Sources('674dd7f3-6a1f-4f5a-a88f-86711b725921')/EpochGroups
            // product calls like this: getNavProperty(set:&query=Sources, key:('674dd7f3-6a1f-4f5a-a88f-86711b725921'), nav:EpochGroups, 
            // query:{{inlineCnt:null, top:null, skip:null, filter:null, orderBy:null, skipToken:null, customOptions:{}, expand:[], select:[]}}), model:null
            if (entitySetName != null && entitySetName.startsWith("&query=")) {
                entitySetName = entitySetName.substring("&query=".length());
            }
            
            // find the property-model associated with this entity set name
            ExtendedPropertyModel<?> model = ExtendedPropertyModel.getPropertyModel(entitySetName);
            
            if (_log.isInfoEnabled()) {
                _log.info("getNavProperty(set:" + entitySetName + ", key:" + entityKey + ", nav:" + navProp 
                        + ", query:{" + OData4JServerUtils.toString(queryInfo) + "}), model:" + model);
            }
            
            if (model == null) {
                _log.warn("Unable to find model for entitySetName '" + entitySetName + "'");
                throw new NotFoundException(entitySetName + " type is not found");
            }
        
            // find root entity 
            Object entity = model.getEntityByKey(entityKey);
            if (entity == null) {
                if (_log.isInfoEnabled()) {
                    _log.info("Unable to find entity in " + model + " with key " + entityKey);
                }
                throw new NotFoundException(entitySetName + "(" + entityKey + ") was not found");
            }
            
            // navProp is the NAME of the entity within the element in entitySetName - need to resolve it to a TYPE
            // not ALWAYS a collection, tho, so we also have to check properties (tho it can't be both)
            Class<?> navPropType = model.getCollectionElementType(navProp);
            boolean isCollection = true;
            if (navPropType == null) {
                navPropType = model.getPropertyType(navProp);
                isCollection = false;
                if (navPropType == null) {
                    _log.warn("Unrecognized collection/property '" + navProp + "' within '" + entitySetName + "'");
                    throw new NotFoundException(navProp + " collection not found in '" + entitySetName + "'");
                }
            }
            ExtendedPropertyModel<?> subModel = ExtendedPropertyModel.getPropertyModel(navPropType);
            if (subModel == null) {
                _log.warn("Unrecognized type '" + navPropType + "' of '" + navProp + "' within '" + entitySetName + "'");
                throw new NotFoundException(navProp + " collection type '" + navPropType + "' is not known");
            }

            final EdmEntitySet subEntitySet = getMetadata().getEdmEntitySet(subModel.entityName());
            // iterate over each sub-entity of entity which matches the navProp - they will all be of the same type
            Iterable<?> iterable = isCollection ? model.getCollectionValue(entity, navProp) : CollectionUtils.makeIterable(model.getPropertyValue(entity, navProp));
            Iterator<?> iter = iterable != null ? iterable.iterator() : null;
            if (iter != null) {
                if (queryInfo.skip != null) {
                    for (int numToSkip = queryInfo.skip.intValue(); numToSkip > 0 && iter.hasNext(); --numToSkip) {
                        iter.next();    // skip
                    }
                }
                
                // TODO - this should influence how data is returned
//TODO              List<EntitySimpleProperty>  expand = queryInfo.expand; - whether or not to expand out sub-elements or leave them as references
//              BoolCommonExpression        filter = queryInfo.filter; - should be used by model
//              List<OrderByExpression>     orderBy = queryInfo.orderBy; - should be used by model?

                for (int numToReturn = queryInfo.top != null ? queryInfo.top.intValue() : Integer.MAX_VALUE; numToReturn > 0 && iter.hasNext(); --numToReturn) {
                    Object o = iter.next();
        /*          
                    List<OProperty<?>> properties = Lists.newArrayList();
                    for (String propName : subModel.getPropertyNames()) {
                        Class<?> propType = subModel.getPropertyType(propName);
                        EdmSimpleType edmType = EdmSimpleType.forJavaType(propType);
                        String propValue = String.valueOf(subModel.getPropertyValue(o, propName));
                        // FIXME - seems weird to dumb this down to a string...
                        properties.add(OProperties.parse(propName, edmType.getFullyQualifiedTypeName(), propValue));
                    }
                    
                    List<OLink> links = Lists.newArrayList();
                    for (String linkName : subModel.getCollectionNames()) {
    //                  Class<?> linkType = subModel.getCollectionElementType(linkName);
    //                  Iterable<?> linkValue = subModel.getCollectionValue(o, linkName);
                        String relation = "unknown";    // FIXME - need values for relation
                        String title = linkName;
                        String href = "/" + linkName;   // FIXME absolute or relative to current URL?
                        links.add(OLinks.relatedEntities(relation, title, href));
        //FIXME             OLinks.relatedEntitiesInline(relation, title, href, relatedEntities);   // controlled via queryInfo $inline/$expand
        //FIXME - how to select this one?               OLinks.relatedEntity(relation, title, href);
        //FIXME             OLinks.relatedEntityInline(relation, title, href, relatedEntity);       // controlled via queryInfo $inline/$expand
                    }
        */
                    if (o != null) {
                        entities.add(toOEntity(subEntitySet, o, queryInfo.expand));
                    }
                }
    
            } else {
                // FIXME no elments found to iterate the navProp is invalid?
                _log.info("no elments found to iterate the navProp is invalid?");
            }
            
            return Responses.entities(entities, subEntitySet, Integer.valueOf(entities.size()), queryInfo.skipToken);
        } finally {
            // make sure to detach the QueryInfo from the thread when we're done
            ExtendedPropertyModel.setQueryInfo(null);
        }
    }
    
    /** @since 1.2 */
    @Override
    protected OEntity toOEntity(EdmEntitySet ees, Object obj, List<EntitySimpleProperty> expand) {
        // let the base class do the hard work
        OEntity oEntity = super.toOEntity(ees, obj, expand);
        // determine if we need to a stream handler to this guy
        InMemoryEntityInfo<?> info = _eis.get(ees.getName());
        
        try {
            @SuppressWarnings("unchecked")
            ExtendedPropertyModel<Object> model = (ExtendedPropertyModel<Object>)_properties.get(info);
            if (model.hasStream()) {
                oEntity = OEntities.create(oEntity.getEntitySet(), oEntity.getEntityKey(), oEntity.getProperties(), oEntity.getLinks(), obj, model.getStreamHandler(obj));
            }
        } catch (Exception ex) {
            _log.error("failed to get properties via reflection in InMemoryEntityInfo - " + ex, ex);
        }

        return oEntity;
    }

    /**
     * Call a function (aka Service Operation)
     *
     * @param funcDesc  the name and other metadata about the function
     * @param params  the parameters to the function
     * @param queryInfo  additional query parameters to apply to collection-valued results
     * @return a BaseResponse appropriately typed to hold the function results
     *    From the spec:<pre>
     *    The return type of &lt;Function&gt; MUST be one of the following:
     *        An EDMSimpleType or collection of EDMSimpleTypes.
     *        An entity type or collection of entity types.
     *        A complex type or collection of complex types.
     *        A row type or collection of row types.
     *        &lt;ReturnType&gt; can contain a maximum of one &lt;CollectionType&gt; element.
     *        &lt;ReturnType&gt; can contain a maximum of one &lt;ReferenceType&gt; element.
     *        &lt;ReturnType&gt; can contain a maximum of one &lt;RowType&gt; element.
     *        A ref type or collection of ref types.</pre>
     */ 
    @Override
    public BaseResponse callFunction(EdmFunctionImport funcDesc, Map<String, OFunctionParameter> params, QueryInfo queryInfo) {
        _log.error("callFunction(" + funcDesc + ", " + params + ", " + queryInfo);
        Map.Entry<EdmFunctionImport,ServiceOperationHandler> descriptor = _functionDescriptors.get(funcDesc.getName());
        if (descriptor != null) {
            return descriptor.getValue().execute(funcDesc, params, queryInfo);
        }
        return super.callFunction(funcDesc, params, queryInfo);
    }

    public static interface ServiceOperationHandler {
        BaseResponse execute(EdmFunctionImport funcDesc, Map<String, OFunctionParameter> params, QueryInfo queryInfo);
    }
}