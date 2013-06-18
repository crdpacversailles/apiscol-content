package fr.ac_versailles.crdp.apiscol.content.languageDetection;

import org.apache.tika.language.LanguageIdentifier;

public class TikaLanguageDetector extends AbstractLanguageDetector {

	@Override
	public String detectLanguage(String texte) {
		return new LanguageIdentifier(texte).getLanguage();
	}


}
