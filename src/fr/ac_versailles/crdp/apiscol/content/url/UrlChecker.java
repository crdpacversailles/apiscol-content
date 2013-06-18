package fr.ac_versailles.crdp.apiscol.content.url;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class UrlChecker {

	public static String checkUrlSyntax(String url) throws InvalidUrlException {
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			throw new InvalidUrlException(String.format(
					"The url %s is not acceptable for this reason : %s", url,
					e.getMessage()));
		}
		URI uri;
		try {
			uri = new URI(url);
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
					uri.getPort(), uri.getPath(), uri.getQuery(), null)
					.toString();
		} catch (URISyntaxException e) {
			throw new InvalidUrlException(String.format(
					"The url %s is not acceptable for this reason : %s", url,
					e.getMessage()));
		}
	}
}
