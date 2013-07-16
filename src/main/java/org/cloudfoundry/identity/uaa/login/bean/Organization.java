package org.cloudfoundry.identity.uaa.login.bean;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Organization {
	private String name;
	private boolean billing_enabled;
	private String quota_definition_guid;
	private String[] domain_guids;
	private String[] user_guids;
	private String[] manager_guids;
	private String[] billing_manager_guids;
	private String[] auditor_guids;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isBilling_enabled() {
		return billing_enabled;
	}

	public void setBilling_enabled(boolean billing_enabled) {
		this.billing_enabled = billing_enabled;
	}

	public String getQuota_definition_guid() {
		return quota_definition_guid;
	}

	public void setQuota_definition_guid(String quota_definition_guid) {
		this.quota_definition_guid = quota_definition_guid;
	}

	public String[] getDomain_guids() {
		return domain_guids;
	}

	public void setDomain_guids(String[] domain_guids) {
		this.domain_guids = domain_guids;
	}

	public String[] getUser_guids() {
		return user_guids;
	}

	public void setUser_guids(String[] user_guids) {
		this.user_guids = user_guids;
	}

	public String[] getManager_guids() {
		return manager_guids;
	}

	public void setManager_guids(String[] manager_guids) {
		this.manager_guids = manager_guids;
	}

	public String[] getBilling_manager_guids() {
		return billing_manager_guids;
	}

	public void setBilling_manager_guids(String[] billing_manager_guids) {
		this.billing_manager_guids = billing_manager_guids;
	}

	public String[] getAuditor_guids() {
		return auditor_guids;
	}

	public void setAuditor_guids(String[] auditor_guids) {
		this.auditor_guids = auditor_guids;
	}

}
