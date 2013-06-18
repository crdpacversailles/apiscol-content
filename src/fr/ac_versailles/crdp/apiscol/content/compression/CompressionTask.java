package fr.ac_versailles.crdp.apiscol.content.compression;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fr.ac_versailles.crdp.apiscol.content.AsyncProcessTrackingObject;
import fr.ac_versailles.crdp.apiscol.content.IRefreshProcess;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceSnapshotDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class CompressionTask implements IRefreshProcess {

	private final String resourceId;
	private String snapshotId;
	private Logger logger;
	private AsyncProcessTrackingObject trackingObject;

	public CompressionTask(String resourceId, Document manifestFile)
			throws IOException, ResourceDirectoryNotFoundException {
		createLogger();
		this.resourceId = resourceId;
		this.snapshotId = String.format("%s-%s",
				Long.toString(System.currentTimeMillis() / 1000L),
				Integer.toString((int) (Math.random() * 1000)));
		ResourceDirectoryInterface.createSnapShot(resourceId, snapshotId,
				manifestFile);
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

	@Override
	public void run() {
		trackingObject.updateStateAndMessage(States.pending,
				"The IMS-CP archive is going to be rebuilt.");
		logger.info(String.format(
				"Beginning the compression of snapshot %s for resource %s",
				snapshotId, resourceId));
		try {
			ResourceDirectoryInterface.zipSnapshot(resourceId, snapshotId);
		} catch (IOException e) {
			logger.error(String
					.format("Probleme of file system access, stopping the compression of snapshot %s for resource %s",
							snapshotId, resourceId));
		} catch (ResourceSnapshotDirectoryNotFoundException e) {
			logger.error(String
					.format("Impossible to compress snapshot %s for resource %s, directory not found",
							snapshotId, resourceId));

		}
		trackingObject.updateStateAndMessage(States.pending,
				"The temporary zip has been successfully created.");
		ResourceDirectoryInterface.commitZip(resourceId, snapshotId);
		try {
			ResourceDirectoryInterface.deleteSnapshot(resourceId, snapshotId);
		} catch (ResourceSnapshotDirectoryNotFoundException e) {
			logger.error(String
					.format("Impossible to delete snapshot %s for resource %s, directory not found",
							snapshotId, resourceId));
		}
		logger.info(String.format(
				"Ending the compression of snapshot %s for resource %s",
				snapshotId, resourceId));
		trackingObject
				.updateStateAndMessage(
						States.done,
						String.format(
								"The new archive %s has been successfully created for resource %s",
								snapshotId, resourceId));
	}

	@Override
	public void setTrackingObject(AsyncProcessTrackingObject trackingObject) {
		this.trackingObject = trackingObject;

	}

}
