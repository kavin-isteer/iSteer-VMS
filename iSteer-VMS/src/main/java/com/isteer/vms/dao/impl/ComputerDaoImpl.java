package com.isteer.vms.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.isteer.vms.dao.ComputerDao;
import com.isteer.vms.dao.rowmapper.ComputerRowMapper;
import com.isteer.vms.model.Computer;

import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class ComputerDaoImpl implements ComputerDao {

	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ComputerDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public List<Computer> getAllComputers() {
		String query = "SELECT id, uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE is_deleted = false AND is_active = true";
		log.debug("Fetching all computers from the database");
		try {
			return jdbcTemplate.query(query, new ComputerRowMapper());
		} catch (DataAccessException e) {
			log.error("Error fetching computers from the database");
			return List.of();
		}
	}
	
	@Override
	public List<Computer> getAllComputers(String isActiveStatus) {
		String query = "SELECT id, uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE is_active = :isActive AND is_deleted = false";
		log.debug("Fetching computers from the database with status {}.", isActiveStatus);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("isActive", Boolean.parseBoolean(isActiveStatus));
		try {
			return namedParameterJdbcTemplate.query(query, params, new ComputerRowMapper());
		} catch (DataAccessException e) {
			log.error("Error fetching computers from the database");
			return List.of();
		}
	}

	@Override
	public Optional<Computer> getComputerByDeviceId(String deviceId) {
		log.debug("Fetching computer with deviceId: {}", deviceId);
		String query = "SELECT id, uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE device_id = :deviceId";

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("deviceId", deviceId);

		try {
			Computer computer = namedParameterJdbcTemplate.queryForObject(query, params, new ComputerRowMapper());
			log.info("Computer found with deviceId: {}", deviceId);
			return Optional.ofNullable(computer);
		} catch (EmptyResultDataAccessException e) {

			log.warn("No computer found with deviceId: {}", deviceId);
			return Optional.empty();
		}
	}

	@Override
	public int createOrUpdateComputer(Computer computer) {
		log.debug("Creating or updating computer with deviceId: {}", computer.getDeviceId());
		String query = "INSERT INTO computers (uuid, device_id, hostname, ip_address, os_version, antivirus_status, firewall_status, logged_in_user, last_update_check, timestamp, is_deleted, is_active) "
				+ "VALUES (:uuid, :deviceId, :machineName, :ipAddress, :osVersion, :antivirusStatus, :firewallStatus, :loggedInUser, :lastUpdateCheck, :timestamp, :isDeleted, :isActive) "
				+ "ON DUPLICATE KEY UPDATE " + "hostname = :machineName, " + "ip_address = :ipAddress, "
				+ "os_version = :osVersion, " + "antivirus_status = :antivirusStatus, "
				+ "firewall_status = :firewallStatus, " + "logged_in_user = :loggedInUser, "
				+ "last_update_check = :lastUpdateCheck, " + "timestamp = :timestamp, " + "is_deleted = :isDeleted, "
				+ "is_active = :isActive ";
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
		try {
			return namedParameterJdbcTemplate.update(query, params);
		} catch (DataAccessException e) {
			log.error("Failed to create or update computer: {}", computer.getDeviceId());
			return 0;
		}
	}

	@Override
	public int getTotalComputersCount() {
		String query = "SELECT COUNT(*) FROM computers WHERE is_active = true";
		log.debug("Fetching total count of active computers from the database");
		try {
			return jdbcTemplate.queryForObject(query, Integer.class);
		} catch(DataAccessException e) {
			log.error("Error fetching total computers count from the database");
			return 0;
		}
	}

	@Override
	public int getVulnerableComputersCount() {
		String query = "SELECT COUNT(DISTINCT c.uuid) FROM computers c "
				+ "JOIN computer_applications ca ON ca.computer_uuid = c.uuid "
				+ "JOIN application_vulnerabilities av ON av.application_uuid = ca.application_uuid "
				+ "WHERE ca.is_deleted = false AND c.is_deleted = false AND c.is_active = true";
		log.debug("Fetching count of vulnerable computers from the database");
		try {
			return jdbcTemplate.queryForObject(query, Integer.class);
		} catch(DataAccessException e) {
			log.error("Error fetching vulnerable computers count from the database");
			return 0;
		}
	}

	@Override
	public Map<String, Integer> getInstalledAppCounts() {
		log.debug("Fetching counts of installed applications per computer from the database");
		String query = "SELECT computer_uuid, COUNT(DISTINCT application_uuid) AS app_count "
				+ "FROM computer_applications WHERE is_deleted = false GROUP BY computer_uuid";
		try {
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while(rs.next()) {
					String computerUuid = rs.getString("computer_uuid");
					Integer appCount = rs.getInt("app_count");
					result.put(computerUuid, appCount);
				}
				return result;
			});
		} catch (Exception e) {
			log.error("Error fetching installed application counts: {}", e.getMessage());
			return Map.of();
		}
	}

	@Override
	public Map<String, Integer> getVulnerableAppCounts() {
		log.debug("Fetching counts of vulnerable applications per computer from the database.");
		String query = "SELECT ca.computer_uuid, COUNT(DISTINCT ca.application_uuid) AS vuln_app_count"
				+ " FROM computer_applications ca "
				+ "JOIN application_vulnerabilities av ON ca.application_uuid = av.application_uuid "
				+ "WHERE ca.is_deleted = false GROUP BY ca.computer_uuid";
		try {
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while(rs.next()) {
					String computerUuid = rs.getString("computer_uuid");
					Integer vulnAppCount = rs.getInt("vuln_app_count");
					result.put(computerUuid, vulnAppCount);
				}
				return result;
			});
		} catch (Exception e) {
			log.error("Error fetching vulnerable application counts: {}", e.getMessage());
			return Map.of();
		}
	}

}
