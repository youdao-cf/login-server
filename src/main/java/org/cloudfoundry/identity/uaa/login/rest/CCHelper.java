package org.cloudfoundry.identity.uaa.login.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.login.bean.CCUser;
import org.cloudfoundry.identity.uaa.login.bean.Organization;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CCHelper {
	private final Log logger = LogFactory.getLog(getClass());
	private static final String ORG_PATH = "/v2/organizations";
	private static final String USER_PATH = "/v2/users";
	private RestTemplate restTemplate;
	private String baseUrl;
	private String orgUrl;
	private String userUrl;

	public CCHelper(RestTemplate restTemplate, String baseUrl) {
		super();
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.orgUrl = this.baseUrl + ORG_PATH;
		this.userUrl = this.baseUrl + USER_PATH;
	}

	public boolean addCCUser(String guid) {
		logger.debug("----------CREATE CC USER -----------");
		CCUser ccUser = new CCUser();
		ccUser.setGuid(guid);
		ccUser.setAdmin(false);
		try {
			restTemplate.postForEntity(userUrl, ccUser, Object.class);
		} catch (Exception e) {
			return false;
		}
		logger.debug("-------------------------------------");
		return true;
	}

	public boolean addUserToOrg(String orgName, String guid) {
		if (orgName == null || guid == null) {
			logger.error("Cannot create organiztion for user");
			return false;
		}
		logger.debug("----------CREATE CC ORGANIZATION -----------");
		Organization org = new Organization();
		org.setName(orgName);
		org.setManager_guids(new String[] { guid });
		org.setUser_guids(new String[] { guid });
		try {
			if (!createOrg(org)) {
				return false;
			}
			return true;
		} finally {
			logger.debug("-------------------------------------");
		}

	}

	private boolean createOrg(Organization org) {
		ResponseEntity response = restTemplate.postForEntity(orgUrl, org,
				Object.class);
		String res = response.getBody().toString();
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(res);
		
		if (response.getStatusCode() == HttpStatus.OK) {
			logger.info("create org successfully");
			return true;
		} else {
			logger.info("create org failed");
			return false;
		}
	}

}
