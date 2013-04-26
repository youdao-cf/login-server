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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.message.PasswordChangeRequest;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
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

	private final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;

	private static String DEFAULT_LOGIN_URL = "http://uaa.cloudfoundry.com/authenticate";

	private static String DEFAULT_SCIM_URL = "http://uaa.cloudfoundry.com/Users";

	private String loginUrl = DEFAULT_LOGIN_URL;

	private String scimUrl = DEFAULT_SCIM_URL;

	// private final AuthenticationManager delegate;

	private LdapAuthHelper ldapAuthHelper;

	public RemoteUaaAuthenticationManager(LdapAuthHelper ldapAuthHelper, RestTemplate restTemplate) {
		super();
		this.ldapAuthHelper = ldapAuthHelper;
		this.restTemplate = restTemplate;
//		RestTemplate restTemplate = new RestTemplate();
//		// The default java.net client doesn't allow you to handle 4xx responses
		List<HttpMessageConverter<?>> list = new ArrayList<HttpMessageConverter<?>>();
		list.add(new MappingJackson2HttpMessageConverter());
		restTemplate.setMessageConverters(list);
		
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

	public void setScimUrl(String scimUrl) {
		this.scimUrl = scimUrl;
	}

	@Override
	public Authentication authenticate(Authentication authentication)
			throws AuthenticationException {

		// if (authentication == null
		// || authentication instanceof UsernamePasswordAuthenticationToken) {
		// return authentication;
		// }
		//
		// UsernamePasswordAuthenticationToken output = new
		// UsernamePasswordAuthenticationToken(
		// authentication, authentication.getCredentials(),
		// authentication.getAuthorities());
		// output.setAuthenticated(authentication.isAuthenticated());
		// output.setDetails(authentication.getDetails());
		//
		// Authentication ldapResult = null;
		// try {
		// ldapResult = delegate.authenticate(output);
		// } catch (AuthenticationException e) {
		// throw new BadCredentialsException("LDAP authentication failed");
		// }

		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		boolean ldapAuthResult = ldapAuthHelper
				.authenticate(username, password);

		if (!ldapAuthResult) {
			throw new BadCredentialsException("LDAP authentication failed");
		}

		logger.debug("Ldap auth successfuly with " + username + ", " + password);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.set("username", username);
		parameters.set("password", password);

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = restTemplate.exchange(loginUrl,
				HttpMethod.POST, new HttpEntity<MultiValueMap<String, Object>>(
						parameters, headers), Map.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			String userFromUaa = (String) response.getBody().get("username");
			if (userFromUaa.equals(userFromUaa)) {
				logger.info("Successful authentication request for " + username);
				return new UsernamePasswordAuthenticationToken(username, null,
						UaaAuthority.USER_AUTHORITIES);
			}
		} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			logger.info("Cannot authenticate for LDAP is not created, and create it now");

//			logger.info("Login as admin");
//			parameters.set("username", "admin3");
//			parameters.set("password", "123");
//			@SuppressWarnings("rawtypes")
//			ResponseEntity<Map> adminResponse = restTemplate.exchange(loginUrl,
//					HttpMethod.POST,
//					new HttpEntity<MultiValueMap<String, Object>>(parameters,
//							headers), Map.class);
//			if (adminResponse.getStatusCode() != HttpStatus.OK) {
//				logger.info("Cannot login as admin, remote server problem");
//				throw new RuntimeException("LDAP authentication failed");
//			}
//			
//			SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
//					"admin3", "123", UaaAuthority.ADMIN_AUTHORITIES));

			ScimUser user = new ScimUser();
			user.setUserName(username);
			user.setName(new ScimUser.Name(username, ""));
			user.addEmail(username);
			// user.setUserType(UaaAuthority.UAA_USER.getUserType());

			ResponseEntity<ScimUser> userResponse = restTemplate.postForEntity(
					scimUrl, user, ScimUser.class);

			ScimUser newUser = userResponse.getBody();

			PasswordChangeRequest change = new PasswordChangeRequest();
			change.setPassword(password);

			ResponseEntity<Void> result = restTemplate.exchange(scimUrl
					+ "/{id}/password", HttpMethod.PUT,
					new HttpEntity<PasswordChangeRequest>(change, headers),
					null, newUser.getId());
			if (result.getStatusCode() != HttpStatus.OK) {
				throw new RuntimeException("Cannot create user in UAA");
			}

			logger.debug("re-login to UAA via rest");

			@SuppressWarnings("rawtypes")
			ResponseEntity<Map> newResponse = restTemplate.exchange(loginUrl,
					HttpMethod.POST,
					new HttpEntity<MultiValueMap<String, Object>>(parameters,
							headers), Map.class);
			if (newResponse.getStatusCode() == HttpStatus.OK) {
				String userFromUaa = (String) response.getBody()
						.get("username");
				if (userFromUaa.equals(userFromUaa)) {
					logger.info("Successful authentication request for "
							+ username);
					Authentication authResult = new UsernamePasswordAuthenticationToken(
							username, null, UaaAuthority.USER_AUTHORITIES);
					SecurityContextHolder.getContext().setAuthentication(
							authResult);
					return authResult;
				}
			} else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				logger.info("Cannot get authentication");
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
