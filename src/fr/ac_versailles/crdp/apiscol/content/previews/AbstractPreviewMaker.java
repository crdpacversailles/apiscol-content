package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.content.AsyncProcessTrackingObject;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractPreviewMaker implements IPreviewMaker {

	private static final int BUFFER_SIZE = 153600;
	protected String previewDirectoryPath;
	protected final String resourceId;
	protected final String entryPoint;
	protected final String realPath;
	protected final String previewUri;

	public AbstractPreviewMaker(String resourceId, String previewsRepoPath,
			String entryPoint, String realPath, String previewUri) {
		this.realPath = realPath;
		this.previewUri = previewUri;
		this.resourceId = resourceId;
		this.entryPoint = entryPoint;
		createLogger();
		previewDirectoryPath = buildPreviewsDirectoryPath(previewsRepoPath,
				resourceId);
	}

	public static String buildPreviewsDirectoryPath(String previewsRepoPath,
			String rId) {
		return FileUtils.getFilePathHierarchy(previewsRepoPath, rId);
	}

	protected Logger logger;
	protected AsyncProcessTrackingObject trackingObject;
	protected Integer identifier;

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

	@Override
	public void run() {
		deletePreview();
		createPreviewDirectory();
		createNewPreview();

	}

	protected void createPreviewDirectory() {
		new File(previewDirectoryPath).getParentFile().mkdirs();
	}

	abstract protected void createNewPreview();

	protected boolean deletePreview() {
		boolean success = true;
		File resourceDirectory = new File(previewDirectoryPath);
		if (!resourceDirectory.exists())
			return true;
		File parent = resourceDirectory.getParentFile();
		File grandParent = parent.getParentFile();
		File grandgrandParent = grandParent.getParentFile();

		success &= FileUtils.deleteDir(resourceDirectory);

		if (success && parent.list().length == 0) {
			success &= FileUtils.deleteDir(parent);
			if (success && grandParent.list().length == 0) {
				success &= FileUtils.deleteDir(grandParent);
				if (success && grandgrandParent.list().length == 0) {
					success &= FileUtils.deleteDir(grandgrandParent);
				}
			}
		}
		return success;
	}

	protected void writePreviewFileToDisk(String urlStr) {
		writePreviewFileToDisk(urlStr, StringUtils.EMPTY);
	}

	protected void writePreviewFileToDisk(String urlStr, String forceName) {
		try {
			URL url = new URL(urlStr);
			url.openConnection();
			InputStream reader = url.openStream();
			String imageName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
			String imageExt = urlStr.substring(urlStr.lastIndexOf(".") + 1);
			String filePath;
			if (StringUtils.isEmpty(forceName))
				filePath = previewDirectoryPath + "/" + imageName;
			else
				filePath = previewDirectoryPath + "/" + forceName + "."
						+ imageExt;
			File file = new File(filePath);
			file.getParentFile().mkdirs();
			FileOutputStream writer = new FileOutputStream(file);
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = 0;
			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[BUFFER_SIZE];
			}
			writer.close();
			reader.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void setTrackingObject(AsyncProcessTrackingObject trackingObject) {
		this.trackingObject = trackingObject;

	}

}
