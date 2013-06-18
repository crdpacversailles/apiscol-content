package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class WebPageThumbExtracter implements ThumbExtracter {
	// TODO parameter
	private static Logger logger;

	public WebPageThumbExtracter() {
		createLogger();
	}

	@Override
	public Map<String, Point> getThumbsFromResource(String resourceId,
			IResourceDataHandler resourceDataHandler, String baseUrl,
			String mainFileName, double minDimensionsSum)
			throws DBAccessException, InexistentResourceInDatabaseException {
		Map<String, Point> list = new HashMap<String, Point>();
		String url = resourceDataHandler.getUrlForResource(resourceId);
		if (StringUtils.isEmpty(url)) {
			logger.warn(String
					.format("The provided url for thumb extraction in content web service for resource %s is void ",
							resourceId));

			return Collections.emptyMap();
		}
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (Exception e) {
			logger.warn(String
					.format("This is not a valid url for thumb extraction for resource %s in content web service : %s",
							resourceId, url));
			return Collections.emptyMap();
		}
		if (doc == null)
			return Collections.emptyMap();
		Elements images = doc.select("img");
		Iterator<Element> it = images.iterator();
		while (it.hasNext()) {
			Element element = it.next();
			String src = element.attr("abs:src");
			try {
				Point size = getImageSize(src);
				if (imageSizeIsSufficient(size, minDimensionsSum))
					list.put(src, size);
			} catch (InvalidImageException e) {
				// nothing. we dont't take the image. Has been logged yet.
			}

		}

		return list;
	}

	private Point getImageSize(String href) throws InvalidImageException {
		URL url;
		try {
			url = new URL(href);
		} catch (MalformedURLException e1) {
			String message = "Problem while trying to fetch url for thumb : "
					+ href;
			logger.error(message);
			return new Point();
		}
		BufferedImage image;
		try {
			image = ImageIO.read(url);
		} catch (Exception e) {

			e.printStackTrace();
			String message = String.format(
					"We were not able to process this image : %s", href);
			logger.warn("Problem while trying to fetch url for thumb : "
					+ message);
			throw new InvalidImageException(message);
		}
		if (image == null)
			return new Point();
		return new Point(image.getWidth(), image.getHeight());
	}

	private boolean imageSizeIsSufficient(Point imageSize,
			double minDimensionSum) throws InvalidImageException {
		double width = imageSize.getX();
		double height = imageSize.getY();
		return width + height > minDimensionSum;
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());
	}

	@Override
	public Map<String, Point> getThumbsFromPreview(String resourceId,
			String previewsRepoPath, String baseUr) {
		return Collections.emptyMap();
	}
}
