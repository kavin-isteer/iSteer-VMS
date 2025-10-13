package com.isteer.vms.dao;

import com.isteer.vms.model.User;

public interface UserDao {
	User findByUsername(String username);
}
