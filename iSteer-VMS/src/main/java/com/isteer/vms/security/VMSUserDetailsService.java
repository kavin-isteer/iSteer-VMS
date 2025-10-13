package com.isteer.vms.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.isteer.vms.dao.UserDao;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.model.User;
@Service
public class VMSUserDetailsService implements UserDetailsService{
	@Autowired
	UserDao userDao;
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userDao.findByUsername(username);
		if(user!=null) {
			return new UserContext(user);
		}else {
			throw new BusinessException("User not found!!", HttpStatus.NOT_FOUND.value());
		}
	}

}
