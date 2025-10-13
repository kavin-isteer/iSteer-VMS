package com.isteer.vms.service;

import com.isteer.vms.dto.AuthenticationStatus;

public interface AuthenticationService {
	AuthenticationStatus authenticate(String username, String password);
	void logout(String token);
}
