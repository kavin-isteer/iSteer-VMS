package com.isteer.vms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.isteer.vms.dto.AuthenticationStatus;
import com.isteer.vms.enums.AuthenticationStatusEnum;
import com.isteer.vms.model.User;
import com.isteer.vms.security.TokenManager;
import com.isteer.vms.security.UserContext;
import com.isteer.vms.service.AuthenticationService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AuthenticationServiceImpl implements AuthenticationService{
	@Autowired
	private TokenManager tokenManager;
	
	@Autowired
	@Lazy
	private AuthenticationManager authenticationManager;
	
	@Override
	public AuthenticationStatus authenticate(String username, String password) {
	    Authentication credentials = new UsernamePasswordAuthenticationToken(username, password);

	    try {
	        Authentication authResult = authenticationManager.authenticate(credentials);

	        if (authResult.isAuthenticated()) {
	            log.info("User {} authenticated successfully", username);

	            String token = tokenManager.generateToken((UserDetails) authResult.getPrincipal());

	            SecurityContextHolder.getContext().setAuthentication(authResult);

	            return AuthenticationStatus.builder()
	                    .status(AuthenticationStatusEnum.AUTHENTICATION_SUCCESS)
	                    .token(token)
	                    .user(((UserContext) authResult.getPrincipal()).getUser())
	                    .build();
	        }

	    } catch (Exception ex) {
	        log.warn("Authentication failed for user {}: {}", username, ex.getMessage());
	    }

	    return AuthenticationStatus.builder()
	            .status(AuthenticationStatusEnum.BAD_CREDENTIALS)
	            .build();
	}


	@Override
	public void logout(String token) {
		tokenManager.invalidateToken(token);
		SecurityContextHolder.clearContext();
		log.info("User logged out successfully");
	}
	
	@Override
	public User currentUser() {
		if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext().getAuthentication() != null
				&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null) {
			UserContext usercon = (UserContext) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			return usercon.getUser();
		} else
			return null;
	}
}
