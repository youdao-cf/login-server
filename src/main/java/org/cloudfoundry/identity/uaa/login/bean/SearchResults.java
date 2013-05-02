package org.cloudfoundry.identity.uaa.login.bean;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Dave Syer
 * 
 */
public class SearchResults<T> {

	private Collection<T> resources;
	private int startIndex;
	private int itemsPerPage;
	private int totalResults;
	private Collection<String> schemas;

	public SearchResults(Collection<String> schemas, Collection<T> resources,
			int startIndex, int itemsPerPage, int totalResults) {
		this.schemas = new ArrayList<String>(schemas);
		this.resources = new ArrayList<T>(resources);
		this.startIndex = startIndex;
		this.itemsPerPage = itemsPerPage;
		this.totalResults = totalResults;
	}

	public SearchResults() {
		this.schemas = new ArrayList<String>();
		this.resources = new ArrayList<T>();
		startIndex = 0;
		itemsPerPage = 0;
		totalResults = 0;
	}

	public Collection<String> getSchemas() {
		return schemas;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getItemsPerPage() {
		return itemsPerPage;
	}

	public int getTotalResults() {
		return totalResults;
	}

	public Collection<T> getResources() {
		return resources;
	}

	public void setResources(Collection<T> resources) {
		this.resources = resources;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public void setItemsPerPage(int itemsPerPage) {
		this.itemsPerPage = itemsPerPage;
	}

	public void setTotalResults(int totalResults) {
		this.totalResults = totalResults;
	}

	public void setSchemas(Collection<String> schemas) {
		this.schemas = schemas;
	}

}
