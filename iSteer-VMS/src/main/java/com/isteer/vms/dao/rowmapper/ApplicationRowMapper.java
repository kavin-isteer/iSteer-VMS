package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.Application;

public class ApplicationRowMapper implements RowMapper<Application>{

	@Override
	public Application mapRow(ResultSet rs, int rowNum) throws SQLException {
		return Application.builder()
				.id(rs.getLong("id"))
				.uuid(rs.getString("uuid"))
				.softwareName(rs.getString("name"))
				.softwareVersion(rs.getString("version"))
				.vendorName(rs.getString("vendor_name"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.build();
	}

	private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
	        return ts != null ? ts.toLocalDateTime() : null;
	    }
	}
