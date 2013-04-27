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

	private static final Object TARGET_GROUP = "cloud_controller.admin";

	private final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;

	private RestTemplate scimTemplate;

	private static String DEFAULT_LOGIN_URL = "http://uaa.cloudfoundry.com/authenticate";

	private String loginUrl = DEFAULT_LOGIN_URL;

	private String baseUrl;

	// private final AuthenticationManager delegate;

	private LdapAuthHelper ldapAuthHelper;

	public RemoteUaaAuthenticationManager(LdapAuthHelper ldapAuthHelper,
			RestTemplate scimTemplate) {
		super();
		this.ldapAuthHelper = ldapAuthHelper;
		this.scimTemplate = scimTemplate;

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
	 * @param loginUrl
	 *            the login url to set
	 */
	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
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
		logger.debug("---------------------------------------------------------------");

		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		logger.debug("1. Get username and password " + username + "/"
				+ password);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.set("username", username);
		parameters.set("password", password);

		logger.debug("2. Start auth to uaa");

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.exchange(loginUrl,
				HttpMethod.POST, new HttpEntity<MultiValueMap<String, Object>>(
						parameters, headers), Map.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			logger.info("3. Successful authentication request for " + username);
			return new UsernamePasswordAuthenticationToken(username, null,
					UaaAuthority.USER_AUTHORITIES);

		} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			logger.debug("3. Uaa auth failed. May first login or user error, try to login to LDAP to check username and password");

			boolean ldapAuthResult = ldapAuthHelper.authenticate(username,
					password);

			if (!ldapAuthResult) {
				logger.debug("4. Ldap auth failed with " + username + ", "
						+ password);
				throw new BadCredentialsException("LDAP authentication failed");
			}

			logger.debug("4. Ldap auth successfully. So create the cf account via scim");

			ScimUser user = new ScimUser();
			user.setUserName(username);
			user.setName(new ScimUser.Name(username, " "));
			user.addEmail(username);
			user.setPassword(password);
			user.setUserType(UaaAuthority.UAA_NONE.getUserType());
			user.setActive(true);
			ScimMeta meta = new ScimMeta();
			Date now = new Date();
			meta.setCreated(now);
			meta.setLastModified(now);
			user.setMeta(meta);
			user.setGroups(Arrays.asList(new Group(null, "uaa.none")));
			user.setApprovals(new HashSet<Approval>());
			user.addPhoneNumber("123456789");
			user.setDisplayName(username);
			user.setSchemas(new String[] { "urn:scim:schemas:core:1.0" });

			logger.debug("5. Start to create ScimUser");

			ResponseEntity<ScimUser> userResponse = scimTemplate.postForEntity(
					baseUrl + "Users", user, ScimUser.class);
			user = userResponse.getBody();

			logger.debug("6. Get all ScimGroups");
			ResponseEntity<SearchResults> groupsResult = scimTemplate
					.getForEntity(baseUrl + "Groups", SearchResults.class);
			SearchResults groups = groupsResult.getBody();
			ScimGroup ccGroup = null;
			List<Map<String, Object>> list = (List<Map<String, Object>>) groups
					.getResources();

			for (Map<String, Object> map : list) {
				logger.debug("-----");
				for (String key : map.keySet()) {
					ScimGroup group = (ScimGroup) map.get(key);
					logger.debug(key + " : " + group.getDisplayName());
					if (group.getDisplayName().equals(TARGET_GROUP)) {
						ccGroup = group;
					}
				}

			}

			if (ccGroup == null) {
				logger.debug("7. Cannot get group cc admin");
				throw new RuntimeException(
						"Could not authenticate with remote server");
			}

			ccGroup.getMembers().add(new ScimGroupMember(user.getId()));

			logger.debug("6.2 Update Group information");

			scimTemplate.put(baseUrl + "Groups/" + ccGroup.getId(), ccGroup);

			logger.debug("7. Re-login to UAA via rest");

			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> newResponse = restTemplate.exchange(loginUrl,
					HttpMethod.POST,
					new HttpEntity<MultiValueMap<String, Object>>(parameters,
							headers), Map.class);
			if (newResponse.getStatusCode() == HttpStatus.OK) {
				logger.debug("8. Successful authentication request for "
						+ username);
				Authentication authResult = new UsernamePasswordAuthenticationToken(
						username, null, UaaAuthority.USER_AUTHORITIES);
				return authResult;

			} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				logger.debug("8. Unauthorited via new user. This should not happen.");
				throw new BadCredentialsException("Authentication failed");
			} else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
				logger.info("8. Internal error from UAA. Please Check the UAA logs.");
			} else {
				logger.error("8. Unexpected status code "
						+ response.getStatusCode() + " from the UAA."
						+ " Is a compatible version running?");
			}
			throw new RuntimeException(
					"Could not authenticate with remote server");
		} else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			logger.info("3. Internal error from UAA. Please Check the UAA logs.");
		} else {
			logger.error("3. Unexpected status code "
					+ response.getStatusCode() + " from the UAA."
					+ " Is a compatible version running?");
		}
		throw new RuntimeException("Could not authenticate with remote server");
	}
}
