package org.cloudfoundry.identity.uaa.login.bean;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Domain {
	private String name;
	private boolean wildcard;
	private String owning_organization_guid;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isWildcard() {
		return wildcard;
	}

	public void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}

	public String getOwning_organization_guid() {
		return owning_organization_guid;
	}

	public void setOwning_organization_guid(String owning_organization_guid) {
		this.owning_organization_guid = owning_organization_guid;
	}


}
