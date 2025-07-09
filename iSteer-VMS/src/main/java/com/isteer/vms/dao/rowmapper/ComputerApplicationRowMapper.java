package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.ComputerApplication;

public class ComputerApplicationRowMapper implements RowMapper<ComputerApplication>{

	@Override
	public ComputerApplication mapRow(ResultSet rs, int rowNum) throws SQLException {
		ComputerApplication computerApplication = new ComputerApplication();
		computerApplication.setUuid(rs.getString("uuid"));
		computerApplication.setApplicationUuid(rs.getString("application_uuid"));
		computerApplication.setComputerUuid(rs.getString("computer_uuid"));
		computerApplication.setSoftwareName(rs.getString("name"));
		computerApplication.setSoftwareVersion(rs.getString("version"));
		computerApplication.setVendorName(rs.getString("vendor_name"));
		
		Timestamp installedAtTs = rs.getTimestamp("installed_date");
	    computerApplication.setCreatedAt(installedAtTs != null ? installedAtTs.toLocalDateTime() : null);
	    
	    computerApplication.setDeleted(rs.getBoolean("is_deleted"));
		computerApplication.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		
		Timestamp updatedAtTs = rs.getTimestamp("updated_at");
	    computerApplication.setCreatedAt(updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null);

		return computerApplication;
	}

}
