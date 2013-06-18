package fr.ac_versailles.crdp.apiscol.content.previews;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;

public class UntypedPreviewMaker extends AbstractPreviewMaker implements
		IPreviewMaker {

	public UntypedPreviewMaker(String resourceId, String previewsRepoPath,
			String entryPoint, String realPath, String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, realPath, previewUri);
	}

	@Override
	protected void createPreviewDirectory() {
		// does Nothing
	}

	@Override
	protected void createNewPreview() {
		trackingObject
		.updateStateAndMessage(States.aborted,
				"Impossible to build preview for this file : " + entryPoint
						+ ". No preview maker registered for his mime type.");
	}

}
