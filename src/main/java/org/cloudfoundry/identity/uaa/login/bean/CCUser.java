package org.cloudfoundry.identity.uaa.login.bean;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class CCUser {
	private String guid;
	private boolean admin;
	private String default_space_guid;
	private String[] space_guids;
	private String[] organization_guids;
	private String[] managed_organization_guids;
	private String[] billing_managed_organization_guids;
	private String[] audited_organization_guids;
	private String[] managed_space_guids;
	private String[] audited_space_guids;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getDefault_space_guid() {
		return default_space_guid;
	}

	public void setDefault_space_guid(String default_space_guid) {
		this.default_space_guid = default_space_guid;
	}

	public String[] getSpace_guids() {
		return space_guids;
	}

	public void setSpace_guids(String[] space_guids) {
		this.space_guids = space_guids;
	}

	public String[] getOrganization_guids() {
		return organization_guids;
	}

	public void setOrganization_guids(String[] organization_guids) {
		this.organization_guids = organization_guids;
	}

	public String[] getManaged_organization_guids() {
		return managed_organization_guids;
	}

	public void setManaged_organization_guids(
			String[] managed_organization_guids) {
		this.managed_organization_guids = managed_organization_guids;
	}

	public String[] getBilling_managed_organization_guids() {
		return billing_managed_organization_guids;
	}

	public void setBilling_managed_organization_guids(
			String[] billing_managed_organization_guids) {
		this.billing_managed_organization_guids = billing_managed_organization_guids;
	}

	public String[] getAudited_organization_guids() {
		return audited_organization_guids;
	}

	public void setAudited_organization_guids(
			String[] audited_organization_guids) {
		this.audited_organization_guids = audited_organization_guids;
	}

	public String[] getManaged_space_guids() {
		return managed_space_guids;
	}

	public void setManaged_space_guids(String[] managed_space_guids) {
		this.managed_space_guids = managed_space_guids;
	}

	public String[] getAudited_space_guids() {
		return audited_space_guids;
	}

	public void setAudited_space_guids(String[] audited_space_guids) {
		this.audited_space_guids = audited_space_guids;
	}

}
