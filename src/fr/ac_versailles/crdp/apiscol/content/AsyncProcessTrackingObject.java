package fr.ac_versailles.crdp.apiscol.content;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;

public class AsyncProcessTrackingObject {

	private String message;
	private States state;
	private final Integer identifier;
	private final IRefreshProcess task;
	private final String resourceId;

	public AsyncProcessTrackingObject(Integer counter, IRefreshProcess task,
			String resourceId) {
		this.identifier = counter;
		this.task = task;
		this.resourceId = resourceId;
		this.state=States.unknown;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public States getState() {
		return state;
	}

	public void setState(States state) {
		this.state = state;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void updateStateAndMessage(States state, String message) {
		setState(state);
		setMessage(message);

	}

}
