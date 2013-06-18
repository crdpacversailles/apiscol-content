package fr.ac_versailles.crdp.apiscol.content.thumbs;

import fr.ac_versailles.crdp.apiscol.content.ContentType;


public class ThumbExtracterFactory {

	public static ThumbExtracter getExtracter(String scormType) {
		if(ContentType.isFile(scormType))
		return new FileThumbExtracter();
		else return new WebPageThumbExtracter(); 
	}

}
