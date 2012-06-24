package ovation.odata.service;

import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.ODataProducerFactory;

import ovation.odata.model.ExtendedPropertyModel;
import ovation.odata.model.OvationModelBase;
import ovation.odata.util.DataContextCache;
import ovation.odata.util.PropertyManager;
import ovation.odata.util.Props;

/**
 * adapts OData4J's framework to Ovation's model 
 * @author Ron
 */
public class OvationOData4JProducer extends ExtendedInMemoryProducer {
	public static final Logger _log = Logger.getLogger(OvationOData4JProducer.class);

	public static String getServiceName() { 
		return PropertyManager.getProperties(OvationOData4JProducer.class).getProperty(Props.SERVER_NAME, Props.SERVER_NAME_DEFAULT);
	}

	/**
	 * used when deploying into Tomcat environment via cmd-line/setenv.sh:
	 * 	-Dodata4j.producerfactory=ovation.odata.service.OvationOData4JServer$Factory
	 * or in web.xml:
	 * <init-param>
	 *   <param-name>odata4j.producerfactory</param-name>
	 *   <param-value>ovation.odata.service.OvationOData4JProducer$Factory</param-value>
	 * </init-param>
	 * 
	 * basically if this guy is called we're NOT in a Jersey stand-alone environment
	 * @author Ron
	 */
	public static class Factory implements ODataProducerFactory { 
		public ODataProducer create(Properties props) {
			return new OvationOData4JProducer();
		}
	}
	
	public static Properties getProps() { return PropertyManager.getProperties(OvationOData4JProducer.class); }
	
	public OvationOData4JProducer() {
		super(getProps().getProperty(Props.SERVER_NAME, Props.SERVER_NAME_DEFAULT), 
			  Props.getProp(getProps(), Props.SERVER_MAX_RESULTS, Props.SERVER_MAX_RESULTS_DEFAULT));
        registerHandlers();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void registerHandlers() {
		// register all the basic handlers
		OvationModelBase.registerOvationModel();
		
		// register all handlers with OData4J
		Set<String> allEntityNames = ExtendedPropertyModel.getEntityNames();
		for (String name : allEntityNames) {
			ExtendedPropertyModel model = ExtendedPropertyModel.getPropertyModel(name);
			register(model.getEntityType(), model, name, model.allGetter());
		}
	}

    /**
     * Releases any resources managed by this producer.
     */
    @Override
	public void close() {
		// clean-up
		DataContextCache.close();
	}
}