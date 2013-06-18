package fr.ac_versailles.crdp.apiscol.content;

import org.apache.commons.lang.StringUtils;


public enum ContentType {
	asset("asset"), sco("sco"), url("url");
	private String value;

	private ContentType(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static ContentType convertStringToType(String scormType) {
		if (StringUtils.isBlank(scormType))
			return asset;
		scormType = scormType.toLowerCase().trim();
		if (scormType.equals("asset"))
			return asset;
		if (scormType.equals("sco"))
			return sco;
		if (scormType.equals("url"))
			return url;
		return asset;

	}

	public static boolean isLink(String type) {
		return StringUtils.equals(type, ContentType.url.toString());
	}

	public static boolean isFile(String type) {
		return StringUtils.equals(type, ContentType.sco.toString())
				|| StringUtils.equals(type, ContentType.asset.toString());
	}
}
