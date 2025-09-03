package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.Computer;

public class ComputerRowMapper implements RowMapper<Computer>{

	@Override
	public Computer mapRow(ResultSet rs, int rowNum) throws SQLException {
		return Computer.builder()
	            .id(rs.getLong("id"))
	            .uuid(rs.getString("uuid"))
	            .deviceId(rs.getString("device_id"))
	            .machineName(rs.getString("hostname"))
	            .ipAddress(rs.getString("ip_address"))
	            .osVersion(rs.getString("os_version"))
	            .antiVirusStatus(rs.getString("antivirus_status"))
	            .firewallStatus(rs.getString("firewall_status"))
	            .loggedinUserName(rs.getString("logged_in_user_name"))
	            .loggedInUserEmail(rs.getString("logged_in_user_email"))
	            .lastUpdateCheck(toLocalDateTime(rs.getTimestamp("last_update_check")))
	            .timestamp(toLocalDateTime(rs.getTimestamp("timestamp")))
	            .isDeleted(rs.getBoolean("is_deleted"))
	            .isActive(rs.getBoolean("is_active"))
	            .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
	            .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
	            .build();
	    }

	    private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
	        return ts != null ? ts.toLocalDateTime() : null;
	    }}
