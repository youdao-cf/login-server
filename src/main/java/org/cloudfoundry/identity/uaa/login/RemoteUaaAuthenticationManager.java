/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.cloudfoundry.identity.uaa.login;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.login.bean.CCUser;
import org.cloudfoundry.identity.uaa.login.bean.SearchResults;
import org.cloudfoundry.identity.uaa.login.rest.CustomObjectMapper;
import org.cloudfoundry.identity.uaa.login.rest.LdapAuthHelper;
import org.cloudfoundry.identity.uaa.login.rest.CCHelper;
import org.cloudfoundry.identity.uaa.oauth.approval.Approval;
import org.cloudfoundry.identity.uaa.scim.ScimGroup;
import org.cloudfoundry.identity.uaa.scim.ScimGroupMember;
import org.cloudfoundry.identity.uaa.scim.ScimMeta;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.cloudfoundry.identity.uaa.scim.ScimUser.Group;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * An authentication manager that can be used to login to a remote UAA service
 * with username and password credentials, without the local server needing to
 * know anything about the user accounts. The request is handled by the UAA's
 * RemoteAuhenticationEndpoint and success or failure is determined by the
 * response code.
 * 
 * @author Dave Syer
 * @author Luke Taylor
 * 
 */
public class RemoteUaaAuthenticationManager implements AuthenticationManager {

	private static final String TARGET_GROUP = "cloud_controller.write";

	private static final String[] SCIM_SCHEMAS = new String[] { "urn:scim:schemas:core:1.0" };

	private static final String DEFAULT_PASSWORD = "";

	private static final String DEFAULT_BASE_URL = "http://localhost:8080/uaa";

	private final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;

	private RestTemplate scimTemplate;

	private String baseUrl = DEFAULT_BASE_URL;

	private LdapAuthHelper ldapAuthHelper;

	private CCHelper ccHelper;

	public RemoteUaaAuthenticationManager(LdapAuthHelper ldapAuthHelper,
			RestTemplate scimTemplate, CCHelper ccHelper) {
		super();
		this.ldapAuthHelper = ldapAuthHelper;
		this.scimTemplate = scimTemplate;
		this.ccHelper = ccHelper;

		restTemplate = new RestTemplate();

		List<HttpMessageConverter<?>> list = scimTemplate
				.getMessageConverters();
		list.remove(list.size() - 1);
		MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
		jsonConverter.setObjectMapper(new CustomObjectMapper());
		list.add(jsonConverter);

		scimTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});

		restTemplate
				.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});
	}

	/**
	 * @param restTemplate
	 *            a rest template to use
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {
		if (authentication == null) {
			logger.warn("Authentication is null, please check your input");
			return null;
		}

		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		logger.debug("Get username and password " + username + "/" + password);

		logger.debug("LDAP verifying....");

		boolean ldapAuthResult = ldapAuthHelper
				.authenticate(username, password);

		if (!ldapAuthResult) {
			logger.debug("Ldap auth failed with " + username + ", " + password);
			throw new BadCredentialsException("LDAP authentication failed");
		}
		logger.debug("LDAP auth passed");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.set("username", username);
		parameters.set("password", DEFAULT_PASSWORD);

		logger.debug("UAA verifying.....");

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl
				+ "/authenticate", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, Object>>(parameters,
						headers), Map.class);

		if (response == null) {
			logger.debug("UAA auth failed and response is null. Please Check the UAA logs.");
			throw new RuntimeException(
					"Could not authenticate with remote server");
		}

		if (response.getStatusCode() == HttpStatus.OK) {
			logger.debug("Successful authentication request for " + username);
			return new UsernamePasswordAuthenticationToken(username, null,
					UaaAuthority.USER_AUTHORITIES);

		} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			logger.debug("Uaa auth failed. It may be first login, so try to create the cf account via scim");

			ScimUser user = new ScimUser();
			user.setUserName(username);
			user.setName(new ScimUser.Name(username, " "));
			user.addEmail(username);
			user.setPassword(DEFAULT_PASSWORD);
			user.setUserType(UaaAuthority.UAA_NONE.getUserType());
			user.setActive(true);
			ScimMeta meta = new ScimMeta();
			Date now = new Date();
			meta.setCreated(now);
			meta.setLastModified(now);
			user.setMeta(meta);
			user.setGroups(Arrays.asList(new Group(null, "uaa.none")));
			user.setApprovals(new HashSet<Approval>());
			user.addPhoneNumber("0");
			user.setDisplayName(username);
			user.setSchemas(SCIM_SCHEMAS);

			logger.debug("Creating User.....");

			ResponseEntity<ScimUser> userResponse = scimTemplate.postForEntity(
					baseUrl + "/Users", user, ScimUser.class);
			user = userResponse.getBody();

			logger.debug("Created User with username " + username);

			logger.debug("Getting all ScimGroups.....");
			ResponseEntity<SearchResults> groupsResult = scimTemplate
					.getForEntity(baseUrl + "/Groups", SearchResults.class);
			SearchResults groups = groupsResult.getBody();
			String ccGroupId = null;
			ScimGroup ccGroup = null;
			List<Map<String, Object>> list = (List<Map<String, Object>>) groups
					.getResources();

			logger.debug("Retriving cloud_controller group id.....");
			for (Map<String, Object> map : list) {
				if (TARGET_GROUP.equals(map.get("displayName").toString()
						.trim())) {
					ccGroupId = (String) map.get("id");
					break;
				}
			}

			if (ccGroupId == null) {
				logger.debug("Cannot get CC group id");
				throw new RuntimeException(
						"Could not authenticate with remote server");
			}

			logger.debug("Getting ScimGroup with id:" + ccGroupId + " ......");

			ResponseEntity<ScimGroup> group = scimTemplate.getForEntity(baseUrl
					+ "/Groups/" + ccGroupId, ScimGroup.class);
			ccGroup = group.getBody();

			if (ccGroup == null) {
				logger.debug("Cannot get Group CC ");
				throw new RuntimeException(
						"Could not authenticate with remote server");
			}

			ccGroup.getMembers().add(new ScimGroupMember(user.getId()));

			logger.debug("Add " + username + " to CC group");

			HttpHeaders groupHeaders = new HttpHeaders();
			groupHeaders.add("If-Match", "*");
			groupHeaders.setContentType(MediaType.APPLICATION_JSON);
			groupHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

			scimTemplate.exchange(baseUrl + "/Groups/" + ccGroupId,
					HttpMethod.PUT, new HttpEntity<ScimGroup>(ccGroup,
							groupHeaders), ScimGroup.class);

			ccHelper.addUserToOrg(username, user.getId());
			
			logger.debug("Relogin to UAA via rest");

			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> newResponse = restTemplate.exchange(baseUrl
					+ "/authenticate", HttpMethod.POST,
					new HttpEntity<MultiValueMap<String, Object>>(parameters,
							headers), Map.class);
			if (newResponse.getStatusCode() == HttpStatus.OK) {
				logger.debug("Successful authentication request for "
						+ username);
				Authentication authResult = new UsernamePasswordAuthenticationToken(
						username, null, UaaAuthority.USER_AUTHORITIES);
				return authResult;

			} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				logger.debug("Unauthorited via new user. This should not happen.");
				throw new BadCredentialsException("Authentication failed");
			} else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
				logger.info("Internal error from UAA. Please Check the UAA logs.");
			} else {
				logger.error("Unexpected status code "
						+ response.getStatusCode() + " from the UAA."
						+ " Is a compatible version running?");
			}
			throw new RuntimeException(
					"Could not authenticate with remote server");
		} else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			logger.info("Internal error from UAA. Please Check the UAA logs.");
		} else {
			logger.error("Unexpected status code " + response.getStatusCode()
					+ " from the UAA." + " Is a compatible version running?");
		}
		throw new RuntimeException("Could not authenticate with remote server");
	}
}
