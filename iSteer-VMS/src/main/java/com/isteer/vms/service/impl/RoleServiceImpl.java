package com.isteer.vms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.isteer.vms.dao.UserDao;
import com.isteer.vms.service.AuthenticationService;
import com.isteer.vms.service.RoleService;

import jakarta.servlet.http.HttpServletRequest;
@Component(value = "roleService")
public class RoleServiceImpl implements RoleService{
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private AuthenticationService authenticationService;
	
	@Autowired
	UserDao userDAo;
	
	@Override
	public boolean authorize() {
		//currently role based authorization not implemented. This method needs to be implemented with proper logic.
		String endpoint = request.getRequestURI();
		int roleId = authenticationService.currentUser().getRoleId();
		if(userDAo.isAuthorized(endpoint, roleId)) {
			return true;
		}
		return false;
	}

}
