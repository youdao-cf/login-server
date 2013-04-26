package org.cloudfoundry.identity.uaa.login;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

public class LdapAuthHelper {
	private String URL = "ldap://soda.rd.netease.com:389/";
	private String BASEDN = "ou=people,dc=rd,dc=netease,dc=com";
	private String FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private LdapContext ctx = null;
	private Hashtable<String, String> env = null;
	private Control[] connCtls = null;

	private void LDAP_connect() {
		env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, FACTORY);
		env.put(Context.PROVIDER_URL, URL + BASEDN);// LDAP server
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		try {
			ctx = new InitialLdapContext(env, connCtls);
		} catch (javax.naming.AuthenticationException e) {
			System.out.println("Authentication faild: " + e.toString());
		} catch (Exception e) {
			System.out.println("Something wrong while authenticating: "
					+ e.toString());
		}
	}

	private String getUserDN(String email) {
		String userDN = "";

		LDAP_connect();

		try {
			SearchControls constraints = new SearchControls();
			constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<?> en = ctx.search("", "mail=" + email,
					constraints); // The UID you are going to query,* means all
									// nodes
			if (en == null) {
				System.out.println("Have no NamingEnumeration.");
			}
			if (!en.hasMoreElements()) {
				System.out.println("Have no element.");
			}
			while (en != null && en.hasMoreElements()) {// maybe more than one
														// element
				Object obj = en.nextElement();
				if (obj instanceof SearchResult) {
					SearchResult si = (SearchResult) obj;
					userDN += si.getName();
					userDN += "," + BASEDN;
				} else {
					System.out.println(obj);
				}
				System.out.println();
			}
		} catch (Exception e) {
			System.out.println("Exception in search():" + e);
		}

		return userDN;
	}

	public boolean authenticate(String ID, String password) {
		boolean valide = false;
		String userDN = getUserDN(ID);
		System.out.println("userdn:" + userDN);
		try {
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			ctx.reconnect(connCtls);
			System.out.println(userDN + " is authenticated");
			valide = true;
		} catch (AuthenticationException e) {
			System.out.println(userDN + " is not authenticated");
			System.out.println(e.toString());
			valide = false;
		} catch (NamingException e) {
			System.out.println(userDN + " is not authenticated");
			valide = false;
		}

		return valide;
	}
}