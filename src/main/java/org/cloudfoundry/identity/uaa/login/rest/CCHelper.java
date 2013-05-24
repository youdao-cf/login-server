package org.cloudfoundry.identity.uaa.login.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.login.bean.CCUser;
import org.cloudfoundry.identity.uaa.login.bean.Domain;
import org.cloudfoundry.identity.uaa.login.bean.Organization;
import org.cloudfoundry.identity.uaa.login.bean.Space;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class CCHelper {
	private final Log logger = LogFactory.getLog(getClass());
	private static final String ORG_PATH = "/v2/organizations";
	private static final String USER_PATH = "/v2/users";
	private static final String SPACE_PATH = "/v2/spaces";
	private static final String DOMAIN_PATH = "/v2/domains";
	private static final String SPACE_DEFAULT_NAME = "default";
	private static final String DOMAIN_SUFFIX = ".cf2.youdao.com";
	private RestTemplate restTemplate;
	private String baseUrl;
	private String orgUrl;
	private String userUrl;
	private String spaceUrl;
	private String domainUrl;

	public CCHelper(RestTemplate restTemplate, String baseUrl) {
		super();
		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
		this.orgUrl = this.baseUrl + ORG_PATH;
		this.userUrl = this.baseUrl + USER_PATH;
		this.spaceUrl = this.baseUrl + SPACE_PATH;
		this.domainUrl = this.baseUrl + DOMAIN_PATH;
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

	public boolean addUserToOrgAndSpace(String name, String user_guid) {
		if (name == null || user_guid == null) {
			logger.error("Cannot create organiztion and default space for user");
			return false;
		}

		String org_guid = addUserToOrg(name, user_guid);

		if (org_guid == null) {
			logger.error("Cannot create Org");
			return false;
		}

		String space_guid = addUserToDefaultSpace(org_guid, user_guid);

		if (space_guid == null) {
			logger.error("Cannot create Space");
			return false;
		}

		String domain_guid = addOrgAndSpaceToDomain(name, org_guid, space_guid);

		if (domain_guid == null) {
			logger.error("Cannot create Domain");
			return false;
		}
		
		logger.debug("==============================");
		logger.debug("Organization: " + org_guid);
		logger.debug("Space: " + space_guid);
		logger.debug("Domain: " + domain_guid);
		logger.debug("==============================");

		return true;
	}

	private String addOrgAndSpaceToDomain(String orgName, String org_guid,
			String space_guid) {
		logger.debug("----------CREATE CC DOMAIN -----------");
		Domain domain = new Domain();
		domain.setName(orgName + DOMAIN_SUFFIX);
		domain.setOwning_organization_id(org_guid);
		domain.setOrganization_guids(new String[] { org_guid });
		domain.setSpace_guids(new String[] { space_guid });
		domain.setWildcard(true);

		try {
			return createDomain(domain);
		} finally {
			logger.debug("-------------------------------------");
		}
	}

	private String addUserToOrg(String orgName, String guid) {
		logger.debug("----------CREATE CC ORGANIZATION -----------");
		Organization org = new Organization();
		org.setName(orgName);
		org.setManager_guids(new String[] { guid });
		org.setUser_guids(new String[] { guid });
		try {
			return createOrg(org);
		} finally {
			logger.debug("-------------------------------------");
		}

	}

	private String addUserToDefaultSpace(String organization_guid,
			String user_guid) {
		logger.debug("----------  CREATE CC SPACE   -----------");
		Space space = new Space();
		space.setName(SPACE_DEFAULT_NAME);
		space.setOrganization_guid(organization_guid);
		space.setManager_guids(new String[] { user_guid });
		space.setDeveloper_guids(new String[] { user_guid });

		try {
			return createSpace(space);
		} finally {
			logger.debug("-------------------------------------");
		}
	}

	private String createOrg(Organization org) {
		ResponseEntity response = restTemplate.postForEntity(orgUrl, org,
				Object.class);
		String res = response.getBody().toString();
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(res);

		if (response.getStatusCode() == HttpStatus.CREATED) {
			logger.info("create org successfully");
			return fetchGuidFromResponse(res);
		} else if (response.getStatusCode() == HttpStatus.CONFLICT) {
			logger.info("org " + org.getName() + " exists now");
		} else {
			logger.info("create org failed");
		}
		return null;
	}

	private String createSpace(Space space) {
		ResponseEntity response = restTemplate.postForEntity(spaceUrl, space,
				Object.class);
		String res = response.getBody().toString();
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(res);

		if (response.getStatusCode() == HttpStatus.CREATED) {
			logger.info("create space successfully");
			return fetchGuidFromResponse(res);
		} else if (response.getStatusCode() == HttpStatus.CONFLICT) {
			logger.info("space " + space.getName() + " exists now");
		} else {
			logger.info("create space failed");
		}
		return null;
	}

	private String createDomain(Domain domain) {
		ResponseEntity response = restTemplate.postForEntity(domainUrl, domain,
				Object.class);
		String res = response.getBody().toString();
		logger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		logger.info(res);

		if (response.getStatusCode() == HttpStatus.CREATED) {
			logger.info("create domain successfully");
			return fetchGuidFromResponse(res);
		} else if (response.getStatusCode() == HttpStatus.CONFLICT) {
			logger.info("domain " + domain.getName() + " exists now");
		} else {
			logger.info("create domain failed");
		}
		return null;
	}

	private String fetchGuidFromResponse(String res) {
		return res != null ? res.substring(res.indexOf("guid=") + 5,
				res.indexOf(", url")) : null;
	}
}
