package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.previews.AbstractPreviewMaker;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;

public class FileThumbExtracter implements ThumbExtracter {

	@Override
	public Map<String, Point> getThumbsFromResource(String resourceId,
			IResourceDataHandler resourceDataHandler, String baseUrl,
			String mainFileName, double minDimensionsSum)
			throws ResourceDirectoryNotFoundException {
		Map<String, Point> urlList = new HashMap<String, Point>();
		Map<String, Point> imagesFilePathList = ResourceDirectoryInterface
				.getImagesList(resourceId, mainFileName);
		Iterator<String> it = imagesFilePathList.keySet().iterator();
		while (it.hasNext()) {
			String filePath = (String) it.next();
			Point image = imagesFilePathList.get(filePath);
			try {
				if (!imageSizeIsSufficient(image, minDimensionsSum))
					continue;
			} catch (InvalidImageException e) {
				e.printStackTrace();
				continue;
			}
			urlList.put(
					getResourceThumbUrlFromFilePath(baseUrl, resourceId,
							filePath), image);
		}
		return urlList;
	}

	private String getResourceThumbUrlFromFilePath(String baseUri,
			String resourceId, String filePath) {
		return String
				.format("%sresources%s",
						baseUri,
						FileUtils.getFilePathHierarchy("", resourceId + "/"
								+ filePath));
	}

	@Override
	public Map<String, Point> getThumbsFromPreview(String resourceId,
			String previewsRepoPath, String baseUrl) {
		Map<String, Point> urlList = new HashMap<String, Point>();
		Map<String, Point> imagesFilePathList = ResourceDirectoryInterface
				.getImagesInPreviewDirectoryList(AbstractPreviewMaker
						.buildPreviewsDirectoryPath(previewsRepoPath,
								resourceId));
		Iterator<String> it = imagesFilePathList.keySet().iterator();
		while (it.hasNext()) {
			String filePath = (String) it.next();
			Point image = imagesFilePathList.get(filePath);
			urlList.put(
					getPreviewThumbUrlFromFilePath(baseUrl, resourceId,
							filePath), image);
		}
		return urlList;
	}

	private boolean imageSizeIsSufficient(Point imageSize,
			double minDimensionSum) throws InvalidImageException {
		double width = imageSize.getX();
		double height = imageSize.getY();
		return width + height > minDimensionSum;
	}

	private String getPreviewThumbUrlFromFilePath(String baseUri,
			String resourceId, String filePath) {
		return String
				.format("%spreviews%s",
						baseUri,
						FileUtils.getFilePathHierarchy("", resourceId + "/"
								+ filePath));
	}

}
