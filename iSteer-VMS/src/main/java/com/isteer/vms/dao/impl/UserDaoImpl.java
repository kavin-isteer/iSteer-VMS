package com.isteer.vms.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.isteer.vms.dao.UserDao;
import com.isteer.vms.model.User;
@Repository
public class UserDaoImpl implements UserDao{
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public User findByUsername(String username) {
		String query = "SELECT id, username, uuid, email, password, role_id, " +
	               "created_at, updated_at, created_by_uuid " +
	               "FROM users " +
	               "WHERE username = ?";
		try {
			return jdbcTemplate.queryForObject(query, new UserRowMapper(), username);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public boolean isAuthorized(String endpoint, int roleId) {
		String query = "SELECT COUNT(*) FROM api_access WHERE apiPath=? AND roleId=?";
		try {
			Integer count = jdbcTemplate.queryForObject(query, Integer.class, endpoint, roleId);
			return count != null && count > 0;
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}
}
	/**
	 * Inner class to map a row from the 'users' table to a User object.
	 */
	class UserRowMapper implements RowMapper<User> {
		@Override
		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			User user = new User();
			user.setId(rs.getInt("id"));
			user.setUsername(rs.getString("username"));
			user.setUuid(rs.getString("uuid"));
			user.setEmail(rs.getString("email"));
			user.setPassword(rs.getString("password"));
			user.setRoleId(rs.getInt("role_id"));
			user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
			user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
			user.setCreatedByUuid(rs.getString("created_by_uuid"));
			return user;
		}
	}