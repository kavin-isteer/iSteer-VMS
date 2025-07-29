package com.isteer.vms.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.rowmapper.ApplicationRowMapper;
import com.isteer.vms.dao.rowmapper.ComputerApplicationRowMapper;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;

import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class ApplicationDaoImpl implements ApplicationDao {

	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public ApplicationDaoImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	@Override
	public List<Application> getAllApplications() {
		log.debug("Fetching all applications from the database");
		String query = "SELECT id, uuid, name, version, vendor_name, created_at FROM applications";
		try {
		return jdbcTemplate.query(query, new ApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error while fetching all applications: {}", e.getMessage());
			return List.of();
		}
	}

	@Override
	public List<Application> getAllApplications(String isVulnerable) {
		log.debug("Fetching all applications with vulnerabilities status: {}", isVulnerable);
		String hasVulnerability = "SELECT DISTINCT a.id, a.uuid, a.name, a.version, a.vendor_name, a.created_at "
				+ "FROM applications a"
				+ " JOIN application_vulnerabilities av ON a.uuid = av.application_uuid";
		
		String noVulnerability = "SELECT a.id, a.uuid, a.name, a.version, a.vendor_name, a.created_at "
				+ "FROM applications a "
				+ "LEFT JOIN application_vulnerabilities av ON a.uuid = av.application_uuid WHERE av.application_uuid IS NULL";
		
		if(Boolean.parseBoolean(isVulnerable)) {
			log.debug("Fetching applications with vulnerabilities");
			try {
				return jdbcTemplate.query(hasVulnerability, new ApplicationRowMapper());
			} catch (Exception e) {
				log.error("Error fetching applications with vulnerabilities with error: {}", e.getMessage());
				return List.of();
			}
		}
		log.debug("Fetching applications without vulneabilities");
		try {
			return jdbcTemplate.query(noVulnerability, new ApplicationRowMapper());
		} catch(Exception e) {
			log.error("Error fetching applications without vulnerabilities with error: {}.", e.getMessage());
			return List.of();
		}
	}

	@Override
	public List<ComputerApplication> getApplicationsByComputerUuid(String computerUuid) {
		log.debug("Fetching applications for computer UUID: {}", computerUuid);
		String query = "SELECT ca.uuid, ca.application_uuid, ca.computer_uuid, a.name, a.version, a.vendor_name, ca.installed_date, ca.is_deleted, ca.created_at, ca.updated_at "
				+ "FROM computer_applications ca " + "JOIN applications a ON ca.application_uuid = a.uuid "
				+ "WHERE ca.computer_uuid = :computerUuid AND ca.is_deleted = false";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("computerUuid", computerUuid);
		try {
			return namedParameterJdbcTemplate.query(query, params, new ComputerApplicationRowMapper());
		} catch (Exception e) {
			log.error("Error fetching applications for computer UUID {}: {}", computerUuid, e.getMessage());
			return List.of();
		}
	}

	@Override
	public int insertApplications(List<Application> applications) {
		log.debug("Inserting {} applications into the database", applications.size());
		String query = "INSERT IGNORE INTO applications (uuid, name, version, vendor_name) "
				+ "VALUES (:uuid, :name, :version, :vendorName)";

		MapSqlParameterSource[] batchParams = applications.stream()
				.map(app -> new MapSqlParameterSource().addValue("uuid", app.getUuid())
						.addValue("name", app.getSoftwareName()).addValue("version", app.getSoftwareVersion())
						.addValue("vendorName", app.getVendorName()))
				.toArray(MapSqlParameterSource[]::new);

		try {
		int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
		for (int i : status) {
			if (i == 0) {
				log.warn("Failed to insert application, status: {}", i);
				return 0;
			}
		}
		log.debug("Successfully inserted {} applications into the database", applications.size());
		return 1;
		} catch (Exception e) {
			log.error("Error inserting applications: {}", e.getMessage());
			return 0;
		}
	}

	@Override
	public int insertComputerApplications(List<ComputerApplication> computerApplications) {
		log.debug("Inserting {} computer applications mappings into the database", computerApplications.size());
		String query = "INSERT IGNORE INTO computer_applications (uuid, application_uuid, computer_uuid, installed_date, is_deleted) "
				+ "VALUES (:uuid, :applicationUuid, :computerUuid, :installedAt, :isDeleted)";
		MapSqlParameterSource[] batchParams = computerApplications.stream()
				.map(app -> new MapSqlParameterSource().addValue("uuid", app.getUuid())
						.addValue("applicationUuid", app.getApplicationUuid())
						.addValue("computerUuid", app.getComputerUuid()).addValue("installedAt", app.getInstalledDate())
						.addValue("isDeleted", app.isDeleted()))
				.toArray(MapSqlParameterSource[]::new);

		try {
		int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
		for (int i : status) {
			if (i == 0) {
				log.warn("Failed to insert computer application mapping, status: {}", i);
				return 0;
			}
		}
		log.debug("Successfully inserted {} computer applications mappings into the database", computerApplications.size());
		return 1;
		} catch (Exception e) {
			log.error("Error inserting computer applications: {}", e.getMessage());
			return 0;
		}
	}

	@Override
	public int deleteOrActivateComputerApplications(List<ComputerApplication> computerApplications) {
		log.debug("Updating {} computer applications mappings to set is_deleted in the database",
				computerApplications.size());
		String query = "UPDATE computer_applications SET is_deleted = :isDeleted WHERE uuid = :uuid";
		MapSqlParameterSource[] batchParams = computerApplications.stream().map(app -> new MapSqlParameterSource()
				.addValue("uuid", app.getUuid()).addValue("isDeleted", app.isDeleted()))
				.toArray(MapSqlParameterSource[]::new);
		try {
		int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
		for (int i : status) {
			if (i == 0) {
				log.warn("Failed to update computer application mapping, status: {}", i);
				return 0;
			}
		}
		return 1;
		} catch (Exception e) {
			log.error("Error updating computer applications: {}", e.getMessage());
			return 0;
		}
	}

	@Override
	public Map<String, Integer> getInstalledVulnerableAppCounts() {
		log.debug("Fetching counts of installed vulnerable applications by severity from the database");
		String query = "select v.severity, COUNT(distinct av.application_uuid) as app_count from computers c "
				+ "join computer_applications ca on ca.computer_uuid = c.uuid "
				+ "join application_vulnerabilities av  on av.application_uuid = ca.application_uuid "
				+ "join vulnerabilities v on v.uuid = av.vulnerability_uuid "
				+ "where c.is_active = true and c.is_deleted = false "
				+ "and ca.is_deleted = false "
				+ "group by v.severity "
				+ "order by v.severity";
		try {
			return jdbcTemplate.query(query, rs -> {
				Map<String, Integer> result = new HashMap<>();
				while(rs.next()) {
					String computerUuid = rs.getString("severity");
					Integer appCount = rs.getInt("app_count");
					result.put(computerUuid, appCount);
				}
				log.debug("Successfully fetched installed vulnerable application counts by severity");
				return result;
			});
		} catch (Exception e) {
			log.error("Error fetching installed vulnerable application counts: {}", e.getMessage());
			return Map.of();
		}
	}

	@Override
	public List<Application> getUnresolvedApplications() {
		log.debug("Fetching unresolved applications from the database");
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

}
