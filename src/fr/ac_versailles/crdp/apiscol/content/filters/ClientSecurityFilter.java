package fr.ac_versailles.crdp.apiscol.content.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import fr.ac_versailles.crdp.apiscol.utils.LogUtility;
import fr.ac_versailles.crdp.apiscol.utils.SecurityUtils;

public class ClientSecurityFilter extends ClientFilter {

	private Map<String, String> secrets;
	private static Logger logger;

	public ClientSecurityFilter() {
		createLogger();
		secrets = new HashMap<String, String>();
	}

	public void addKey(String url, String secret) {
		secrets.put(url, secret);
	}

	@Override
	public ClientResponse handle(ClientRequest request)
			throws ClientHandlerException {
		ClientRequest mcr = addAuthorizationToken(request);
		ClientResponse resp = getNext().handle(mcr);
		return resp;
	}

	private ClientRequest addAuthorizationToken(ClientRequest request) {
		String ifMatch = (String) request.getHeaders().getFirst(
				HttpHeaders.IF_MATCH);
		List<Object> headerContent = new ArrayList<Object>();
		String url = request.getURI().toString();
		Iterator<String> it = secrets.keySet().iterator();
		String secretHashTableKey="";
		while (it.hasNext()) {
			String key = (String) it.next();
			if(url.startsWith(key))
				secretHashTableKey=key;
		}
		if(StringUtils.isEmpty(secretHashTableKey)) {
			logger.error(String.format("no shared secret registred for uri %s", url));
			return request;
		}
		headerContent.add(SecurityUtils.hashWithSharedSecret(ifMatch,
				secrets.get(secretHashTableKey)));
		
		request.getHeaders().put(HttpHeaders.AUTHORIZATION, headerContent);
		return request;
	}
	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}
}
