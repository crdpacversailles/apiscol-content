package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;

import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.content.ResourceApi;
import fr.ac_versailles.crdp.apiscol.content.filters.ClientSecurityFilter;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class ConversionServerInterface {

	private static Logger logger;
	private static Client client;
	private static String conversionWsSharedSecret;
	private static WebResource conversionWebServiceResource;
	private static boolean initialized = false;

	public static List<String> askForConversion(String filePath,
			Set<String> mimeTypeList) {
		String jsonMimeTypesList = new Gson().toJson(mimeTypeList);
		File file = new File(filePath);
		FormDataMultiPart form = new FormDataMultiPart()
				.field("file", file, MediaType.MULTIPART_FORM_DATA_TYPE)
				.field("output-mime-types", jsonMimeTypesList)
				.field("fname", file.getName()).field("page-limit", "10");
		System.out.println("asking for conversion of file "
				+ file.getAbsolutePath());
		ClientResponse previewsWebServiceResponse;
		try {
			previewsWebServiceResponse = conversionWebServiceResource
					.path("conversion").accept(MediaType.APPLICATION_XML)
					.type(MediaType.MULTIPART_FORM_DATA)
					.header(HttpHeaders.IF_MATCH, UUID.randomUUID().toString())
					.entity(form).post(ClientResponse.class);
		} catch (UniformInterfaceException e2) {
			logger.error("Request to previews web service was aborted for file"
					+ filePath);
			e2.printStackTrace();
			return null;
		} catch (ClientHandlerException e2) {
			logger.error("Request to previews web service was aborted for file"
					+ filePath);
			e2.printStackTrace();
			return null;
		}
		Document xmlResponse;
		int status = previewsWebServiceResponse.getStatus();

		if (Response.Status.OK.getStatusCode() == status) {
			try {
				xmlResponse = previewsWebServiceResponse
						.getEntity(Document.class);
			} catch (Exception e1) {
				String string = "Error while trying to connect to previews service for file "
						+ filePath;
				logger.error(string);
				return null;
			}
		} else {
			String message = previewsWebServiceResponse.getEntity(String.class);
			String string = "Error response while trying to connect to previews service for file "
					+ filePath
					+ " With code "
					+ status
					+ " and message : "
					+ message;
			logger.error(string);
			return null;
		}

		String conversionUrl = extractConversionUrl(xmlResponse);
		System.out.println(conversionUrl);
		String conversionId = conversionUrl.replace(
				conversionWebServiceResource.getURI().toString()
						+ "/conversion/", "");
		long duration = 1000;
		int counter = 0;
		boolean success = false;
		while (counter < 100) {
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			}
			previewsWebServiceResponse = conversionWebServiceResource
					.path("conversion").path(conversionId)
					.accept(MediaType.APPLICATION_XML)
					.header(HttpHeaders.IF_MATCH, UUID.randomUUID().toString())
					.get(ClientResponse.class);
			try {
				xmlResponse = previewsWebServiceResponse
						.getEntity(Document.class);
			} catch (Exception e1) {
				logger.error("Error while trying to get conversion representation from previews service for file "
						+ filePath + " with uri " + conversionUrl);
				return null;
			}
			String state = extractConversionState(xmlResponse);
			if (state.equals("success")) {
				success = true;
				break;
			}
			if (counter > 10)
				duration = 10000;
			if (counter > 50)
				duration = 100000;
			counter++;
		}
		if (!success) {
			logger.error("Error while trying to get format conversion result for file "
					+ filePath);
			return null;
		}
		List<String> images = extractImagesUrisFromResponse(xmlResponse,
				mimeTypeList);

		return images;
	}

	private static List<String> extractImagesUrisFromResponse(
			Document xmlResponse, Set<String> mimeTypeList) {
		List<String> list = new ArrayList<String>();
		Element root = xmlResponse.getDocumentElement();
		NodeList formats = ((Element) root.getElementsByTagNameNS(
				UsedNamespaces.APISCOL.toString(), "output").item(0))
				.getElementsByTagNameNS(UsedNamespaces.APISCOL.toString(),
						"format");
		Element format, file;
		NodeList files;
		for (int i = 0; i < formats.getLength(); i++) {
			format = (Element) formats.item(i);
			if (mimeTypeList.contains(format.getAttribute("mime-type"))) {
				files = format.getElementsByTagNameNS(
						UsedNamespaces.APISCOL.toString(), "file");
				for (int j = 0; j < files.getLength(); j++) {
					file = (Element) files.item(j);
					list.add(file.getAttribute("href"));
					System.out.println(file.getAttribute("href"));
				}
			}
		}

		return list;
	}

	private static String extractConversionState(Document xmlResponse) {
		Element root = xmlResponse.getDocumentElement();
		return ((Element) root.getElementsByTagNameNS(
				UsedNamespaces.APISCOL.toString(), "state").item(0))
				.getTextContent().trim().toLowerCase();
	}

	private static String extractConversionUrl(Document xmlResponse) {
		Element root = xmlResponse.getDocumentElement();

		return ((Element) root.getElementsByTagNameNS(
				UsedNamespaces.ATOM.toString(), "link").item(0))
				.getAttribute("href");
	}

	private static void createLogger() {
		if (logger == null)
			logger = LogUtility.createLogger(ConversionServerInterface.class
					.getCanonicalName());
	}

	public static void initialize(ServletContext context) {
		if (initialized)
			return;
		createLogger();
		client = Client.create();

		conversionWsSharedSecret = ResourceApi.getProperty(
				ParametersKeys.conversionWsSharedSecret, context);
		URI conversionWebserviceUrl = null;
		try {
			conversionWebserviceUrl = new URI(ResourceApi.getProperty(
					ParametersKeys.conversionWebserviceUrl, context));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		conversionWebServiceResource = client.resource(UriBuilder.fromUri(
				conversionWebserviceUrl).build());
		ClientSecurityFilter securityFilter = new ClientSecurityFilter();
		securityFilter.addKey(conversionWebserviceUrl.toString(),
				conversionWsSharedSecret);

		conversionWebServiceResource.addFilter(securityFilter);
		initialized = true;

	}

}
