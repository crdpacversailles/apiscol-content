package fr.ac_versailles.crdp.apiscol.content.crawler;

import fr.ac_versailles.crdp.apiscol.content.crawler.LinkRefresher.Terminations;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;

public class LinkRefreshingHandler {
	public enum State {
		INACTIVE, RUNNING
	}

	private LinkRefreshingHandler() {

	}

	private State currentState = State.INACTIVE;
	private LinkRefresher worker;
	private static LinkRefreshingHandler instance;
	private Terminations lastProcessTermination = Terminations.NONE;
	private String currentlyParsedUrl = "";
	private int lastProcessNumberOfErrors;

	public State getCurrentState() {
		return currentState;
	}

	public void startUdateProcess(
			ISearchEngineQueryHandler searchEngineQueryHandler,
			IResourceDataHandler resourceDataHandler) {
		currentState = State.RUNNING;
		worker = new LinkRefresher(searchEngineQueryHandler,
				resourceDataHandler, this);
		Thread refreshProcess = new Thread(worker);
		refreshProcess.start();
	}

	public static LinkRefreshingHandler getInstance() {
		if (instance == null)
			instance = new LinkRefreshingHandler();
		return instance;
	}

	public void notifyRefreshingProcessTermination(Terminations termination,
			int nbErrors) {
		this.lastProcessTermination = termination;
		this.lastProcessNumberOfErrors=nbErrors;
		currentState = State.INACTIVE;

	}

	public Terminations getLastProcessTermination() {
		return lastProcessTermination;
	}

	public String getCurrentlyParsedUrl() {
		return currentlyParsedUrl;
	}

	public void setCurrentlyParsedUrl(String currentlyParsedUrl) {
		this.currentlyParsedUrl = currentlyParsedUrl;
	}

	public int getLastProcessNumberOfErrors() {
		return lastProcessNumberOfErrors;
	}

}
