package com.isteer.vms.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.rowmapper.ApplicationRowMapper;
import com.isteer.vms.dao.rowmapper.ComputerApplicationRowMapper;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation of the ApplicationDao interface for database operations related to applications.
 * This class uses JdbcTemplate and NamedParameterJdbcTemplate for database interaction.
 */
@Repository
@Log4j2
public class ApplicationDaoImpl implements ApplicationDao {

	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ObjectMapper objectMapper;

	public ApplicationDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Retrieves a list of all applications from the database.
	 *
	 * @return A list of {@link Application} objects. Returns an empty list if an error occurs.
	 */
	@Override
	public List<Application> getAllApplications() {
		log.debug("Fetching all applications from the database");
		// SQL query to select all applications.
		String query = "SELECT id, uuid, name, version, vendor_name, created_at FROM applications";
		try {
			// Execute the query and map the results to a list of Application objects.
			return jdbcTemplate.query(query, new ApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error while fetching all applications: {}", e.getMessage());
			// Return an empty list in case of any exception.
			return List.of();
		}
	}

	//Unused currently, but may be utilized in future
	@SuppressWarnings("unused")
	/**
	 * Retrieves applications based on their vulnerability status.
	 *
	 * @param isVulnerable A string ("true" or "false") indicating whether to fetch
	 * applications with or without vulnerabilities.
	 * @return A list of {@link Application} objects matching the criteria. Returns an empty list if an error occurs.
	 */
	@Override
	public List<Application> getAllApplications(String isVulnerable) {
		log.debug("Fetching all applications with vulnerabilities status: {}", isVulnerable);
		
		// SQL query to select applications that have at least one associated vulnerability.
		String hasVulnerability = "SELECT DISTINCT a.id, a.uuid, a.name, a.version, a.vendor_name, a.created_at "
				+ "FROM applications a"
				+ " JOIN application_vulnerabilities av ON a.uuid = av.application_uuid";

		// SQL query to select applications that have no associated vulnerabilities.
		String noVulnerability = "SELECT a.id, a.uuid, a.name, a.version, a.vendor_name, a.created_at "
				+ "FROM applications a "
				+ "LEFT JOIN application_vulnerabilities av ON a.uuid = av.application_uuid WHERE av.application_uuid IS NULL";

		// Check if the request is for vulnerable applications.
		if (Boolean.parseBoolean(isVulnerable)) {
			log.debug("Fetching applications with vulnerabilities");
			try {
				return jdbcTemplate.query(hasVulnerability, new ApplicationRowMapper());
			} catch (Exception e) {
				log.error("Error fetching applications with vulnerabilities with error: {}", e.getMessage());
				return List.of();
			}
		}
		
		// If not, fetch applications without vulnerabilities.
		log.debug("Fetching applications without vulnerabilities");
		try {
			return jdbcTemplate.query(noVulnerability, new ApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error fetching applications without vulnerabilities with error: {}.", e.getMessage());
			return List.of();
		}
	}

	/**
	 * Fetches all active applications installed on a specific computer, identified by its UUID.
	 *
	 * @param computerUuid The UUID of the computer.
	 * @return A list of {@link ComputerApplication} objects. Returns an empty list if an error occurs.
	 */
	@Override
	public List<ComputerApplication> getApplicationsByComputerUuid(String computerUuid) {
		log.debug("Fetching applications for computer UUID: {}", computerUuid);
		
		// SQL query to join computer_applications with applications for a specific computer.
		String query = "SELECT ca.uuid, ca.application_uuid, ca.computer_uuid, a.name, a.version, a.vendor_name, ca.installed_date, ca.process_ids, ca.is_deleted, ca.created_at, ca.updated_at "
				+ "FROM computer_applications ca " + "JOIN applications a ON ca.application_uuid = a.uuid "
				+ "WHERE ca.computer_uuid = :computerUuid AND ca.is_deleted = false";
		
		// Create a parameter source and add the computer UUID.
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("computerUuid", computerUuid);
		
		try {
			// Execute the query using named parameters.
			return namedParameterJdbcTemplate.query(query, params, new ComputerApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error fetching applications for computer UUID {}: {}", computerUuid, e.getMessage());
			return List.of();
		}
	}

	/**
	 * Inserts a batch of new applications into the database. It ignores duplicates based on unique keys.
	 *
	 * @param applications A list of {@link Application} objects to insert.
	 * @return 1 on success, 0 on failure.
	 */
	@Override
	public int insertApplications(List<Application> applications) {
		log.debug("Inserting {} applications into the database", applications.size());
		
		// SQL query to insert an application. `INSERT IGNORE` prevents errors if the application already exists.
		String query = "INSERT IGNORE INTO applications (uuid, name, version, vendor_name) "
				+ "VALUES (:uuid, :name, :version, :vendorName)";
		
		// Create a batch of parameter sources from the list of applications.
		MapSqlParameterSource[] batchParams = applications.stream()
				.map(app -> new MapSqlParameterSource()
						.addValue("uuid", app.getUuid())
						.addValue("name", app.getSoftwareName())
						.addValue("version", app.getSoftwareVersion())
						.addValue("vendorName", app.getVendorName()))
				.toArray(MapSqlParameterSource[]::new);

		try {
			// Execute the batch update.
			int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
			// Check the status array for any failures.
			for (int i : status) {
				if (i == 0) {
					log.warn("Failed to insert application, status: {}", i);
					return 0; // Indicate failure
				}
			}
			log.debug("Successfully inserted {} applications into the database", applications.size());
			return 1; // Indicate success
		} catch (Exception e) {
			log.error("Error inserting applications: {}", e.getMessage());
			return 0; // Indicate failure
		}
	}

	/**
	 * Inserts or updates a batch of computer-application associations.
	 * If a record with the same primary key already exists, it updates the specified fields.
	 *
	 * @param computerApplications A list of {@link ComputerApplication} objects to insert or update.
	 * @return 1 on success, 0 on failure.
	 */
	@Override
	public int insertComputerApplications(List<ComputerApplication> computerApplications) {
		log.debug("Inserting {} computer applications mappings into the database", computerApplications.size());
		
		// SQL query with `ON DUPLICATE KEY UPDATE` to handle existing records.
		String query = "INSERT INTO computer_applications (uuid, application_uuid, computer_uuid, installed_date, is_deleted) "
				+ "VALUES (:uuid, :applicationUuid, :computerUuid, :installedAt, :isDeleted)"
				+ " ON DUPLICATE KEY UPDATE installed_date = :installedAt, is_deleted = :isDeleted";
		
		// Create a batch of parameter sources.
		MapSqlParameterSource[] batchParams = computerApplications.stream()
				.map(app -> new MapSqlParameterSource()
						.addValue("uuid", app.getUuid())
						.addValue("applicationUuid", app.getApplicationUuid())
						.addValue("computerUuid", app.getComputerUuid())
						.addValue("installedAt", app.getInstalledDate())
						.addValue("isDeleted", app.isDeleted()))
				.toArray(MapSqlParameterSource[]::new);

		try {
			// Execute the batch update.
			int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
			// Check status for any failures.
			for (int i : status) {
				if (i == 0) {
					log.warn("Failed to insert computer application mapping, status: {}", i);
					return 0; // Indicate failure
				}
			}
			log.debug("Successfully inserted {} computer applications mappings into the database", computerApplications.size());
			return 1; // Indicate success
		} catch (Exception e) {
			log.error("Error inserting computer applications: {}", e.getMessage());
			return 0; // Indicate failure
		}
	}

	/**
	 * Performs a soft delete or activation for a batch of computer-application associations
	 * by setting the `is_deleted` flag.
	 *
	 * @param computerApplications A list of {@link ComputerApplication} objects to update.
	 * @return 1 on success, 0 on failure.
	 */
	@Override
	public int deleteOrActivateComputerApplications(List<ComputerApplication> computerApplications) {
		log.debug("Updating {} computer applications mappings to set is_deleted in the database",
				computerApplications.size());
		
		// SQL query to update the is_deleted status.
		String query = "UPDATE computer_applications SET is_deleted = :isDeleted WHERE uuid = :uuid";
		
		// Create a batch of parameter sources.
		MapSqlParameterSource[] batchParams = computerApplications.stream().map(app -> new MapSqlParameterSource()
				.addValue("uuid", app.getUuid())
				.addValue("isDeleted", app.isDeleted())
				.addValue("processIds", app.getProcessIds() == null ? "[]" : toJsonString(app.getProcessIds())))
				.toArray(MapSqlParameterSource[]::new);
		try {
			// Execute the batch update.
			int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
			for (int i : status) {
				if (i == 0) {
					log.warn("Failed to update computer application mapping, status: {}", i);
					return 0; // Indicate failure
				}
			}
			return 1; // Indicate success
		} catch (Exception e) {
			log.error("Error updating computer applications: {}", e.getMessage());
			return 0; // Indicate failure
		}
	}

	/**
	 * Calculates and returns the count of installed vulnerable applications, grouped by vulnerability severity.
	 *
	 * @return A map where the key is the severity level (String) and the value is the count (Integer).
	 * Returns an empty map if an error occurs.
	 */
	@Override
	public Map<String, Integer> getInstalledVulnerableAppCounts() {
		log.debug("Fetching counts of installed vulnerable applications by severity from the database");
		
		// SQL query to count distinct vulnerable apps on active computers, grouped by severity.
		String query = "select v.severity, COUNT(distinct av.application_uuid) as app_count from computers c "
				+ "join computer_applications ca on ca.computer_uuid = c.uuid "
				+ "join application_vulnerabilities av on av.application_uuid = ca.application_uuid "
				+ "join vulnerabilities v on v.uuid = av.vulnerability_uuid "
				+ "where c.is_active = true and c.is_deleted = false "
				+ "and ca.is_deleted = false "
				+ "group by v.severity "
				+ "order by v.severity";
		try {
			// Execute the query and process the ResultSet into a Map.
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while (rs.next()) {
					String severity = rs.getString("severity");
					Integer appCount = rs.getInt("app_count");
					result.put(severity, appCount);
				}
				log.debug("Successfully fetched installed vulnerable application counts by severity");
				return result;
			});
		} catch (Exception e) {
			log.error("Error fetching installed vulnerable application counts: {}", e.getMessage());
			return Map.of();
		}
	}

	/**
	 * Retrieves a list of applications that have unresolved Common Platform Enumeration (CPE) names.
	 *
	 * @return A list of {@link Application} objects with unresolved CPEs. Returns an empty list if an error occurs.
	 */
	@Override
	public List<Application> getUnresolvedApplications() {
		log.debug("Fetching unresolved applications from the database");
		
		// SQL query to find applications linked to unresolved CPE details.
		String query = "select a.id, a.uuid, a.name, a.vendor_name, a.version, a.created_at from applications a "
				+ "join application_cpe_name_details acnd "
				+ "on acnd.application_uuid = a.uuid "
				+ "where acnd.is_resolved_cpe = false";

		try {
			return jdbcTemplate.query(query, new ApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error fetching unresolved applications: {}", e.getMessage());
			return List.of();
		}
	}

	/**
	 * Updates the process IDs for a batch of computer-application associations.
	 * The update only occurs if the new process ID list is different from the existing one.
	 *
	 * @param compAppsToUpdate A list of {@link ComputerApplication} objects with updated process IDs.
	 */
	@Override
	public void updateProcessIdsBatch(List<ComputerApplication> compAppsToUpdate) {
		log.debug("Updating process IDs for {} computer applications in batch", compAppsToUpdate.size());
		
		// SQL query to update process_ids, with a condition to avoid unnecessary writes.
		String query = "UPDATE computer_applications SET process_ids = :processIds WHERE computer_uuid = :computerUuid"
				+ " AND application_uuid = :applicationUuid AND (process_ids IS NULL OR process_ids != :processIds)";

		// Create a batch of parameter sources.
		MapSqlParameterSource[] params = compAppsToUpdate.stream()
				.map(app -> new MapSqlParameterSource()
						.addValue("computerUuid", app.getComputerUuid())
						.addValue("applicationUuid", app.getApplicationUuid())
						.addValue("processIds", toJsonString(app.getProcessIds())))
				.toArray(MapSqlParameterSource[]::new);
		try {
			// Execute the batch update.
			int[] status = namedParameterJdbcTemplate.batchUpdate(query, params);
			for (int i : status) {
				if (i == 0) {
					// A status of 0 could mean the record was not found or the process_ids were already the same.
					// This is logged as a warning rather than a critical failure.
					log.warn("Failed to update process IDs for a computer application, status: {}", i);
				}
			}
			log.debug("Successfully updated process IDs for {} computer applications", compAppsToUpdate.size());
		} catch (Exception e) {
			log.error("Error updating process IDs for computer applications: {}", e.getMessage());
		}
	}

	/**
	 * A private helper method to convert a list of process IDs into a JSON string representation.
	 *
	 * @param processIds A list of integers representing process IDs.
	 * @return A JSON array as a string (e.g., "[123, 456]"). Returns "[]" if the list is null, empty, or an error occurs.
	 */
	private String toJsonString(List<Integer> processIds) {
		// Handle null or empty lists gracefully.
		if (processIds == null || processIds.isEmpty()) {
			return "[]"; // Return an empty JSON array.
		}
		try {
			// Use ObjectMapper to serialize the list to a JSON string.
			return objectMapper.writeValueAsString(processIds);
		} catch (JsonProcessingException e) {
			log.error("Error converting process IDs to JSON", e);
			return "[]"; // Return an empty array on error.
		}
	}

	/**
	 * Finds detailed information about applications installed on a specific computer,
	 * including application details and CPE information.
	 *
	 * @param computerUuid The UUID of the computer.
	 * @return A list of Maps, where each map represents a row of application data.
	 */
	@Override
	public List<Map<String, Object>> findApplicationsByComputerUuid(String computerUuid) {
		// A multi-line SQL query for readability, fetching comprehensive application data.
		String sql = """
            SELECT
                ca.uuid as computer_application_uuid,
                ca.installed_date,
                ca.process_ids,
                a.uuid as application_uuid,
                a.name as software_name,
                a.version as software_version,
                a.vendor_name as vendor,
                acnd.cpe_name,
                acnd.is_resolved_cpe
            FROM computer_applications ca
            INNER JOIN applications a ON ca.application_uuid = a.uuid
            LEFT JOIN application_cpe_name_details acnd ON a.uuid = acnd.application_uuid
            WHERE ca.computer_uuid = ? AND ca.is_deleted = 0
            ORDER BY a.name
            """;
		// Execute the query and return the list of results.
		return jdbcTemplate.queryForList(sql, computerUuid);
	}

	/**
	 * Parses a JSON string of process IDs into a List of Integers.
	 *
	 * @param processIdsJson The JSON string to parse (e.g., "[101, 202]").
	 * @return A list of integers. Returns an empty list if the JSON is null, blank, or invalid.
	 */
	@Override
	public List<Integer> parseProcessIds(String processIdsJson) {
		// Return an empty list for null or empty input.
		if (processIdsJson == null || processIdsJson.trim().isEmpty()) {
			return List.of();
		}
		
		try {
			// Deserialize the JSON string into a List of Integers.
			return objectMapper.readValue(processIdsJson, new TypeReference<List<Integer>>() {});
		} catch (Exception e) {
			log.error("Error parsing process IDs JSON: {}", processIdsJson, e);
			// Return an empty list in case of a parsing error.
			return List.of();
		}
	}
}