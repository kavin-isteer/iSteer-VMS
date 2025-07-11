package com.isteer.vms.dao.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.isteer.vms.dao.ApplicationDao;
import com.isteer.vms.dao.rowmapper.ApplicationRowMapper;
import com.isteer.vms.dao.rowmapper.ComputerApplicationRowMapper;
import com.isteer.vms.model.Application;
import com.isteer.vms.model.ComputerApplication;

@Repository
public class ApplicationDaoImpl implements ApplicationDao{
	
	private static final Logger logger = LogManager.getLogger(ApplicationDaoImpl.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public List<Application> getAllApplications() {
		logger.debug("Fetching all applications from the database");
		String query = "SELECT id, uuid, name, version, vendor_name, created_at FROM applications";
		return jdbcTemplate.query(query, new ApplicationRowMapper());
	}

	@Override
	public List<ComputerApplication> getApplicationsByComputerUuid(String computerUuid) {
		logger.debug("Fetching applications for computer UUID: {}", computerUuid);
		String query = "SELECT ca.uuid, ca.application_uuid, ca.computer_uuid, a.name, a.version, a.vendor_name, ca.installed_date, ca.is_deleted, ca.created_at, ca.updated_at "
				+ "FROM computer_applications ca "
				+ "JOIN applications a ON ca.application_uuid = a.uuid "
				+ "WHERE ca.computer_uuid = :computerUuid";
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("computerUuid", computerUuid);
		try {
			return namedParameterJdbcTemplate.query(query, params, new ComputerApplicationRowMapper());
		} catch (Exception e) {
			e.printStackTrace();
			// Handle exception or log it
		}
		return null;
	}

	@Override
	public int insertApplications(List<Application> applications) {
		logger.debug("Inserting {} applications into the database", applications.size());
	    String query = "INSERT IGNORE INTO applications (uuid, name, version, vendor_name) " +
	                   "VALUES (:uuid, :name, :version, :vendorName)";

	    MapSqlParameterSource[] batchParams = applications.stream()
	        .map(app -> new MapSqlParameterSource()
	            .addValue("uuid", UUID.randomUUID().toString())
	            .addValue("name", app.getSoftwareName())
	            .addValue("version", app.getSoftwareVersion())
	            .addValue("vendorName", app.getVendorName()))
	        .toArray(MapSqlParameterSource[]::new);

	   int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
	   for (int i : status) {
		   if (i == 0) {
			   logger.warn("Failed to insert application, status: {}", i);
			   return 0;
		   }
	   }
	   return 1;
	}

	@Override
	public int insertComputerApplications(List<ComputerApplication> computerApplications) {
		logger.debug("Inserting {} computer applications mappings into the database", computerApplications.size());
		String query = "INSERT IGNORE INTO computer_applications (uuid, application_uuid, computer_uuid, installed_date, is_deleted) " +
				"VALUES (:uuid, :applicationUuid, :computerUuid, :installedAt, :isDeleted)";
		MapSqlParameterSource[] batchParams = computerApplications.stream()
				.map(app -> new MapSqlParameterSource()
						.addValue("uuid", app.getUuid())
						.addValue("applicationUuid", app.getApplicationUuid())
						.addValue("computerUuid", app.getComputerUuid())
						.addValue("installedAt", app.getInstalledDate())
						.addValue("isDeleted", app.isDeleted()))
				.toArray(MapSqlParameterSource[]::new);
		
		int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
		for (int i : status) {
			   if (i == 0) {
				   logger.warn("Failed to insert computer application mapping, status: {}", i);
				   return 0;
			   }
		   }
		   return 1;
	}

	@Override
	public int deleteOrActivateComputerApplications(List<ComputerApplication> computerApplications) {
		logger.debug("Updating {} computer applications mappings to set is_deleted in the database", computerApplications.size());
		String query = "UPDATE computer_applications SET is_deleted = :isDeleted WHERE uuid = :uuid";
		MapSqlParameterSource[] batchParams = computerApplications.stream()
				.map(app -> new MapSqlParameterSource()
						.addValue("uuid", app.getUuid())
						.addValue("isDeleted", app.isDeleted()))
				.toArray(MapSqlParameterSource[]::new);
		int[] status = namedParameterJdbcTemplate.batchUpdate(query, batchParams);
		for (int i : status) {
			if (i == 0) {
				logger.warn("Failed to update computer application mapping, status: {}", i);
				return 0;
			}
		}
		return 1;
	}

}
