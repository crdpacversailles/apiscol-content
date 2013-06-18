package fr.ac_versailles.crdp.apiscol.content.previews;

import java.util.HashMap;
import java.util.Map;

public class MapTokenResolver implements ITokenResolver {

	protected Map<String, String> tokenMap = new HashMap<String, String>();

	public MapTokenResolver(Map<String, String> tokenMap) {
		this.tokenMap = tokenMap;
	}

	public String resolveToken(String tokenName) {
		return this.tokenMap.get(tokenName);
	}

}
