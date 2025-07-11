package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.ComputerApplication;

public class ComputerApplicationRowMapper implements RowMapper<ComputerApplication>{

	@Override
	public ComputerApplication mapRow(ResultSet rs, int rowNum) throws SQLException {
//		ComputerApplication computerApplication = new ComputerApplication();
//		computerApplication.setUuid(rs.getString("uuid"));
//		computerApplication.setApplicationUuid(rs.getString("application_uuid"));
//		computerApplication.setComputerUuid(rs.getString("computer_uuid"));
//		computerApplication.setSoftwareName(rs.getString("name"));
//		computerApplication.setSoftwareVersion(rs.getString("version"));
//		computerApplication.setVendorName(rs.getString("vendor_name"));
//		
//		Timestamp installedAtTs = rs.getTimestamp("installed_date");
//	    computerApplication.setCreatedAt(installedAtTs != null ? installedAtTs.toLocalDateTime() : null);
//	    
//	    computerApplication.setDeleted(rs.getBoolean("is_deleted"));
//		computerApplication.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
//		
//		Timestamp updatedAtTs = rs.getTimestamp("updated_at");
//	    computerApplication.setCreatedAt(updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null);
//
//		return computerApplication;
		
		return ComputerApplication.builder()
				.uuid(rs.getString("uuid"))
				.applicationUuid(rs.getString("application_uuid"))
				.computerUuid(rs.getString("computer_uuid"))
				.softwareName(rs.getString("name"))
				.softwareVersion(rs.getString("version"))
				.vendorName(rs.getString("vendor_name"))
				.installedDate(toLocalDateTime(rs.getTimestamp("installed_date")))
				.isDeleted(rs.getBoolean("is_deleted"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
				.build();
	}
	private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
	}
