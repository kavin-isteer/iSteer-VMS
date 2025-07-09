package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.Computer;

public class ComputerRowMapper implements RowMapper<Computer>{

	@Override
	public Computer mapRow(ResultSet rs, int rowNum) throws SQLException {
	    Computer computer = new Computer();

	    computer.setId(rs.getLong("id"));
	    computer.setUuid(rs.getString("uuid"));
	    computer.setDeviceId(rs.getString("device_id"));
	    computer.setMachineName(rs.getString("hostname"));
	    computer.setIpAddress(rs.getString("ip_address"));
	    computer.setOsVersion(rs.getString("os_version"));
	    computer.setAntiVirusStatus(rs.getString("antivirus_status"));
	    computer.setFirewallStatus(rs.getString("firewall_status"));
	    computer.setLoggedinUser(rs.getString("logged_in_user"));

	    Timestamp lastUpdateCheckTs = rs.getTimestamp("last_update_check");
	    computer.setLastUpdateCheck(lastUpdateCheckTs != null ? lastUpdateCheckTs.toLocalDateTime() : null);

	    Timestamp ts = rs.getTimestamp("timestamp");
	    computer.setTimestamp(ts != null ? ts.toLocalDateTime() : null);

	    computer.setDeleted(rs.getBoolean("is_deleted"));
	    computer.setActive(rs.getBoolean("is_active"));

	    Timestamp createdAtTs = rs.getTimestamp("created_at");
	    computer.setCreatedAt(createdAtTs != null ? createdAtTs.toLocalDateTime() : null);

	    Timestamp updatedAtTs = rs.getTimestamp("updated_at");
	    computer.setUpdatedAt(updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null);

	    return computer;
	}


}
