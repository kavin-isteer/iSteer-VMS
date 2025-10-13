package com.isteer.vms.service;

import com.isteer.vms.dto.AuthenticationStatus;
import com.isteer.vms.model.User;

public interface AuthenticationService {
	AuthenticationStatus authenticate(String username, String password);
	void logout(String token);
	User currentUser();
}
