package com.isteer.vms.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.isteer.vms.util.JwtUtil;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class TokenManager {
	@Autowired
	private JwtUtil jwtUtil;
	
	private Map<UserDetails,String> tokenStore = new HashMap<>();

	public String generateToken(UserDetails userDetails) {
		String token ;
		do {
		token= jwtUtil.generateToken(userDetails instanceof UserContext ? ((UserContext)userDetails).getUser() : null);
		}while (tokenStore.containsValue(token));
		tokenStore.put(userDetails, token);
		return token;
	}

	public UserDetails getUserDetails(String token) {
		return tokenStore.entrySet().stream()
				.filter(entry -> entry.getValue().equals(token))
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	public void invalidateToken(String token) {
		tokenStore.values().removeIf(storedToken -> storedToken.equals(token));
	}
	
	public void invalidateAllUserSessions(UserDetails userDetails) {
		tokenStore.keySet().removeIf(storedUser -> storedUser.getUsername().equals(userDetails.getUsername()));
	}
	
	public boolean validateToken(String token) {
		return tokenStore.containsValue(token) && jwtUtil.validateToken(token, getUserDetails(token));
	}
}
