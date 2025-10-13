package com.isteer.vms.security;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.isteer.vms.dto.AuthenticationStatus;
import com.isteer.vms.enums.AuthenticationStatusEnum;
import com.isteer.vms.service.AuthenticationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter{
	@Autowired
	private TokenManager tokenManager;
	@Autowired
	private AuthenticationService authenticationService;
	
	private String loginLink = "/login";
	private String logoutLink = "/logout";
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
	        throws ServletException, IOException {

	    String currentLink = request.getServletPath() + (request.getPathInfo() != null ? request.getPathInfo() : "");
	    String authHeader = request.getHeader("Authorization");
	    String jwtToken = null;
	    boolean authenticated = false;

	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        jwtToken = authHeader.substring(7);
	        authenticated = checkToken(request, response, jwtToken);
	    }

	    response.setContentType("application/json");

	    if (authenticated && request.getMethod().equalsIgnoreCase("POST") && currentLink.equals(logoutLink)) {
	        authenticationService.logout(jwtToken);
	        response.setStatus(HttpServletResponse.SC_OK);
	        response.getWriter().println("{\"message\":\"Logged out successfully\"}");
	        return;
	    } else if (!authenticated && request.getMethod().equalsIgnoreCase("POST") && currentLink.equals(loginLink)) {
	        String username = request.getParameter("username");
	        String password = request.getParameter("password");

	        if (username == null || password == null) {
	            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	            response.getWriter().println("{\"message\":\"Username and password are required\"}");
	            return;
	        }

	        AuthenticationStatus status = authenticationService.authenticate(username, password);

	        if (status.getStatus() == AuthenticationStatusEnum.AUTHENTICATION_SUCCESS) {
	            response.setStatus(HttpServletResponse.SC_OK);
	            response.getWriter().println("{\"token\":\"" + status.getToken() + "\",\"user\":{\"uuid\":\"" + status.getUser().getUuid()
	                    + "\",\"username\":\"" + status.getUser().getUsername()
	                    + "\",\"roleId\":" + status.getUser().getRoleId() + "}}");
	        } else if (status.getStatus() == AuthenticationStatusEnum.BAD_CREDENTIALS) {
	            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	            response.getWriter().println("{\"message\":\"Invalid username or password\"}");
	        }
	        return;
	    }else if(!authenticated) {
	    	response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	        PrintWriter writer = response.getWriter();
	        writer.println("{\"message\":\"Unauthorized: Invalid or missing token\"}");
	        writer.flush();
	        return;
	    }

	    // For all other requests, continue the filter chain
	    filterChain.doFilter(request, response);
	}
	
	private boolean checkToken(HttpServletRequest request, HttpServletResponse response, String token) {
			if (tokenManager.validateToken(token)) {
				UserDetails userDetails = tokenManager.getUserDetails(token);
				Authentication securityToken = new PreAuthenticatedAuthenticationToken(userDetails, null,
						userDetails.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(securityToken);
				return true;
			} else {
				return false;
			}
	}
}
