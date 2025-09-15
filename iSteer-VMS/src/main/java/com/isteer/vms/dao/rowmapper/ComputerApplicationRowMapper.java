package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isteer.vms.model.ComputerApplication;

public class ComputerApplicationRowMapper implements RowMapper<ComputerApplication>{
	
	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public ComputerApplication mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		 String processIdsJson = rs.getString("process_ids"); // Assuming this is the column name
	        List<Integer> processIds = new ArrayList<>();

	        if (processIdsJson != null && !processIdsJson.isBlank()) {
	            try {
	                processIds = objectMapper.readValue(processIdsJson, new TypeReference<List<Integer>>() {});
	            } catch (Exception e) {
	                throw new SQLException("Failed to parse process_ids JSON: " + processIdsJson, e);
	            }
	        }
		return ComputerApplication.builder()
				.uuid(rs.getString("uuid"))
				.applicationUuid(rs.getString("application_uuid"))
				.computerUuid(rs.getString("computer_uuid"))
				.softwareName(rs.getString("name"))
				.softwareVersion(rs.getString("version"))
				.vendorName(rs.getString("vendor_name"))
				.installedDate(toLocalDateTime(rs.getTimestamp("installed_date")))
				.processIds(processIds)
				.isDeleted(rs.getBoolean("is_deleted"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
				.build();
	}
	private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
	}
