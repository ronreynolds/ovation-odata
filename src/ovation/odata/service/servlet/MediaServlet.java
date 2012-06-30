package ovation.odata.service.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import ovation.IEntityBase;
import ovation.Resource;
import ovation.Response;
import ovation.odata.model.OvationModelBase;
import ovation.odata.util.TypeDataMap;
import ovation.odata.util.TypeDataMap.TypeData;

/**
 * servlet to serve up media (images, avis, etc) based on element URI
 * e.g., http://localhost:8080/ovodata/media/ovation%3A%2F%2F%2F2c8fec8a-0248-444b-b42b-af46df407712%2F%237-2-1-2%3A1000049
 *                                          (ovation:///2c8fec8a-0248-444b-b42b-af46df407712/#7-2-1-2:1000049) 
 * @author Ron
 *
 */
public class MediaServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger _log = Logger.getLogger(MediaServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	    final String requestUri  = req.getRequestURI();    // "/ovodata/media/fuzzywuzzywuzabear"
	    final String contextPath = req.getContextPath();   // "/ovodata"
	    final String servletPath = req.getServletPath();   // "/media"
	    
	    String uri = requestUri.substring(contextPath.length());   // trims off the "/ovodata" part
	    uri = (uri.length() > servletPath.length() + 1) ? uri.substring(servletPath.length() + 1) : null;
	    
		_log.debug("uri = " + uri);
		
		if (uri == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No resource specified");
			return;
		}
		
//		uri = URLDecoder.decode(uri);
		
		// this requires the user already be authenticated in the context of this request
		IEntityBase entity = OvationModelBase.getByURI(uri);
		if (entity == null) {
		    _log.info("no resource found for uri '" + uri + "'");
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find entity " + uri);
			return;
		}
		
		// these are the 2 types with UTIs
		Resource resource = entity instanceof Resource ? (Resource)entity : null;
		Response response = entity instanceof Response ? (Response)entity : null;

		if (resource == null && response == null) {
            _log.info("entity @ '" + uri + "' is not supported - " + entity);
			resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, entity + " is of wrong type");
			return;
		}
		
		// load the requested media
		String uti  = resource != null ? resource.getUti()        : response.getUTI();
		byte[] data = resource != null ? resource.getDataBytes()  : response.getDataBytes();
		
		if (data == null) {
            _log.info("entity @ '" + uri + "' contains no data to return - " + entity);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find any data for " + uri);
			return;
		}
		
		// set response content type
		setContentType(resp, uti);
		resp.setContentLength(data.length);
		
		// stream media back to client
		resp.getOutputStream().write(data);
		
		// done - tomcat should handle flushing and all that
        _log.info("returned entity @ '" + uri + "' - " + entity);

	}
	
	static void setContentType(HttpServletResponse resp, String uti) {
		TypeData typeData = TypeDataMap.getUTIData(uti);
		if (typeData != null) {
			resp.setContentType(typeData.getMimeType());
		} else {
			_log.error("Unknown UTI type - '" + uti + "'");
		}
	}
	
	/** @since 1.2 */
	public static String generateUrl(IEntityBase obj) { 
	    return "media/" + obj.getURIString(); //URLEncoder.encode(obj.getURIString()); 
	}
}