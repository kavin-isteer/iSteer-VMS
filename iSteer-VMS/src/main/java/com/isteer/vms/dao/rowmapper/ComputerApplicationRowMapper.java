package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.ComputerApplication;

public class ComputerApplicationRowMapper implements RowMapper<ComputerApplication>{

	@Override
	public ComputerApplication mapRow(ResultSet rs, int rowNum) throws SQLException {
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
