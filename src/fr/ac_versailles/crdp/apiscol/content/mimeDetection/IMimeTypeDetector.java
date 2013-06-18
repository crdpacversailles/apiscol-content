package fr.ac_versailles.crdp.apiscol.content.mimeDetection;

import java.io.File;

public interface IMimeTypeDetector {

	String detectType(File mainFile);

}
