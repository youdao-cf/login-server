package org.cloudfoundry.identity.uaa.login.rest;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.login.bean.MetaData;
import org.cloudfoundry.identity.uaa.login.bean.Organization;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class OrganizationHelper {
	private final Log logger = LogFactory.getLog(getClass());
	private static final String ORG_PATH = "/v2/organizations";
	private RestTemplate restTemplate;
	private String baseUrl;
	private String orgUrl;

	public OrganizationHelper(RestTemplate restTemplate, String baseUrl) {
		super();
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.orgUrl = this.baseUrl + ORG_PATH;
	}

	public boolean addUserToOrg(ScimUser user) {
		if (user == null || user.getId() == null) {
			logger.error("Cannot create organiztion for user");
			return false;
		}
		Organization org = new Organization();
		org.setName(user.getDisplayName() + "'s organization");
		org.setManager_guids(new String[] { user.getId() });
		org.setUser_guids(new String[] { user.getId() });
		if (!createOrg(org)) {
			return false;
		}
		return true;

	}

	private boolean createOrg(Organization org) {
		ResponseEntity response = restTemplate.postForEntity(orgUrl, org,
				List.class);
		String res = response.getBody().toString();
		String all = response.toString();
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(res);
		logger.info("<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(all);
		try {
			List<Map<String, Object>> resOrg = (List<Map<String, Object>>) response
					.getBody();
			for (Map<String, Object> map : resOrg) {
				MetaData meta = (MetaData) map.get("metadata");
				Organization orga = (Organization) map.get("entity");
				logger.info(meta.getGuid() + " : " + orga.getName());
			}
		} catch (Exception e) {
			logger.error("convert failed", e);
		}
		if (response.getStatusCode() == HttpStatus.OK) {
			logger.info("create org successfully");
			return true;
		} else {
			logger.info("create org failed");
			return false;
		}
	}

}
