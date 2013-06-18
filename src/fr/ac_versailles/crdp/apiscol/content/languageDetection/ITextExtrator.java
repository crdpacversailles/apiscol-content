package fr.ac_versailles.crdp.apiscol.content.languageDetection;

import java.io.File;
import java.net.URL;

public interface ITextExtrator {

	String extractText(File file);

	String extractText(URL url);


}
