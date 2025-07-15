package com.isteer.vms.dao.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;

import com.isteer.vms.model.ApplicationCpeName;

public class ApplicationCpeNameRowMapper implements RowMapper<ApplicationCpeName>{

	@Override
	public ApplicationCpeName mapRow(ResultSet rs, int rowNum) throws SQLException {
		return ApplicationCpeName.builder()
				.uuid(rs.getString("uuid"))
				.applicationUuid(rs.getString("application_uuid"))
				.cpeName(rs.getString("cpe_name"))
				.isResolvedCpe(rs.getBoolean("is_resolved_cpe"))
				.createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
				.updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
				.build();
	}
	
	private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

}
