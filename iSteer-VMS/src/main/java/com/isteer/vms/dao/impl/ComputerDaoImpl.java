package com.isteer.vms.dao.impl;

import java.util.ArrayList;
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
import com.isteer.vms.dto.ApplicationResponseDto;
import com.isteer.vms.dto.ComputerResponseDto;
import com.isteer.vms.exception.BusinessException;
import com.isteer.vms.model.Computer;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation of the {@link ComputerDao} interface for database operations related to computers.
 * This class uses JdbcTemplate and NamedParameterJdbcTemplate to interact with the database.
 */
@Repository
@Log4j2
public class ComputerDaoImpl implements ComputerDao {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ComputerDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	/**
	 * Retrieves a list of all active and non-deleted computers from the database.
	 *
	 * @return A list of {@link Computer} objects. Returns an empty list if an error occurs.
	 */
	@Override
	public List<Computer> getAllComputers() {
		// SQL query to select all computers that are not marked as deleted and are active.
		String query = "SELECT id, uuid, device_id, hostname, serial_number, mac_address, ip_address, os_version, antivirus_status, firewall_status, logged_in_user_name, logged_in_user_email, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE is_deleted = false AND is_active = true";
		log.debug("Fetching all computers from the database");
		try {
			// Execute the query and map each row to a Computer object.
			return jdbcTemplate.query(query, new ComputerRowMapper());
		} catch (DataAccessException e) {
			log.error("Error fetching computers from the database", e);
			// Return an empty list in case of a database access error.
			return List.of();
		}
	}

	/**
	 * Retrieves computers from the database based on their active status.
	 *
	 * @param isActiveStatus A string ("true" or "false") to filter computers by their active state.
	 * @return A list of {@link Computer} objects matching the active status. Returns an empty list on error.
	 */
	@Override
	public List<Computer> getAllComputers(String isActiveStatus) {
		// SQL query to select computers based on the 'is_active' flag.
		String query = "SELECT id, uuid, device_id, hostname, serial_number, mac_address, ip_address, os_version, antivirus_status, firewall_status, logged_in_user_name, logged_in_user_email, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE is_active = :isActive AND is_deleted = false";
		log.debug("Fetching computers from the database with status {}.", isActiveStatus);
		
		// Set up named parameters for the query.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("isActive", Boolean.parseBoolean(isActiveStatus));
		
		try {
			// Execute the query with the specified parameters.
			return namedParameterJdbcTemplate.query(query, params, new ComputerRowMapper());
		} catch (DataAccessException e) {
			log.error("Error fetching computers from the database", e);
			return List.of();
		}
	}

	/**
	 * Fetches a single computer from the database by its unique device ID.
	 *
	 * @param deviceId The device ID of the computer to retrieve.
	 * @return An {@link Optional} containing the {@link Computer} if found, otherwise an empty Optional.
	 */
	@Override
	public Optional<Computer> getComputerByDeviceId(String deviceId) {
		log.debug("Fetching computer with deviceId: {}", deviceId);
		String query = "SELECT id, uuid, device_id, hostname, serial_number, mac_address, ip_address, os_version, antivirus_status, firewall_status, logged_in_user_name, logged_in_user_email, last_update_check, timestamp, is_deleted, is_active, created_at, updated_at FROM computers WHERE device_id = :deviceId";

		// Bind the deviceId to the named parameter.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("deviceId", deviceId);

		try {
			// Execute the query, expecting a single result.
			Computer computer = namedParameterJdbcTemplate.queryForObject(query, params, new ComputerRowMapper());
			log.info("Computer found with deviceId: {}", deviceId);
			return Optional.ofNullable(computer);
		} catch (EmptyResultDataAccessException e) {
			// This exception is expected when no record is found.
			log.warn("No computer found with deviceId: {}", deviceId);
			return Optional.empty();
		}
	}

	/**
	 * Inserts a new computer record into the database.
	 *
	 * @param computer The {@link Computer} object to be created.
	 * @return The number of rows affected (1 on success, 0 on failure).
	 */
	@Override
	public int createComputer(Computer computer) {
		log.debug("Creating computer with deviceId: {}", computer.getDeviceId());
		// SQL INSERT statement.
		String query = "INSERT INTO computers (uuid, device_id, hostname, serial_number, mac_address, ip_address, os_version, antivirus_status, firewall_status, logged_in_user_name, logged_in_user_email, last_update_check, timestamp, is_deleted, is_active) "
				+ "VALUES (:uuid, :deviceId, :machineName, :serialNumber, :macAddress, :ipAddress, :osVersion, :antivirusStatus, :firewallStatus, :loggedInUserName, :loggedInUserEmail, :lastUpdateCheck, :timestamp, :isDeleted, :isActive) ";
		
		// Map computer object properties to SQL named parameters.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("uuid", computer.getUuid());
		params.addValue("deviceId", computer.getDeviceId());
		params.addValue("machineName", computer.getMachineName());
		params.addValue("serialNumber", computer.getSerialNumber());
		params.addValue("macAddress", computer.getMacAddress());
		params.addValue("ipAddress", computer.getIpAddress());
		params.addValue("osVersion", computer.getOsVersion());
		params.addValue("antivirusStatus", computer.getAntiVirusStatus());
		params.addValue("firewallStatus", computer.getFirewallStatus());
		params.addValue("loggedInUserName", computer.getLoggedinUserName());
		params.addValue("loggedInUserEmail", computer.getLoggedInUserEmail());
		params.addValue("lastUpdateCheck", computer.getLastUpdateCheck());
		params.addValue("timestamp", computer.getTimestamp());
		params.addValue("isDeleted", computer.isDeleted());
		params.addValue("isActive", computer.isActive());
		
		try {
			// Execute the insert operation.
			return namedParameterJdbcTemplate.update(query, params);
		} catch (DataAccessException e) {
			log.error("Failed to create computer: {}, with error message: {}", computer.getDeviceId(), e.getMessage());
			return 0; // Return 0 to indicate failure.
		}
	}

	/**
	 * Updates an existing computer's details in the database, identified by its device ID.
	 *
	 * @param computer The {@link Computer} object containing the updated information.
	 * @return The number of rows affected (1 on success, 0 on failure).
	 */
	@Override
	public int updateComputer(Computer computer) {
		log.debug("Updating computer with deviceId: {}", computer.getDeviceId());
		// SQL UPDATE statement.
		String query = "UPDATE computers SET hostname = :machineName, ip_address = :ipAddress, os_version = :osVersion, antivirus_status = :antivirusStatus, firewall_status = :firewallStatus, logged_in_user_name = :loggedInUserName, logged_in_user_email = :loggedInUserEmail, last_update_check = :lastUpdateCheck, timestamp = :timestamp, is_deleted = :isDeleted, is_active = :isActive WHERE device_id = :deviceId";
		
		// Map computer object properties to SQL named parameters.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("deviceId", computer.getDeviceId());
		params.addValue("machineName", computer.getMachineName());
		params.addValue("ipAddress", computer.getIpAddress());
		params.addValue("osVersion", computer.getOsVersion());
		params.addValue("antivirusStatus", computer.getAntiVirusStatus());
		params.addValue("firewallStatus", computer.getFirewallStatus());
		params.addValue("loggedInUserName", computer.getLoggedinUserName());
		params.addValue("loggedInUserEmail", computer.getLoggedInUserEmail());
		params.addValue("lastUpdateCheck", computer.getLastUpdateCheck());
		params.addValue("timestamp", computer.getTimestamp());
		params.addValue("isDeleted", computer.isDeleted());
		params.addValue("isActive", computer.isActive());
		
		try {
			// Execute the update operation.
			return namedParameterJdbcTemplate.update(query, params);
		} catch (DataAccessException e) {
			log.error("Failed to update computer: {}", computer.getDeviceId(), e);
			return 0; // Return 0 to indicate failure.
		}
	}

	/**
	 * Gets the total count of active computers in the database.
	 *
	 * @return The total number of active computers, or 0 if an error occurs.
	 */
	@Override
	public int getTotalComputersCount() {
		String query = "SELECT COUNT(*) FROM computers WHERE is_active = true";
		log.debug("Fetching total count of active computers from the database");
		try {
			return jdbcTemplate.queryForObject(query, Integer.class);
		} catch (DataAccessException e) {
			log.error("Error fetching total computers count from the database", e);
			return 0;
		}
	}

	/**
	 * Calculates the number of unique computers that have at least one vulnerability.
	 *
	 * @return The count of vulnerable computers, or 0 if an error occurs.
	 */
	@Override
	public int getVulnerableComputersCount() {
		// This query counts distinct computers linked to vulnerabilities via their applications.
		String query = "SELECT COUNT(DISTINCT c.uuid) FROM computers c "
				+ "JOIN computer_applications ca ON ca.computer_uuid = c.uuid "
				+ "JOIN application_vulnerabilities av ON av.application_uuid = ca.application_uuid "
				+ "WHERE ca.is_deleted = false AND c.is_deleted = false AND c.is_active = true";
		log.debug("Fetching count of vulnerable computers from the database");
		try {
			return jdbcTemplate.queryForObject(query, Integer.class);
		} catch (DataAccessException e) {
			log.error("Error fetching vulnerable computers count from the database", e);
			return 0;
		}
	}

	/**
	 * Gets a map of computer UUIDs to the count of installed applications on each.
	 *
	 * @return A {@link Map} where the key is the computer UUID and the value is the installed app count.
	 */
	@Override
	public Map<String, Integer> getInstalledAppCounts() {
		log.debug("Fetching counts of installed applications per computer from the database");
		String query = "SELECT computer_uuid, COUNT(DISTINCT application_uuid) AS app_count "
				+ "FROM computer_applications WHERE is_deleted = false GROUP BY computer_uuid";
		try {
			// Use a ResultSetExtractor (as a lambda) to process the entire ResultSet into a Map.
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while (rs.next()) {
					String computerUuid = rs.getString("computer_uuid");
					Integer appCount = rs.getInt("app_count");
					result.put(computerUuid, appCount);
				}
				return result;
			});
		} catch (Exception e) {
			log.error("Error fetching installed application counts: {}", e.getMessage());
			return Map.of(); // Return an immutable empty map on error.
		}
	}

	/**
	 * Gets a map of computer UUIDs to the count of vulnerable applications on each.
	 *
	 * @return A {@link Map} where the key is the computer UUID and the value is the vulnerable app count.
	 */
	@Override
	public Map<String, Integer> getVulnerableAppCounts() {
		log.debug("Fetching counts of vulnerable applications per computer from the database.");
		// This query joins with the vulnerabilities table to count only vulnerable apps.
		String query = "SELECT ca.computer_uuid, COUNT(DISTINCT ca.application_uuid) AS vuln_app_count"
				+ " FROM computer_applications ca "
				+ "JOIN application_vulnerabilities av ON ca.application_uuid = av.application_uuid "
				+ "WHERE ca.is_deleted = false GROUP BY ca.computer_uuid";
		try {
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while (rs.next()) {
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

	/**
	 * Fetches a detailed list of all computers, aggregating their applications and the severity
	 * counts of vulnerabilities for each application.
	 *
	 * @return A list of {@link ComputerResponseDto} objects, each representing a computer with its vulnerable applications.
	 */
	@Override
	public List<ComputerResponseDto> getAllComputersWithVulnerabilities() {
		log.info("Fetching all computers with vulnerabilities from the database");
		// Complex SQL query to join computers, applications, and vulnerabilities, and aggregate severity counts.
		String sql = "SELECT " +
			    "cmp.uuid AS computer_uuid, " +
			    "cmp.hostname, " +
			    "cmp.serial_number, " +
			    "cmp.mac_address, " +
			    "cmp.ip_address, " +
			    "cmp.logged_in_user_name, " +
			    "cmp.logged_in_user_email, " +
			    "a.uuid AS application_uuid, " +
			    "a.name, " +
			    "a.version, " +
			    "a.vendor_name, " +
			    "COUNT(CASE WHEN v.severity = 'HIGH' THEN 1 END) AS high_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'MEDIUM' THEN 1 END) AS medium_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'LOW' THEN 1 END) AS low_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'CRITICAL' THEN 1 END) AS critical_severity_count " +
			    "FROM " +
			    "computers cmp " +
			    "INNER JOIN " +
			    "computer_applications cap ON cmp.uuid = cap.computer_uuid " +
			    "INNER JOIN " +
			    "applications a ON cap.application_uuid = a.uuid " +
			    "INNER JOIN " +
			    "application_vulnerabilities av ON a.uuid = av.application_uuid " +
			    "INNER JOIN " +
			    "vulnerabilities v ON av.vulnerability_uuid = v.uuid " +
			    "WHERE " +
			    "cmp.is_deleted = 0 AND cap.is_deleted=0 AND v.is_deleted = 0 " +
			    "GROUP BY " +
			    "cmp.uuid, cmp.hostname, cmp.serial_number, cmp.mac_address, cmp.ip_address, cmp.logged_in_user_name, cmp.logged_in_user_email, a.uuid, a.name, a.version, a.vendor_name;";

		// Use a ResultSetExtractor to transform the flat SQL result into a nested DTO structure.
		return jdbcTemplate.query(sql, rs -> {
			Map<String, ComputerResponseDto> dtoMap = new HashMap<>();
			while (rs.next()) {
				String computerUuid = rs.getString("computer_uuid");
				// Check if we have already created a DTO for this computer.
				if (dtoMap.containsKey(computerUuid)) {
					// If yes, just create the application DTO and add it to the existing computer's list.
					ApplicationResponseDto appDto = ApplicationResponseDto.builder().
							uuid(rs.getString("application_uuid"))
							.softwareName(rs.getString("name"))
							.softwareVersion(rs.getString("version"))
							.vendor(rs.getString("vendor_name"))
							.criticalVulnerabilityCount(rs.getInt("critical_severity_count"))
							.highVulnerabilityCount(rs.getInt("high_severity_count"))
							.mediumVulnerabilityCount(rs.getInt("medium_severity_count"))
							.lowVulnerabilityCount(rs.getInt("low_severity_count"))
							.build();
					dtoMap.get(computerUuid).getApplicationDetails().add(appDto);
				} else {
					// If no, create both the application DTO and the main computer DTO.
					List<ApplicationResponseDto> appDetails = new ArrayList<>();
					ApplicationResponseDto appDto = ApplicationResponseDto.builder().
							uuid(rs.getString("application_uuid"))
							.softwareName(rs.getString("name"))
							.softwareVersion(rs.getString("version"))
							.vendor(rs.getString("vendor_name"))
							.criticalVulnerabilityCount(rs.getInt("critical_severity_count"))
							.highVulnerabilityCount(rs.getInt("high_severity_count"))
							.mediumVulnerabilityCount(rs.getInt("medium_severity_count"))
							.lowVulnerabilityCount(rs.getInt("low_severity_count"))
							.build();
					appDetails.add(appDto);
					
					ComputerResponseDto computerDto = ComputerResponseDto.builder()
							.uuid(computerUuid)
							.machineName(rs.getString("hostname"))
							.serialNumber(rs.getString("serial_number"))
							.macAddress(rs.getString("mac_address"))
							.ipAddress(rs.getString("ip_address"))
							.loggedInUserName(rs.getString("logged_in_user_name"))
							.loggedInUserEmail(rs.getString("logged_in_user_email"))
							.applicationDetails(appDetails)
							.build();
					// Add the new computer DTO to the map to group subsequent applications.
					dtoMap.put(computerUuid, computerDto);
				}
			}
			// Convert the map's values to a list for the final return value.
			return new ArrayList<>(dtoMap.values());
		});
	}

	/**
	 * Fetches detailed vulnerability information for a single computer, identified by its UUID.
	 *
	 * @param uuid The unique identifier of the computer.
	 * @return A {@link ComputerResponseDto} for the specified computer, or null if an error occurs.
	 * @throws BusinessException if no computer is found for the given UUID.
	 */
	@Override
	public ComputerResponseDto getComputerWithVulnerabilitiesByUuid(String uuid) {
		log.info("Fetching computer with vulnerabilities for UUID: {} from the database.", uuid);
		// Similar to the above query, but with a WHERE clause to filter by computer UUID.
		String sql = "SELECT " +
			    "cmp.uuid AS computer_uuid, " +
			    "cmp.hostname, " +
			    "cmp.serial_number, " +
			    "cmp.mac_address, " +
			    "cmp.ip_address, " +
			    "cmp.logged_in_user_name, " +
			    "cmp.logged_in_user_email, " +
			    "a.uuid AS application_uuid, " +
			    "a.name, " +
			    "a.version, " +
			    "a.vendor_name, " +
			    "COUNT(CASE WHEN v.severity = 'HIGH' THEN 1 END) AS high_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'MEDIUM' THEN 1 END) AS medium_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'LOW' THEN 1 END) AS low_severity_count, " +
			    "COUNT(CASE WHEN v.severity = 'CRITICAL' THEN 1 END) AS critical_severity_count " +
			    "FROM " +
			    "computers cmp " +
			    "INNER JOIN " +
			    "computer_applications cap ON cmp.uuid = cap.computer_uuid " +
			    "INNER JOIN " +
			    "applications a ON cap.application_uuid = a.uuid " +
			    "INNER JOIN " +
			    "application_vulnerabilities av ON a.uuid = av.application_uuid " +
			    "INNER JOIN " +
			    "vulnerabilities v ON av.vulnerability_uuid = v.uuid " +
			    "WHERE " +
			    "cmp.is_deleted = 0 AND cap.is_deleted=0 AND v.is_deleted = 0 " +
			    "AND cmp.uuid = :computerUuid " +
			    "GROUP BY " +
			    "cmp.uuid, cmp.hostname, cmp.serial_number, cmp.mac_address, cmp.ip_address, cmp.logged_in_user_name, cmp.logged_in_user_email, a.uuid, a.name, a.version, a.vendor_name;";

		// Bind the computer UUID parameter.
		MapSqlParameterSource params = new MapSqlParameterSource()
	            .addValue("computerUuid", uuid);
		try {
			// Execute the query and process the results.
			return namedParameterJdbcTemplate.query(sql, params, rs -> {
				// Since we expect only one computer, we don't need a list. We build one DTO.
				ComputerResponseDto computerDto = null;
				while (rs.next()) {
					// On the first row, initialize the main ComputerResponseDto.
					if (computerDto == null) {
						computerDto = ComputerResponseDto.builder()
								.uuid(rs.getString("computer_uuid"))
								.machineName(rs.getString("hostname"))
								.serialNumber(rs.getString("serial_number"))
								.macAddress(rs.getString("mac_address"))
								.ipAddress(rs.getString("ip_address"))
								.loggedInUserName(rs.getString("logged_in_user_name"))
								.loggedInUserEmail(rs.getString("logged_in_user_email"))
								.applicationDetails(new ArrayList<>())
								.build();
					}
					// For every row, create an ApplicationResponseDto and add it to the computer's list.
					ApplicationResponseDto appDto = ApplicationResponseDto.builder().
							uuid(rs.getString("application_uuid"))
							.softwareName(rs.getString("name"))
							.softwareVersion(rs.getString("version"))
							.vendor(rs.getString("vendor_name"))
							.criticalVulnerabilityCount(rs.getInt("critical_severity_count"))
							.highVulnerabilityCount(rs.getInt("high_severity_count"))
							.mediumVulnerabilityCount(rs.getInt("medium_severity_count"))
							.lowVulnerabilityCount(rs.getInt("low_severity_count"))
							.build();
					computerDto.getApplicationDetails().add(appDto);
				}
				// If after processing the result set computerDto is still null, it means no rows were found.
				if (computerDto == null) {
					throw new EmptyResultDataAccessException(1);
				}
				return computerDto;
			});
		} catch (EmptyResultDataAccessException ex) {
			// Throw a specific business exception if the computer is not found.
			throw new BusinessException("Computer not found with UUID: " + uuid, 404);
		} catch (Exception ex) {
			log.error("Exception occurred while fetching computer with vulnerabilities for UUID: {}, with error: {}", uuid, ex.getMessage());
			return null;
		}
	}

	/**
	 * Finds a computer by its UUID.
	 *
	 * @param uuid The UUID of the computer to find.
	 * @return An {@link Optional} containing the {@link Computer} if found, otherwise an empty Optional.
	 */
	@Override
	public Optional<Computer> findByUuid(String uuid) {
		// A simple SQL query to select a single active, non-deleted computer by its UUID.
		String sql = """
            SELECT id, uuid, device_id, hostname, serial_number, mac_address, ip_address,
                   os_version, antivirus_status, firewall_status, logged_in_user_name,
                   logged_in_user_email, last_update_check, timestamp, is_deleted,
                   is_active, created_at, updated_at
            FROM computers 
            WHERE uuid = ? AND is_deleted = 0 AND is_active = 1
            """;
        
        try {
			// Use queryForObject as we expect exactly one result.
            Computer computer = jdbcTemplate.queryForObject(sql, new ComputerRowMapper(), uuid);
            return Optional.of(computer);
        } catch (EmptyResultDataAccessException e) {
			// Handle the case where no computer is found for the given UUID.
            log.warn("Computer not found with UUID: {}", uuid);
            return Optional.empty();
        }
    }
}