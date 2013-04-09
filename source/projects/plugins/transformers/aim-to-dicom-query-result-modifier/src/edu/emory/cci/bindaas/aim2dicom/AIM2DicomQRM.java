package edu.emory.cci.bindaas.aim2dicom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.BundleContext;
import org.springframework.util.StopWatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import edu.emory.cci.bindaas.aim2dicom.bundle.Activator;
import edu.emory.cci.bindaas.commons.xml2json.XML2JSON;
import edu.emory.cci.bindaas.commons.xml2json.model.Mapping;
import edu.emory.cci.bindaas.commons.xml2json.model.Type;
import edu.emory.cci.bindaas.core.util.ProfilerService;
import edu.emory.cci.bindaas.framework.api.IQueryResultModifier;
import edu.emory.cci.bindaas.framework.model.ModifierException;
import edu.emory.cci.bindaas.framework.model.QueryResult;
import edu.emory.cci.bindaas.framework.model.QueryResult.Callback;
import edu.emory.cci.bindaas.framework.util.DocumentationUtil;
import edu.emory.cci.bindaas.framework.util.GSONUtil;
import edu.emory.cci.bindaas.framework.util.StandardMimeType;

public class AIM2DicomQRM implements IQueryResultModifier {

	private Log log = LogFactory.getLog(getClass());
	private static final String DOCUMENTATION_RESOURCES_LOCATION = "META-INF/documentation";
	private JsonObject documentation;
	private final static String seriesUIDAttributeName ="seriesUID";

	@Override
	public JsonObject getDocumentation() {

		return documentation;
	}

	public void init() {
		BundleContext context = Activator.getContext();
		documentation = DocumentationUtil.getProviderDocumentation(context,
				DOCUMENTATION_RESOURCES_LOCATION);
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("class", getClass().getName());
		context.registerService(IQueryResultModifier.class.getName(), this,
				props);
	}

	@Override
	public void validate() throws ModifierException {
		// TODO Auto-generated method stub

	}

	@Override
	public QueryResult modifyQueryResult(final QueryResult queryResult,
			JsonObject dataSource, String user, JsonObject modifierProperties)
			throws Exception {
		
		
		final AIM2DicomQRMProperties props = GSONUtil.getGSONInstance()
				.fromJson(modifierProperties, AIM2DicomQRMProperties.class);
		if (props != null && props.imageURL != null) {
			queryResult.setCallback(true);
			queryResult.setMime(true);
			queryResult.setMimeType(StandardMimeType.ZIP.toString());
			queryResult.setCallback(new Callback() {

				@Override
				public void callback(OutputStream servletOutputStream,
						Properties context) throws Exception {
					ProfilerService profiler = Activator.getProfilerService();
					boolean enableProfiling = profiler!=null && profiler.isEnabled();
					StopWatch stopWatch = null;
					if(enableProfiling)
						 stopWatch = profiler.getThreadLocalStopWatch();
					
					enableProfiling = enableProfiling && stopWatch!=null;
					
					try {
						
						if(enableProfiling)
							stopWatch.start("parseAnnotations");
						List<JsonObject> annotations = parseAnnotations(queryResult
								.getData());
						
						// Create a set of URLs from where to download images
						
						Set<String> setOfUniqueSeries = new HashSet<String>();
						
						for (JsonObject annotation : annotations) {
						
							if(annotation.get(seriesUIDAttributeName)!=null)
							{
								String seriesUID = annotation.get(seriesUIDAttributeName)
										.getAsString();
								setOfUniqueSeries.add(seriesUID);	
							}
							else
							{
								log.warn("Encountered annotation missing seriesUID");
							}
							
						}

						// create json array
						
						JsonArray arrayOfSeries = GSONUtil.getGSONInstance().toJsonTree(setOfUniqueSeries,HashSet.class).getAsJsonArray();

						// download and stream image
						if(enableProfiling)
							{
								stopWatch.stop();
								stopWatch.start("Download and Stream [" + setOfUniqueSeries.size() + "] series");
							}
						String seriesJson = arrayOfSeries.toString().trim();
						writeDicomImage(props.imageURL, props.apiKey,seriesJson.substring(1, seriesJson.length() - 1) ,  servletOutputStream);
						
					} catch (Exception e) {
						log.error(e);
						throw e;
					}
					finally{
						if(enableProfiling && stopWatch.isRunning())
						{
							stopWatch.stop();
							log.info(stopWatch.prettyPrint());
						}
					}
				}

				private void writeDicomImage(String imageUrlToFetchDicom,
						String apiKey , String series , OutputStream zos) throws ClientProtocolException,
						IOException {
					HttpPost post = new HttpPost(imageUrlToFetchDicom);
					post.addHeader("api_key", apiKey);
					
					List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			        nvps.add(new BasicNameValuePair(seriesUIDAttributeName, series)) ;
			        post.setEntity(new UrlEncodedFormEntity(nvps));
			        
					HttpClient httpClient = new DefaultHttpClient();
					HttpResponse response = httpClient.execute(post);
					if (response != null && response.getStatusLine().getStatusCode() == 200 && response.getEntity() != null) {
						response.getEntity().writeTo(zos);
					}
					else
					{
						if(response!=null)
							log.warn("Request to url [" + imageUrlToFetchDicom + "] failed. Reason=[" + response.getStatusLine().toString() + "]");
						else
							log.warn("Request to url [" + imageUrlToFetchDicom + "] failed");
					}
				}

				private List<JsonObject> parseAnnotations(byte[] dataBytes)
						throws Exception {
					log.debug("Parsing Annotations");
					Mapping seriesUID = new Mapping();
					seriesUID.setName(seriesUIDAttributeName);
					seriesUID.setType(Type.SIMPLE);
					seriesUID
							.setXpath("/ns1:ImageAnnotation/ns1:imageReferenceCollection/ns1:ImageReference/ns1:imageStudy/ns1:ImageStudy/ns1:imageSeries/ns1:ImageSeries/@instanceUID");

					XML2JSON xml2json = new XML2JSON();
					Map<String, String> prefixes = xml2json.getPrefixes();
					prefixes.put("ns1",
							"gme://caCORE.caCORE/3.2/edu.northwestern.radiology.AIM");
					xml2json.setNamespaceAware(true);
					xml2json.setMappings(Arrays
							.asList(new Mapping[] { seriesUID }));
					xml2json.setRootElementSelector("/results/ns1:ImageAnnotation");
					xml2json.init();

					InputStream is = new ByteArrayInputStream(dataBytes);

					return xml2json.parseXML(is);
				}
			});

		} else {
			String error = "QRM properties missing attribute imageUrl";
			log.error(error);
			throw new Exception(error);
		}
		return queryResult;
	}

	public static class AIM2DicomQRMProperties {
		@Expose
		private String imageURL;
		@Expose
		private String apiKey;

		public String getImageUrl(String seriesInstanceUID) {
			if (imageURL != null) {
				String imageUrl = this.imageURL + "?seriesUID="
						+ seriesInstanceUID;
				return imageUrl;
			} else
				return null;

		}
	}

	@Override
	public String getDescriptiveName() {

		return "Fetch DICOM Images for AIM annotations";
	}

}