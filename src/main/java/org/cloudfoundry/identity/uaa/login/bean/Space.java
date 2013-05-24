package org.cloudfoundry.identity.uaa.login.bean;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Space {
	private String name;
	private String organization_guid;
	private String[] developer_guids;
	private String[] manager_guids;
	private String[] auditor_guids;
	private String[] app_guids;
	private String[] domain_guids;
	private String[] service_instance_guids;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrganization_guid() {
		return organization_guid;
	}

	public void setOrganization_guid(String organization_guid) {
		this.organization_guid = organization_guid;
	}

	public String[] getDeveloper_guids() {
		return developer_guids;
	}

	public void setDeveloper_guids(String[] developer_guids) {
		this.developer_guids = developer_guids;
	}

	public String[] getManager_guids() {
		return manager_guids;
	}

	public void setManager_guids(String[] manager_guids) {
		this.manager_guids = manager_guids;
	}

	public String[] getAuditor_guids() {
		return auditor_guids;
	}

	public void setAuditor_guids(String[] auditor_guids) {
		this.auditor_guids = auditor_guids;
	}

	public String[] getApp_guids() {
		return app_guids;
	}

	public void setApp_guids(String[] app_guids) {
		this.app_guids = app_guids;
	}

	public String[] getDomain_guids() {
		return domain_guids;
	}

	public void setDomain_guids(String[] domain_guids) {
		this.domain_guids = domain_guids;
	}

	public String[] getService_instance_guids() {
		return service_instance_guids;
	}

	public void setService_instance_guids(String[] service_instance_guids) {
		this.service_instance_guids = service_instance_guids;
	}

}
