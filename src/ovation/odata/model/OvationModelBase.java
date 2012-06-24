package ovation.odata.model;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.joda.time.DateTime;
import org.odata4j.core.OEntityKey;
import org.odata4j.producer.QueryInfo;

import ovation.AnalysisRecord;
import ovation.CoordinateSystem;
import ovation.DerivedResponse;
import ovation.Epoch;
import ovation.EpochGroup;
import ovation.Experiment;
import ovation.ExternalDevice;
import ovation.Group;
import ovation.IAnnotatableEntityBase;
import ovation.IAnnotation;
import ovation.IEntityBase;
import ovation.IIOBase;
import ovation.INoteAnnotation;
import ovation.IResponseData;
import ovation.IShape;
import ovation.ITaggableEntityBase;
import ovation.ITimelineElement;
import ovation.ImageAnnotatable;
import ovation.ImageAnnotation;
import ovation.IndexedURLResponse;
import ovation.KeywordTag;
import ovation.NumericData;
import ovation.NumericDataFormat;
import ovation.Project;
import ovation.Resource;
import ovation.Response;
import ovation.Source;
import ovation.Stimulus;
import ovation.TimelineAnnotation;
import ovation.URLResource;
import ovation.URLResponse;
import ovation.User;
import ovation.odata.model.dao.PrimitiveCollectionModel;
import ovation.odata.model.dao.Property;
import ovation.odata.util.CollectionUtils;
import ovation.odata.util.DataContextCache;


public abstract class OvationModelBase<V extends IEntityBase> extends ExtendedPropertyModel<V> {
    public static final String GET_ALL_PQL = "true";	// apparently this PQL "query" returns all instances of a type

    protected OvationModelBase(Map<String,Class<?>> fieldTypes, Map<String,Class<?>> collectionTypes) {
    	super(fieldTypes, collectionTypes, PropertyName.UUID.name());
    }
    
	// rules of thumb - 
	// collections (association) can ONLY refer to other entity types 
	// properties (aggregation) can refer to primitive types and entity types
    
    /** register model handlers for all Ovation API model classes */
    public static void registerOvationModel() {
    	// top-level types
        ExtendedPropertyModel.addPropertyModel(new AnalysisRecordModel());
        ExtendedPropertyModel.addPropertyModel(new DerivedResponseModel());
        ExtendedPropertyModel.addPropertyModel(new EpochGroupModel());
        ExtendedPropertyModel.addPropertyModel(new EpochModel());
        ExtendedPropertyModel.addPropertyModel(new ExperimentModel());
        ExtendedPropertyModel.addPropertyModel(new ExternalDeviceModel());
        ExtendedPropertyModel.addPropertyModel(new KeywordTagModel());
        ExtendedPropertyModel.addPropertyModel(new ProjectModel());
        ExtendedPropertyModel.addPropertyModel(new ResourceModel());
        ExtendedPropertyModel.addPropertyModel(new ResponseModel());
        ExtendedPropertyModel.addPropertyModel(new SourceModel());
        ExtendedPropertyModel.addPropertyModel(new StimulusModel());
        ExtendedPropertyModel.addPropertyModel(new URLResourceModel());
        ExtendedPropertyModel.addPropertyModel(new UserModel());		
        ExtendedPropertyModel.addPropertyModel(new CoordinateSystemModel());
        ExtendedPropertyModel.addPropertyModel(new ShapeModel());
        ExtendedPropertyModel.addPropertyModel(new GroupModel());
        
        // supporting types
        ExtendedPropertyModel.addPropertyModel(new StringModel());		// so we can return a collection of strings (they may have fixed this in odata4j 0.6)
//TODO        ExtendedPropertyModel.addPropertyModel(new PrimitiveCollectionModel<String>(String.class));
        ExtendedPropertyModel.addPropertyModel(new PrimitiveCollectionModel<Double>(Double.class));
        ExtendedPropertyModel.addPropertyModel(new PrimitiveCollectionModel<Float>(Float.class));
        ExtendedPropertyModel.addPropertyModel(new PrimitiveCollectionModel<Integer>(Integer.class));
        ExtendedPropertyModel.addPropertyModel(new PrimitiveCollectionModel<Long>(Long.class));

        ExtendedPropertyModel.addPropertyModel(new Property.Model());	// so we can return string-string pairs
        
        ExtendedPropertyModel.addPropertyModel(new ITaggableEntityBaseModel());	// dunno about this idea but we have a collection of base-types
        ExtendedPropertyModel.addPropertyModel(new IAnnotationModel());
    }
    
    // note, these two enums feel like they could be expanded to also have the getters but doing so would take a 
    // fair amount of time (tho might result in a very nice expendable design...)
    
    interface NameEnum {
        public Class<?> getType();
// public Object getProperty(Object o) { return _getter.execute(o); }
        // Func<T> _getter = new Func<T>() { public T execute(Object o) { return o.getThing(); } }; ...  pity it can't be bound to the Class<?> returned by getType()...
    }
    
    /** every property of every child-type - ensures consistent naming and also makes common util functions doable */
    protected enum PropertyName implements NameEnum {
        URI(String.class), UUID(String.class), IsIncomplete(Boolean.class),Owner(User.class),               // EntityBase
        Tag(String.class),	 																				// KeywordTag 
        																									// TaggableEntityBase 
        																									// AnnotatableEntityBase 
        EntryFunctionName(String.class), Name(String.class), Notes(String.class), Project(Project.class), 
        ScmRevision(String.class), ScmURL(String.class), 													// AnalysisRecord
        Experiment(Experiment.class), Manufacturer(String.class),  											// ExternalDevice + Name(String.class), SerializedLocation(String.class), 		
        Label(String.class), ParentSource(Source.class), ParentRoot(Source.class),  						// Source + SerializedLocation(String.class), 				
        Data(byte[].class), UTI(String.class), 																// Resource + Name(String.class), Notes(String.class),  			
        URL(String.class), 																					// URLResource
        ExternalDevice(ExternalDevice.class), Units(String.class), 											// IOBase
        Epoch(Epoch.class), PluginID(String.class),  														// Stimulus + SerializedLocation(String.class), 			
        																									// ResponseDataBase 	
        																									// Response + Epoch(ovation.Epoch.class), SerializedLocation(String.class), UTI(String.class), 										 			
        Description(String.class), 																			// DerivedResponse + Epoch(Epoch.class), Name(String.class), SerializedLocation(String.class), 			 		
        EndTime(DateTime.class), StartTime(DateTime.class),                                                 // TimelineElement      

        EpochCount(Integer.class), ParentEpochGroup(EpochGroup.class), Source(Source.class),				// EpochGroup + Experiment(ovation.Experiment.class), Label(String.class), SerializedLocation(String.class),   			
        Duration(Double.class), EpochGroup(EpochGroup.class), ExcludeFromAnalysis(Boolean.class), 
        NextEpoch(Epoch.class), PreviousEpoch(Epoch.class), ProtocolID(String.class),						// Epoch + SerializedLocation(String.class)
        Purpose(String.class),																				// PurposeAndNotesEntity + Notes(String.class),
        																									// Experiment + SerializedLocation(String.class)
         																									// Project + Name(String.class), SerializedLocation(String.class)
        Username(String.class),																				// User
        Text(String.class),																					// IAnnotation
    	ByteOrder(String.class), NumericDataFormat(String.class), NumericByteOrder(String.class), SampleBytes(Short.class), // NumericDataType
        CoordinateSystem(CoordinateSystem.class),                                                           // IShape
        DataBytes(byte[].class),                                                                            // URLResponse
        End(Long.class), Start(Long.class),                                                                 // IndexedURLResponse
        Shape(IShape.class),                                                                                // ImageAnnotation
        GroupName(String.class),                                                                            // Group
    	;
        
    	final Class<?> 	_type;
    	PropertyName(Class<?> type)	{ _type = type; }

        public Class<?> getType()   { return _type; }
    };
    
    /** every collection (association) of every child-type - ensures consistent naming */
    protected enum CollectionName implements NameEnum {
        MyProperties(Property.class), Properties(Property.class), ResourceNames(String.class), Resources(Resource.class),		// EntityBase
        Tagged(ITaggableEntityBase.class),																											// KeywordTag 			
        KeywordTags(KeywordTag.class), MyKeywordTags(KeywordTag.class), MyTags(String.class), Tags(String.class),									// TaggableEntityBase 
        AnnotationGroupTags(String.class), Annotations(IAnnotation.class), MyAnnotationGroupTags(String.class), MyAnnotations(IAnnotation.class),	// AnnotatableEntityBase
        AnalysisParameters(Property.class), Epochs(Epoch.class), 																					// AnalysisRecord
        																																			// ExternalDevice
        AllEpochGroups(EpochGroup.class), AllExperiments(Experiment.class), ChildLeafSourceDescendants(Source.class), SourceChildren(Source.class), 
        EpochGroups(EpochGroup.class), Experiments(Experiment.class), 																				// Source
        																																			// Resource
        																																			// URLResource
        DeviceParameters(Property.class), DimensionLabels(String.class),																			// IOBase
        StimulusParameters(Property.class), 																										// Stimulus
        DoubleData(Double.class), FloatData(Float.class), FloatingPointData(Double.class), IntData(Integer.class), IntegerData(Integer.class), 
        ShortIntData(Short.class), UnsignedIntData(Long.class),																						// NumericDataType																
        MatlabShape(Long.class), Shape(Long.class),																									// ResponseDataBase
        SamplingRates(Double.class), SamplingUnits(String.class), 																					// Response
        DerivationParameters(Property.class), 																										// DerivedResponse
        																																			// TimelineElement
        ChildLeafGroupDescendants(EpochGroup.class), GroupChildren(EpochGroup.class), EpochsUnsorted(Epoch.class), 									// EpochGroup + Epochs(Epoch.class), 
        AnalysisRecords(AnalysisRecord.class), DerivedResponses(DerivedResponse.class), DerivedResponseNames(String.class), 
        MyDerivedResponseNames(String.class), MyDerivedResponses(DerivedResponse.class), ProtocolParameters(Property.class), 
        Responses(Response.class), ResponseNames(String.class), StimuliNames(String.class), Stimuli(Stimulus.class),								// Epoch
        																																			// PurposeAndNotesEntity
        ExternalDevices(ExternalDevice.class), Projects(Project.class), Sources(Source.class),	                                                    // Experiment + EpochGroups(EpochGroup.class), Epochs(Epoch.class), 
        AnalysisRecordNames(String.class), MyAnalysisRecords(AnalysisRecord.class), MyAnalysisRecordNames(String.class), 							// Project + AnalysisRecords(AnalysisRecord.class), Experiments(Experiment.class),
    	Annotated(IAnnotatableEntityBase.class),																									// IAnnotation
        ReferencePoint(Double.class), ScaleFactors(Double.class), Units(String.class),                                                              // CoordinateSystem
        MyCoordinateSystems(CoordinateSystem.class), CoordinateSystems(CoordinateSystem.class)                                                      // ImageAnnotatable    
    	;
        
    	final Class<?> _type;
    	CollectionName(Class<?> type) { _type = type; }
    	
        public Class<?> getType()		{ return _type; }
    };
    
    /** @return entity that matches key or null if none */
    public V getEntityByKey(OEntityKey key) {
        Iterator<V> itr = getEntityIterByUUID(key);
        if (itr.hasNext() == false) {
            _log.error("Unable to find Project with UUID " + key.toKeyStringWithoutParentheses());
            return null;
        }
        V v = itr.next(); // the object we want.
        if (itr.hasNext()) {
            _log.error("Found multiple " + getEntityType() + " with UUID " + key.toKeyStringWithoutParentheses());
        }
        return v;
    }
    
    @SuppressWarnings("unchecked")
    protected Iterator<V> getEntityIterByUUID(OEntityKey key) {
        String query     = "uuid == " + key.toKeyStringWithoutParentheses();
        if (_log.isDebugEnabled()) { 
        	_log.debug("executing type:'" + getEntityType() + "', query:'" + query + "'");
        }
        return (Iterator<V>)DataContextCache.getThreadContext().query(getEntityType(), query);
    }
    
    

    
    /**
     * model-hierarchy (if this changes this code must be updated)
     *  AnalysisRecord											-> AnnotatableEntityBase -> TaggableEntityBase -> EntityBase -> ooObj -> ooAbstractObj
     *  DerivedResp -> ResponseDataBase      -> IOBase 			-> AnnotatableEntityBase -> ...
     *  EpochGroup 							 -> TimelineElement -> AnnotatableEntityBase -> ...
     *  Epoch 								 -> TimelineElement -> ...
     *  Experiment 	-> PurposeAndNotesEntity -> TimelineElement -> ...
     *  ExternalDevi								   			-> AnnotatableEntityBase -> ...
     *  KeywordTag 																					  		   -> EntityBase -> ...
     *  Project 	-> PurposeAndNotesEntity -> ...
     *  Resource 									   			-> AnnotatableEntityBase -> ...
     *  Response 	-> ResponseDataBase      -> ...
     *  Source 										   			-> AnnotatableEntityBase -> ...
     *  Stimulus 							 -> IOBase          -> ...
     *  URLResource	-> Resource              -> ...
     * 
     * * ooAbstractObj
     * ** ooObj
     * *** EntityBase [IEntityBase(IooObj)] (MyProperties:<String,Object>[], Owner:User, Properties:<String,Object[]>[], ResourceNames:String[], Resources:Resource[], URI:String, UUID:String, IsComplete:bool) (package-private)
     * **** CoordinateSystem (Name:String, ReferencePoint:double[], ScaleFactors:double[], Units:String[]) - v1.2
     * **** KeywordTag (Tag:String, Tagged:TaggableEntityBase[])
     * **** Shape (no props or collections) - v1.2 (package-private)
     * ***** Line (endX:double, endY:double, startX:double, startY:double) - v1.2
     * ***** Oval (height:double, width:double, x:double, y:double) - v1.2
     * ***** Point (coordinates:double[]) - v1.2
     * ***** Polygon (xCoordinates:double[], yCoordinates:double[]) - v1.2
     * **** TaggableEntityBase[ITaggableEntityBase(IooObj, IEntityBase)] (KeywordTags:KeywordTag[], MyKeywordTags:KeywordTag[], MyTags:String[], Tags:String[]) (package-private) 
     * ***** AnnotatableEntityBase[IAnnotatableEntityBase(IooObj, IEntityBase, ITaggableEntityBase)] (AnnotationGroupTags:String[], Annotations:IAnnotation[], MyAnnotationGroupTags:String[], MyAnnotations:IAnnotation[])
     * ****** AnalysisRecord (AnalysisParameters:<String,Object>[], EntryFunctionName:String, Epochs:Epoch[], Name:String, Notes:String, Project:Project, ScmRevision:String, ScmURL:String, SerializedLocation:String)
     * ****** ExternalDevice (Experiment:Experiment, Manufacturer:String, Name:String, SerializedLocation:String)
     * ****** IOBase[IIOBase(IAnnotatableEntityBase, IIOData), IIOData] (DeviceParameters:<String,Object>[], DimensionLabels:String[], ExternalDevice:ExternalDevice, Units:String) (package-private)
     * ******* ResponseDataBase [IResponseData(IIOData), ImageAnnotatable(IAnnotatableEntityBase)] (Data:NumericData, DataBytes:byte[], DoubleData:double[], FloatData:float[], FloatingPointData:double[], IntData:int[], IntegerData:int[], MatlabShape:long[], NumericDataType:NumericDataType, Shape:long[])
     * ******** DerivedResponse (DerivationParameters:<String,Object>[], Description:String, Epoch:Epoch, Name:String, SerializedLocation:String)
     * ******** Response[Comparable<Response>, ImageAnnotatable] (Epoch:Epoch, SamplingRates:double[], SamplingUnits:String[], SerializedLocation:String, UTI:String)
     * ********* URLResponse(data: NumericData, dataBytes:byte[], dataBytesStream:ByteArrayOutputStream, dataStream:InputStream, URL:URL, URLString:string) - v1.2
     * ********** IndexedURLResponse(end:long, start:long) - v1.2
     * ******* Stimulus[Comparable<Stimulus>] (Epoch:Epoch, PluginID:String, SerializedLocation:String, StimulusParameters:<String,Object>[])
     * ****** Resource[ImageAnnotatable] (Data:byte[], Name:String, Notes:String, Uti:String)
     * ******* URLResource (URL:String)
     * ****** SavedQuery (expressionTree:ExpressionTree(not in our model), name:string, predicateString:String, resultType:string, serializedData:byte[], isSynchronizationQuery:boolean) - v1.2
     * ******* SynchronizationQuery (active:boolean) - v1.2 (package-private)
     * ****** Source[Comparable<Source>, SourceContainer(Iterable<Source> getSourcesWithLabel(String label))] (AllEpochGroups:EpochGroup[], AllExperiments:Experiment[], ChildLeafDescendants:Source[], Children:Source[], EpochGroups:EpochGroup[], Experiments:Experiment[], Label:String, Parent:Source, ParentRoot:Source, SerializedLocation:String)
     * ****** TimelineElement[ITimelineElement] (EndTime:DateTime, StartTime:DateTime) (package-private)
     * ******* Epoch (AnalysisRecords:AnalysisRecord[], DerivedResponses:DerivedResponse[], DerivedResponseNames:String[], Duration:double, EpochGroup:EpochGroup, ExcludeFromAnalysis:bool, MyDerivedResponseNames:String[], MyDerivedResponses:DerivedResponse[], NextEpoch:Epoch, PreviousEpoch:Epoch, ProtocolID:String, ProtocolParameters:<String,Object>[], Responses:Response[], ResponseNames:String[], SerializedLocation:String, StimuliNames:String[], Stimuli:Stimulus[])
     * ******* EpochGroup (ChildLeafDescendants:EpochGroup[], Children:EpochGroup[], EpochCount:int, Epochs:Epoch[], EpochsUnsorted:Epoch[], Experiment:Experiement, Label:String, Parent:EpochGroup, SerializedLocation:String, Source:Source)
     * ******* PurposeAndNotesEntity[IOwnerNotes,IScientificPurpose] (Notes:String, Purpose:String) (package-private)
     * ******** Experiment (EpochGroups:EpochGroup[], Epochs:Epoch[], ExternalDevices:ExternalDevice[], Projects:Project[], SerializedLocation:String, Sources:Source[])
     * ******** Project (AnalysisRecords:AnalysisRecord[], AnalysisRecordNames:String[], Experiments:Experiment[], MyAnalysisRecords:AnalysisRecord[], MyAnalysisRecordNames:String[], Name:String, SerializedLocation:String)
     * ***** Annotation[IAnnotation(ITaggableEntityBase)] (annotated:IAnnotatableEntityBase[], text:string)
     * ****** ImageAnnotation (shape:IShape) - v1.2
     * ****** Note[INoteAnnotation(IAnnotation)] (no properties or collections) - v1.2
     * ****** TimelineAnnotation[ITimelineAnnotation(IAnnotation)] (endTime:DateTime, startTime:DateTime) - v1.2
     * ***** Group (groupName:string) - v1.2
     * ***** User (username:string)
     * ImageAnnotatable(DataBytes:byte[], DataStream:InputStream, CoordinateSystems:CoordinateSystem[], MyCoordinateSystems:CoordinateSystem[]) - v1.2
     * 
     * 
     */
    
    /**  
     * the add* utility methods are used during setup to configure the service metadata properly for each type; 
     * they add the properties and collections along with their types for each of the Ovation base-type classes.  
     * 
     * @param propertyTypeMap
     * @param collectionTypeMap
     */
    /** this method and addCollections leverages the type associated with the PropertyName (thus it's only stored in 1 place and is guaranteed consistent system-wide*/
    protected static void addProperties(Map<String,Class<?>> propertyTypeMap, PropertyName... props) {
    	for (PropertyName prop : props) {
    		propertyTypeMap.put(prop.name(), prop._type);
    	}
    }
    protected static void addCollections(Map<String,Class<?>> collectionTypeMap, CollectionName... cols) {
    	for (CollectionName col : cols) {
    		collectionTypeMap.put(col.name(), col._type);
    	}
    }

    
    /** these type-specific methods add the properties and collections for each type 
     * here and in the specific object-type models are the code that needs to change when the underlying
     * Ovation model changes
     */
    protected static void addEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.URI, PropertyName.UUID, PropertyName.IsIncomplete, PropertyName.Owner); 
        addCollections(collectionTypeMap, CollectionName.MyProperties, CollectionName.Properties, CollectionName.ResourceNames, CollectionName.Resources);    
        // no parent type within Ovation
    }    
    protected static void addKeywordTag(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Tag); 
        addCollections(collectionTypeMap, CollectionName.Tagged);   
        addEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addTaggableEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addCollections(collectionTypeMap, CollectionName.KeywordTags, CollectionName.MyKeywordTags, CollectionName.MyTags, CollectionName.Tags);   
        addEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addAnnotatableEntityBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addCollections(collectionTypeMap, CollectionName.AnnotationGroupTags, CollectionName.Annotations, CollectionName.MyAnnotationGroupTags, CollectionName.MyAnnotations);   
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addAnalysisRecord(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EntryFunctionName, PropertyName.Name, PropertyName.Notes, PropertyName.Project, PropertyName.ScmRevision, PropertyName.ScmURL); 
        addCollections(collectionTypeMap, CollectionName.AnalysisParameters, CollectionName.Epochs);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addExternalDevice(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Experiment, PropertyName.Manufacturer, PropertyName.Name);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addSource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Label, PropertyName.ParentSource, PropertyName.ParentRoot); 
        addCollections(collectionTypeMap, CollectionName.AllEpochGroups, CollectionName.AllExperiments, CollectionName.ChildLeafSourceDescendants, CollectionName.SourceChildren, CollectionName.EpochGroups, CollectionName.Experiments);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addImageAnnotatable(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addProperties (propertyTypeMap,   PropertyName.DataBytes);
        addCollections(collectionTypeMap, CollectionName.CoordinateSystems, CollectionName.MyCoordinateSystems);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Data, PropertyName.Name, PropertyName.Notes, PropertyName.UTI); 
        addImageAnnotatable(propertyTypeMap, collectionTypeMap);    // implemented by ResponseDataBase, Response (which extends ResponseDataBase), and Resource
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addURLResource(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.URL); 
        addResource(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addIOBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.ExternalDevice, PropertyName.Units); 
        addCollections(collectionTypeMap, CollectionName.DeviceParameters, CollectionName.DimensionLabels);
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addStimulus(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Epoch, PropertyName.PluginID); 
        addCollections(collectionTypeMap, CollectionName.StimulusParameters);
        addIOBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResponseDataBase(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.ByteOrder, PropertyName.NumericDataFormat, PropertyName.NumericByteOrder, PropertyName.SampleBytes, PropertyName.Data); 
        addCollections(collectionTypeMap, CollectionName.MatlabShape, CollectionName.Shape, CollectionName.FloatingPointData, CollectionName.IntegerData, CollectionName.UnsignedIntData);
        addImageAnnotatable(propertyTypeMap, collectionTypeMap);    // implemented by ResponseDataBase, Response (which extends ResponseDataBase), and Resource
        addIOBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addResponse(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Epoch, PropertyName.UTI);  
        addCollections(collectionTypeMap, CollectionName.SamplingRates, CollectionName.SamplingUnits);
        addResponseDataBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addDerivedResponse(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Description, PropertyName.Epoch, PropertyName.Name); 
        addCollections(collectionTypeMap, CollectionName.DerivationParameters);
        addResponseDataBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addTimelineElement(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EndTime, PropertyName.StartTime); 
        addAnnotatableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addEpochGroup(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.EpochCount, PropertyName.Experiment, PropertyName.Label, PropertyName.ParentEpochGroup, PropertyName.Source); 
        addCollections(collectionTypeMap, CollectionName.ChildLeafGroupDescendants, CollectionName.GroupChildren, CollectionName.Epochs, CollectionName.EpochsUnsorted);   
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addEpoch(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Duration, PropertyName.EpochGroup, PropertyName.ExcludeFromAnalysis, PropertyName.NextEpoch, PropertyName.PreviousEpoch, PropertyName.ProtocolID); 
        addCollections(collectionTypeMap, CollectionName.AnalysisRecords, CollectionName.DerivedResponses, CollectionName.DerivedResponseNames, CollectionName.ProtocolParameters, CollectionName.Responses, CollectionName.ResponseNames, CollectionName.StimuliNames, CollectionName.Stimuli);   
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addPurposeAndNotesEntity(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        // addProperties(IOwnerNotes { String getNotes(); TODO
        // addProperties(IScientificPurpose {String getPurpose(); TODO
    	addProperties (propertyTypeMap,   PropertyName.Notes, PropertyName.Purpose); 
        addTimelineElement(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addExperiment(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addCollections(collectionTypeMap, CollectionName.EpochGroups, CollectionName.Epochs, CollectionName.ExternalDevices, CollectionName.Projects, CollectionName.Sources);   
        addPurposeAndNotesEntity(propertyTypeMap, collectionTypeMap);
    }    
    protected static void addProject(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Name); 
        addCollections(collectionTypeMap, CollectionName.AnalysisRecords, CollectionName.AnalysisRecordNames, CollectionName.Experiments, CollectionName.MyAnalysisRecords, CollectionName.MyAnalysisRecordNames);   
        addPurposeAndNotesEntity(propertyTypeMap, collectionTypeMap);
    }    
    // User extends TaggableEntityBase
    protected static void addUser(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Username);
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }    
    // IAnnotation extends ITaggableEntityBase
    protected static void addIAnnotation(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
    	addProperties (propertyTypeMap,   PropertyName.Text);
    	addCollections(collectionTypeMap, CollectionName.Annotated);
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }
    // CoordinateSystem extends EntityBase
    protected static void addCoordinateSystem(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addProperties(propertyTypeMap, PropertyName.Name);
        addCollections(collectionTypeMap, CollectionName.ReferencePoint, CollectionName.ScaleFactors, CollectionName.Units);
        addEntityBase(propertyTypeMap, collectionTypeMap);
    }
    // IShape extends nothing
    protected static void addIShape(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addProperties(propertyTypeMap, PropertyName.Shape);
    }
    // Group extends ITaggableEntityBase
    protected static void addGroup(Map<String,Class<?>> propertyTypeMap, Map<String,Class<?>> collectionTypeMap) {
        addProperties(propertyTypeMap, PropertyName.GroupName);
        addTaggableEntityBase(propertyTypeMap, collectionTypeMap);
    }

    
    /**
     * the get* utility methods are used at request-processing time to reduce duplicate code 
     * across the various models (since they share so much in common in their base types).
     * not all base types are exposed as public classes, tho, so some of this code is still
     * in the child models even tho it's common to many of them.
     * 
     * @param obj
     * @param col
     * @return
     */
    // IEntityBase extends IooObj
    protected static Object getProperty(IEntityBase obj, PropertyName prop) {
    	switch (prop) {
    		case URI:			return obj.getURIString();	// always return String version - URI version just confuses odata4j
    		case UUID:			return obj.getUuid();
    		case IsIncomplete:	return obj.isIncomplete();
            case Owner:         return obj.getOwner();
//    		case SerializedName:return obj.getSerializedName();
    		default: 			_log.error("Unknown property '" + prop + "' for type '" + obj + "'"); return null;	// nowhere to go from here
    	}
    }
    protected static Iterable<?> getCollection(IEntityBase obj, CollectionName col) {
    	switch (col) {
			case MyProperties:	return Property.makeIterable(obj.getMyProperties());
			case Properties:	return Property.makeIterable(obj.getProperties());
			case ResourceNames:	return CollectionUtils.makeIterable(obj.getResourceNames());
			case Resources:		return obj.getResourcesIterable();  
			default:            _log.error("Unknown collection '" + col + "' for type '" + obj + "'"); return null;
		}
    }
    
    // KeywordTag extends EntityBase
    protected static Object getProperty(KeywordTag obj, PropertyName prop) {
    	switch (prop) {
    		case Tag: 	return obj.getTag();
    		default: 	return getProperty((IEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(KeywordTag obj, CollectionName col) {
    	switch (col) {
			case Tagged: 	return CollectionUtils.makeIterable(obj.getTagged());
			default: 		return getCollection((IEntityBase)obj, col);
		}
    }

    // TaggableEntityBase extends EntityBase implements ITaggableEntityBase
    protected static Object getProperty(ITaggableEntityBase obj, PropertyName prop) {
    	switch (prop) {
    		default: return getProperty((IEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(ITaggableEntityBase obj, CollectionName col) {
    	switch (col) {
			case KeywordTags: 	return CollectionUtils.makeIterable(obj.getKeywordTags());
			case MyKeywordTags: return CollectionUtils.makeIterable(obj.getMyKeywordTags());
			case MyTags: 		return CollectionUtils.makeIterable(obj.getMyTags());
			case Tags: 			return CollectionUtils.makeIterable(obj.getTags());
			default: 			return getCollection((IEntityBase)obj, col);
		}
    }

    // AnnotatableEntityBase extends TaggableEntityBase implements IAnnotatableEntityBase
    protected static Object getProperty(IAnnotatableEntityBase obj, PropertyName prop) {
        // all classes that implement ImageAnnotatable also extend IAnnotatableEntityBase
        // this seems better than adding support for it in 3 different places (Response, DerivedResponse, and Resource)
        if (obj instanceof ImageAnnotatable) {
            switch (prop) {
                case DataBytes : return ((ImageAnnotatable)obj).getDataBytes();
            }
        }        
    	switch (prop) {
    		default: return getProperty((ITaggableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(IAnnotatableEntityBase obj, CollectionName col) {
        // all classes that implement ImageAnnotatable also extend IAnnotatableEntityBase
        // this seems better than adding support for it in 3 different places (Response, DerivedResponse, and Resource)
        if (obj instanceof ImageAnnotatable) {
            switch (col) {
                case CoordinateSystems  : return CollectionUtils.makeIterable(((ImageAnnotatable)obj).getCoordinateSystems()); 
                case MyCoordinateSystems: return CollectionUtils.makeIterable(((ImageAnnotatable)obj).getMyCoordinateSystems());
            }
        }

    	switch (col) {
			case AnnotationGroupTags: 	return CollectionUtils.makeIterable(obj.getAnnotationGroupTags());
			case Annotations:			return obj.getAnnotationsIterable();
			case MyAnnotationGroupTags: return CollectionUtils.makeIterable(obj.getMyAnnotationGroupTags());
			case MyAnnotations:			return obj.getMyAnnotationsIterable();
			default: 					return getCollection((ITaggableEntityBase)obj, col);
		}
    }
    
    // AnalysisRecord extends AnnotatableEntityBase
    protected static Object getProperty(AnalysisRecord obj, PropertyName prop) {
    	switch (prop) {
    		case EntryFunctionName:	return obj.getEntryFunctionName();
    		case Name:				return obj.getName();
    		case Notes:				return obj.getNotes();
    		case Project:			return obj.getProject();
    		case ScmRevision:		return obj.getScmRevision();
    		case ScmURL:			return convertURLToString(obj.getScmURL());
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(AnalysisRecord obj, CollectionName col) {
    	switch (col) {
			case AnalysisParameters:return Property.makeIterable(obj.getAnalysisParameters());
			case Epochs:			return obj.getEpochsIterable();
			default: 				return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }

    // ExternalDevice extends AnnotatableEntityBase
    protected static Object getProperty(ExternalDevice obj, PropertyName prop) {
    	switch (prop) {
    		case Experiment:		return obj.getExperiment();
    		case Manufacturer:		return obj.getManufacturer();
    		case Name:				return obj.getName();
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(ExternalDevice obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }

    // Source extends AnnotatableEntityBase
    protected static Object getProperty(Source obj, PropertyName prop) {
    	switch (prop) {
    		case Label:				return obj.getLabel();
    		case ParentSource: 		return obj.getParent();
    		case ParentRoot: 		return obj.getParentRoot();
    		default: 				return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Source obj, CollectionName col) {
    	switch (col) {
			case AllEpochGroups: 			return CollectionUtils.makeIterable(obj.getAllEpochGroups());
			case AllExperiments: 			return CollectionUtils.makeIterable(obj.getAllExperiments());
			case ChildLeafSourceDescendants:return CollectionUtils.makeIterable(obj.getChildLeafDescendants());
			case SourceChildren: 			return CollectionUtils.makeIterable(obj.getChildren());
			case EpochGroups: 				return CollectionUtils.makeIterable(obj.getEpochGroups());
			case Experiments: 				return CollectionUtils.makeIterable(obj.getExperiments());
			default: 						return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // Resource extends AnnotatableEntityBase
    protected static Object getProperty(Resource obj, PropertyName prop) {
    	switch (prop) {
    		case Data:	return obj.getDataBytes();
    		case Name:	return obj.getName();
    		case Notes:	return obj.getNotes();
    		case UTI:	return obj.getUti();
    		default: 	return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Resource obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // URLResource extends Resource
    protected static Object getProperty(URLResource obj, PropertyName prop) {
    	switch (prop) {
    		case URL: return obj.getURLString();
    		default: return getProperty((Resource)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(URLResource obj, CollectionName col) {
    	switch (col) {
			default: return getCollection((Resource)obj, col);
		}
    }
    
    // IOBase extends AnnotatableEntityBase implements IIOBase
    protected static Object getProperty(IIOBase obj, PropertyName prop) {
    	switch (prop) {
    		case ExternalDevice:return obj.getExternalDevice();
    		case Units:			return obj.getUnits();
    		default: 			return getProperty((IAnnotatableEntityBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(IIOBase obj, CollectionName col) {
    	switch (col) {
			case DeviceParameters: 	return Property.makeIterable(obj.getDeviceParameters());
			case DimensionLabels:	return CollectionUtils.makeIterable(obj.getDimensionLabels());
			default: return getCollection((IAnnotatableEntityBase)obj, col);
		}
    }
    
    // Stimulus extends IOBase 
    protected static Object getProperty(Stimulus obj, PropertyName prop) {
    	switch (prop) {
    		case Epoch:				return obj.getEpoch();
    		case PluginID:			return obj.getPluginID();
    		default: 				return getProperty((IIOBase)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Stimulus obj, CollectionName col) {
    	switch (col) {
			case StimulusParameters:    return Property.makeIterable(obj.getStimulusParameters());
			default: 					return getCollection((IIOBase)obj, col);
		}
    }
    
    // ResponseDataBase extends IOBase implements IResponseData 
    protected static Object getProperty(IResponseData obj, PropertyName prop) {
		NumericData   data = obj.getData();
    	try {
	    	switch (prop) {
		    	case ByteOrder:			return String.valueOf(data.getByteOrder());
		    	case NumericDataFormat:	return String.valueOf(data.getDataFormat());
		    	case NumericByteOrder:	return String.valueOf(data.getNumericByteOrder());
		    	case SampleBytes:		return data.getSampleBytes();
		    	case Data:				return data.getDataBytes();
	    		default: 				return getProperty((IIOBase)obj, prop);
	    	}
		} catch (RuntimeException ndx) { 
			// was NumericDataException, but that's no longer public (??)
			// there are several reasons this exception is thrown; not all of them are due to type-mismatch (some are just thrown because there's no data at all)
			return null;
		}
    }
    
    protected static Iterable<?> getCollection(IResponseData obj, CollectionName col) {
		NumericData 		data = obj.getData();
		NumericDataFormat 	type = data.getDataFormat();

    	switch (col) {
			case MatlabShape:		return CollectionUtils.makeIterable(obj.getMatlabShape());
			case Shape:				return CollectionUtils.makeIterable(obj.getShape());
			case FloatingPointData:	return type == NumericDataFormat.FloatingPointDataType      ? CollectionUtils.makeIterable(data.getFloatingPointData()) : null;
			case IntegerData:		return type == NumericDataFormat.SignedFixedPointDataType   ? CollectionUtils.makeIterable(data.getIntegerData()) 		: null;
            case UnsignedIntData:	return type == NumericDataFormat.UnsignedFixedPointDataType ? CollectionUtils.makeIterable(data.getUnsignedIntData()) 	: null;
			default: 				return getCollection((IIOBase)obj, col);
		}
    }
    
    // Response extends ResponseDataBase
    protected static Object getProperty(Response obj, PropertyName prop) {
    	switch (prop) {
    		case Epoch:				return obj.getEpoch();
    		case UTI:				return obj.getUTI();
    		default: 				return getProperty((IResponseData)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Response obj, CollectionName col) {
    	switch (col) {
			case SamplingRates:	return CollectionUtils.makeIterable(obj.getSamplingRates());
			case SamplingUnits: return CollectionUtils.makeIterable(obj.getSamplingUnits());
			default: 			return getCollection((IResponseData)obj, col);
		}
    }
    
    // DerivedResponse extends ResponseDataBase (extends IOBase implements IResponseDataBase)
    protected static Object getProperty(DerivedResponse obj, PropertyName prop) {
    	switch (prop) {
    		case Description: 			return obj.getDescription();
    		case Epoch:					return obj.getEpoch();
    		case Name:					return obj.getName();
    		default: 					return getProperty((IResponseData)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(DerivedResponse obj, CollectionName col) {
    	switch (col) {
			case DerivationParameters : return Property.makeIterable(obj.getDerivationParameters());
			default: 					return getCollection((IResponseData)obj, col);
		}
    }
    
    
    // ITimelineElement extends IooObj, IEntityBase, ITaggableEntityBase, IAnnotatableEntityBase 
    protected static Object getProperty(ITimelineElement obj, PropertyName prop) {
    	switch (prop) {
			case EndTime:	return obj.getEndTime();
			case StartTime:	return obj.getStartTime();
   			default: 		return getProperty((IAnnotatableEntityBase)obj, prop); 
    	}
    }
    protected static Iterable<?> getCollection(ITimelineElement obj, CollectionName col) {
    	// has no collections
    	return getCollection((IAnnotatableEntityBase)obj, col);
    }
    
    // EpochGroup extends TimelineElement
    protected static Object getProperty(EpochGroup obj, PropertyName prop) {
    	switch (prop) {
    		case EpochCount:		return obj.getEpochCount();
    		case Experiment:		return obj.getExperiment();
    		case Label:				return obj.getLabel();
    		case ParentEpochGroup:	return obj.getParent();
    		case Source:			return obj.getSource();
    		default: 				return getProperty((ITimelineElement)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(EpochGroup obj, CollectionName col) {
    	switch (col) {
			case ChildLeafGroupDescendants:	return CollectionUtils.makeIterable(obj.getChildLeafDescendants());
			case GroupChildren:				return CollectionUtils.makeIterable(obj.getChildren());
			case Epochs:					return CollectionUtils.makeIterable(obj.getEpochs());
			case EpochsUnsorted:   			return CollectionUtils.makeIterable(obj.getEpochsUnsorted());
			default: 						return getCollection((ITimelineElement)obj, col);
		}
    }
    
    // Epoch extends TimelineElement
    protected static Object getProperty(Epoch obj, PropertyName prop) {
    	switch (prop) {
    		case Duration:				return obj.getDuration();
    		case EpochGroup:			return obj.getEpochGroup();
    		case ExcludeFromAnalysis:	return obj.getExcludeFromAnalysis();
    		case NextEpoch:				return obj.getNextEpoch();
    		case PreviousEpoch:			return obj.getPreviousEpoch();
    		case ProtocolID:			return obj.getProtocolID();
    		default: 					return getProperty((ITimelineElement)obj, prop);
    	}
    }
    protected static Iterable<?> getCollection(Epoch obj, CollectionName col) {
    	switch (col) {
			case AnalysisRecords:		return CollectionUtils.makeIterable(obj.getAnalysisRecords());
			case DerivedResponses:		return obj.getDerivedResponseIterable();
			case DerivedResponseNames:	return CollectionUtils.makeIterable(obj.getDerivedResponseNames());
			case ProtocolParameters:	return Property.makeIterable(obj.getProtocolParameters());
			case Responses:				return obj.getResponseIterable();
			case ResponseNames:			return CollectionUtils.makeIterable(obj.getResponseNames());
			case StimuliNames:			return CollectionUtils.makeIterable(obj.getStimuliNames());
			case Stimuli:   			return obj.getStimulusIterable();
			default: 					return getCollection((ITimelineElement)obj, col);
		}
    }
    
    // Experiment extends PurposeAndNotesEntity (extends TimelineElement (extends AnnotatableEntityBase implements ITimelineElement) implements IOwnerNotes, IScientificPurpose)
    protected static Object getProperty(Experiment obj, PropertyName prop) {
    	switch (prop) {
	    	case Notes:					return obj.getNotes();		// 2 different interfaces so easier to just handle it here (for now)
	    	case Purpose: 				return obj.getPurpose();
	    	default: 					return getProperty((ITimelineElement)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(Experiment obj, CollectionName col) {
    	switch (col) {
    		case EpochGroups:		return CollectionUtils.makeIterable(obj.getEpochGroups());
    		case Epochs:			return obj.getEpochsIterable();
    		case ExternalDevices:	return CollectionUtils.makeIterable(obj.getExternalDevices());
    		case Projects:			return CollectionUtils.makeIterable(obj.getProjects());
    		case Sources:   		return CollectionUtils.makeIterable(obj.getSources());
    		default:				return getCollection((ITimelineElement)obj, col); 
    	
    	}
    }    
    
    // Project extends PurposeAndNotesEntity (extends TimelineElement (extends AnnotatableEntityBase implements ITimelineElement) implements IOwnerNotes, IScientificPurpose)
    protected static Object getProperty(Project obj, PropertyName prop) {
    	switch (prop) {
	    	case Name:					return obj.getName();
	    	case Notes:					return obj.getNotes();		// 2 different interfaces so easier to just handle it here (for now)
	    	case Purpose: 				return obj.getPurpose();
	    	default: 					return getProperty((ITimelineElement)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(Project obj, CollectionName col) {
    	switch (col) {
    		case AnalysisRecords: 		return obj.getAnalysisRecordIterable();
    		case AnalysisRecordNames:	return CollectionUtils.makeIterable(obj.getAnalysisRecordNames());
    		case Experiments:			return CollectionUtils.makeIterable(obj.getExperiments());
    		case MyAnalysisRecords:		return obj.getMyAnalysisRecordIterable();
    		case MyAnalysisRecordNames:	return CollectionUtils.makeIterable(obj.getMyAnalysisRecordNames());
	    	default: 					return getCollection((ITimelineElement)obj, col); 
    	}
    }
    
    // User extends TaggableEntityBase
    protected static Object getProperty(User obj, PropertyName prop) {
    	switch (prop) {
	    	case Username:	return obj.getUsername();
	    	default: 		return getProperty((ITaggableEntityBase)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(User obj, CollectionName col) {
    	switch (col) {
	    	default: return getCollection((ITaggableEntityBase)obj, col); 
    	}
    }

    // IAnnotation extends ITaggableEntityBase
    protected static Object getProperty(IAnnotation obj, PropertyName prop) {
    	switch (prop) {
	    	case Text:	return obj.getText();
	    	default: 	return getProperty((ITaggableEntityBase)obj, prop); 
    	}
    }    
    protected static Iterable<?> getCollection(IAnnotation obj, CollectionName col) {
    	switch (col) {
    		case Annotated:	return CollectionUtils.makeIterable(obj.getAnnotated()); 
	    	default:		return getCollection((ITaggableEntityBase)obj, col); 
    	}
    }
    
    // ImageAnnotatable extends IAnnotatableEntityBase
    /** @since 1.2 */
    protected static Object getProperty(ImageAnnotatable obj, PropertyName prop) {
        switch (prop) {
            // TODO? public abstract java.io.InputStream getDataStream();
            // TODO? public abstract byte[] getDataBytes();
            default:    return getProperty((IAnnotatableEntityBase)obj, prop); 
        }
    }
    /** @since 1.2 */
    protected static Iterable<?> getCollection(ImageAnnotatable obj, CollectionName col) {
        switch (col) {
            case MyCoordinateSystems:   return CollectionUtils.makeIterable(obj.getMyCoordinateSystems());
            case CoordinateSystems:     return CollectionUtils.makeIterable(obj.getCoordinateSystems());
            default:                    return getCollection((IAnnotatableEntityBase)obj, col); 
        }
    }
    
    // CoordinateSystem extends EntityBase
    /** @since 1.2 */
    protected static Object getProperty(CoordinateSystem obj, PropertyName prop) {
        switch (prop) {
            case Name : return obj.getName();
            default:    return getProperty((IEntityBase)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(CoordinateSystem obj, CollectionName col) {
        switch (col) {
            case ReferencePoint:return CollectionUtils.makeIterable(obj.getReferencePoint());
            case ScaleFactors:  return CollectionUtils.makeIterable(obj.getScaleFactors());
            case Units:         return CollectionUtils.makeIterable(obj.getUnits());
            default:            return getCollection((IEntityBase)obj, col); 
        }
    }

/* TODO - none of these are public, but each has a *View class which is and implements IShape which is what's returned by getShape below    
    * ***** Line (endX:double, endY:double, startX:double, startY:double) - v1.2
    
    * ***** Oval (height:double, width:double, x:double, y:double) - v1.2
    * ***** Point (coordinates:double[]) - v1.2
    * ***** Polygon (xCoordinates:double[], yCoordinates:double[]) - v1.2
    
*/

    // IShape extends nothing, but is implemented by LineView, OvalView, PointView, and PolygonView (all of which have different data, which makes the metadata difficult)
    // everything that implements IShape (currently) also don't extend anything so we can't pass the call up to anybody
    /** @since 1.2 */
    protected static Object getProperty(IShape obj, PropertyName prop) {
        switch (prop) {
            case CoordinateSystem: obj.getCoordinateSystem();
            default              : _log.error("Unknown property '" + prop + "' for type '" + obj + "'"); return null;   // nowhere to go from here 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(IShape obj, CollectionName col) {
        switch (col) {
            default: _log.error("Unknown collection '" + col + "' for type '" + obj + "'"); return null; 
        }
    }
    
    // URLResponse extends Response
    /** @since 1.2 */
    protected static Object getProperty(URLResponse obj, PropertyName prop) {
        switch (prop) {
            case Data       : return obj.getData();
            case DataBytes  : return obj.getDataBytes();
//          case DataStream : return obj.getDataStream();
            case URL        : return obj.getURLString();
            default         : return getProperty((Response)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(URLResponse obj, CollectionName col) {
        switch (col) {
            default: return getCollection((Response)obj, col); 
        }
    }

    // IndexedURLResponse extends URLResponse
    /** @since 1.2 */
    protected static Object getProperty(IndexedURLResponse obj, PropertyName prop) {
        switch (prop) {
            case End    : return Long.valueOf(obj.getEnd());
            case Start  : return Long.valueOf(obj.getStart());
            default     : return getProperty((URLResponse)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(IndexedURLResponse obj, CollectionName col) {
        switch (col) {
            default: return getCollection((URLResponse)obj, col); 
        }
    }


    // ImageAnnotation implements IAnnotation
    /** @since 1.2 */
    protected static Object getProperty(ImageAnnotation obj, PropertyName prop) {
        switch (prop) {
            case Shape  : return obj.getShape(); 
            default     : return getProperty((IAnnotation)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(ImageAnnotation obj, CollectionName col) {
        switch (col) {
            default: return getCollection((IAnnotation)obj, col); 
        }
    }

    // INoteAnnotation extends IAnnotation
    /** @since 1.2 */
    protected static Object getProperty(INoteAnnotation obj, PropertyName prop) {
        switch (prop) {
            default     : return getProperty((IAnnotation)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(INoteAnnotation obj, CollectionName col) {
        switch (col) {
            default: return getCollection((IAnnotation)obj, col); 
        }
    }

    // ImageAnnotation implements IAnnotation
    /** @since 1.2 */
    protected static Object getProperty(TimelineAnnotation obj, PropertyName prop) {
        switch (prop) {
            case EndTime    : return obj.getEndTime();
            case StartTime  : return obj.getStartTime();
            default         : return getProperty((IAnnotation)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(TimelineAnnotation obj, CollectionName col) {
        switch (col) {
            default: return getCollection((IAnnotation)obj, col); 
        }
    }

    // Group implements ITaggableEntityBase
    /** @since 1.2 */
    protected static Object getProperty(Group obj, PropertyName prop) {
        switch (prop) {
            case GroupName : return obj.getGroupName();
            default         : return getProperty((ITaggableEntityBase)obj, prop); 
        }
    }    
    /** @since 1.2 */
    protected static Iterable<?> getCollection(Group obj, CollectionName col) {
        switch (col) {
            default: return getCollection((ITaggableEntityBase)obj, col); 
        }
    }

    
    /* cast the Value type to a type which subclasses IEntityBase
     * @throws ClassCastException if the value-type doesn't extended from Ovations IEntityBase
     */
//    protected Class<? extends IEntityBase> getOvationEntityType() { return (Class<? extends IEntityBase>) getEntityType(); }
    
    @SuppressWarnings("unchecked")
	protected Iterable<V> executeQueryInfo() {
        return (Iterable<V>)executeQueryInfo(getEntityType(), getQueryInfo());
    }
    
    @SuppressWarnings("unchecked")
	protected Iterable<V> executeQuery(String query) {
    	return (Iterable<V>)CollectionUtils.makeIterable(executeQuery(getEntityType(), query));
    }
    
    protected static Iterable<? extends IEntityBase> executeQueryInfo(Class<? extends IEntityBase> type, QueryInfo info) {
        Map<String,String> customOptions = (info != null && info.customOptions != null) ? info.customOptions : null;
        if (customOptions != null) {
            String pqlQuery = customOptions.get("pql");
            String entityUrl = customOptions.get("url");
            if (pqlQuery != null) {
                Iterator<? extends IEntityBase> iter = executeQuery((Class<? extends IEntityBase>)type, pqlQuery);
                _log.info("query '" + pqlQuery + "' = " + iter);
                return CollectionUtils.makeIterable(iter);
            } else
            if (entityUrl != null) {
            	IEntityBase obj = getByURI(entityUrl);
                _log.info("url '" + entityUrl + "' = " + obj);
                if (obj == null) return CollectionUtils.makeEmptyIterable();
                return CollectionUtils.makeIterable(obj);
            } else {
                _log.info("both pql and url params are null");
            }
        } else {
            _log.info("customOptions is null - info:" + info);
        }
        return null;
    }
    
	public static <T extends IEntityBase> Iterator<T> executeQuery(Class<? extends IEntityBase> type, String pqlQuery) {
    	return DataContextCache.getThreadContext().query(type, pqlQuery);
    }

    public static IEntityBase getByURI(String uri) { 
    	return DataContextCache.getThreadContext().objectWithURI(uri);
    }

	public static String 			convertURLToString(URL url) 	{ return url != null ? url.toExternalForm() : null; }
}
