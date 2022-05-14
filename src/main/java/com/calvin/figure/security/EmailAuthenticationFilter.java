package com.calvin.figure.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

public class EmailAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    
    public static final String SPRING_SECURITY_FORM_EMAIL_KEY = "email";

	public static final String SPRING_SECURITY_FORM_CODE_KEY = "code";

	private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/users/emailLogin",
			"POST");

	private String emailParameter = SPRING_SECURITY_FORM_EMAIL_KEY;

	private String codeParameter = SPRING_SECURITY_FORM_CODE_KEY;

	private boolean postOnly = true;

	public EmailAuthenticationFilter() {
		super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
	}

	public EmailAuthenticationFilter(AuthenticationManager authenticationManager) {
		super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		if (this.postOnly && !request.getMethod().equals("POST")) {
			throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
		}
		String email = obtainEmail(request);
		email = (email != null) ? email : "";
		email = email.trim();
		String code = obtainCode(request);
		code = (code != null) ? code : "";
		EmailAuthenticationToken authRequest = new EmailAuthenticationToken(email, code);
		// Allow subclasses to set the "details" property
		setDetails(request, authRequest);
		return this.getAuthenticationManager().authenticate(authRequest);
	}

	@Nullable
	protected String obtainCode(HttpServletRequest request) {
		return request.getParameter(this.codeParameter);
	}

	@Nullable
	protected String obtainEmail(HttpServletRequest request) {
		return request.getParameter(this.emailParameter);
	}

	/**
	 * Provided so that subclasses may configure what is put into the authentication
	 * request's details property.
	 * @param request that an authentication request is being created for
	 * @param authRequest the authentication request object that should have its details
	 * set
	 */
	protected void setDetails(HttpServletRequest request, EmailAuthenticationToken authRequest) {
		authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
	}


	public void setEmailParameter(String emailParameter) {
		Assert.hasText(emailParameter, "Email parameter must not be empty or null");
		this.emailParameter = emailParameter;
	}


	public void setCodeParameter(String codeParameter) {
		Assert.hasText(codeParameter, "Code parameter must not be empty or null");
		this.codeParameter = codeParameter;
	}
    
}
