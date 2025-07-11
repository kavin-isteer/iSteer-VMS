package com.isteer.vms.dao.impl;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.isteer.vms.dao.ComputerDao;
import com.isteer.vms.dao.rowmapper.ComputerRowMapper;
import com.isteer.vms.model.Computer;

@Repository
public class ComputerDaoImpl implements ComputerDao{
	
	private static final Logger logger = LogManager.getLogger(ComputerDaoImpl.class);
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public List<Computer> getAllComputers() {
		String query = "SELECT id, uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers";
		return jdbcTemplate.query(query, new ComputerRowMapper());
	}

	@Override
	public Optional<Computer> getComputerByDeviceId(String deviceId) {
		logger.debug("Fetching computer with deviceId: {}", deviceId);
	    String query = "SELECT id, uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE device_id = :deviceId";
	    
	    MapSqlParameterSource params = new MapSqlParameterSource();
	    params.addValue("deviceId", deviceId);

	    try {
	        Computer computer = namedParameterJdbcTemplate.queryForObject(query, params, new ComputerRowMapper());
	        logger.info("Computer found with deviceId: {}", deviceId);
	        return Optional.ofNullable(computer);
	    } catch (EmptyResultDataAccessException e) {
	    	
	    	logger.warn("No computer found with deviceId: {}", deviceId);
	        return Optional.empty();
	    }
	}

	@Override
	public int createOrUpdateComputer(Computer computer) {
		logger.debug("Creating or updating computer with deviceId: {}", computer.getDeviceId());
		String query = "INSERT INTO computers (uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active) " +
				"VALUES (:uuid, :deviceId, :machineName, :ipAddress, :osVersion, :antivirusStatus, :firewallStatus, :loggedInUser, :lastUpdateCheck, :timestamp, :isDeleted, :isActive) " +
				"ON DUPLICATE KEY UPDATE " +
				"hostname = :machineName, " +
				"ip_address = :ipAddress, " +
				"os_version = :osVersion, " +
				"antivirus_status = :antivirusStatus, " +
				"firewall_status = :firewallStatus, " +
				"logged_in_user = :loggedInUser, " +
				"last_update_check = :lastUpdateCheck, " +
				"timestamp = :timestamp, " +
				"is_deleted = :isDeleted, " +
				"is_active = :isActive ";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("uuid", computer.getUuid());
		params.addValue("deviceId", computer.getDeviceId());
		params.addValue("machineName", computer.getMachineName());
		params.addValue("ipAddress", computer.getIpAddress());
		params.addValue("osVersion", computer.getOsVersion());
		params.addValue("antivirusStatus", computer.getAntiVirusStatus());
		params.addValue("firewallStatus", computer.getFirewallStatus());
		params.addValue("loggedInUser", computer.getLoggedinUser());
		params.addValue("lastUpdateCheck", computer.getLastUpdateCheck());
		params.addValue("timestamp", computer.getTimestamp());
		params.addValue("isDeleted", computer.isDeleted());
		params.addValue("isActive", computer.isActive());
		return namedParameterJdbcTemplate.update(query, params);
	}

}
