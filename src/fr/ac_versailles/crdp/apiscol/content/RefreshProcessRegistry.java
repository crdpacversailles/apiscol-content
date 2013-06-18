package fr.ac_versailles.crdp.apiscol.content;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;

public class RefreshProcessRegistry {

	public enum States {
		initiated("initiated"), aborted("aborted"), pending("pending"), unknown(
				"unknown"), done("done");
		private String value;

		private States(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	private static Integer counter;
	private static Map<Integer, AsyncProcessTrackingObject> processes = new ConcurrentHashMap<Integer, AsyncProcessTrackingObject>();

	public RefreshProcessRegistry() {
		counter = 0;
	}

	public States getTransferState(Integer refreshProcessIdentifier) {
		if (!processes.containsKey(refreshProcessIdentifier))
			return States.unknown;
		return processes.get(refreshProcessIdentifier).getState();
	}

	public String getMessage(Integer transferIdentifier) {
		if (!processes.containsKey(transferIdentifier))
			return "This transfer operation is missing from system memory.";
		return processes.get(transferIdentifier).getMessage();
	}

	public Integer register(IRefreshProcess task, String resourceId) {
		counter++;
		System.out.println("on attribue le counter " + counter);
		String message = "The process has been initiated and is going to begin as soon as possible";
		AsyncProcessTrackingObject trackingObject = new AsyncProcessTrackingObject(
				counter, task, resourceId);
		trackingObject.setMessage(message);
		trackingObject.setState(States.initiated);
		processes.put(counter, trackingObject);
		task.setTrackingObject(trackingObject);
		return counter;
	}

	public void updateState(Integer identifier, States state, String message) {
		processes.get(identifier).setMessage(message);
		processes.get(identifier).setState(state);
	}

	public String getResourceIdForTransfer(Integer identifier) {
		return processes.get(identifier).getResourceId();
	}

}
