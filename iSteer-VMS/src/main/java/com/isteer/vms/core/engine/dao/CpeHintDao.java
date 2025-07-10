package com.isteer.vms.core.engine.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.isteer.vms.core.engine.enums.HintAddedBy;
import com.isteer.vms.core.engine.model.CpeHint;

@Repository
public class CpeHintDao {
	@Autowired
	JdbcTemplate jdbcTemplate;

	public List<CpeHint> getAllCpeHints(JdbcTemplate jdbcTemplate) {
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at,evidence_type, addedBy FROM dependency_hints";
		return jdbcTemplate.query(query, new CpeHintRowMapper());
	}

	public List<CpeHint> getAllVendorCpeHints(String evidenceType) {
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at,evidence_type, addedBy FROM dependency_hints WHERE type = ? AND evidence_type = ?";
		return jdbcTemplate.query(query,new CpeHintRowMapper(),  new Object[] { "vendor", evidenceType });
	}
	
	public List<CpeHint> getAllVendorCpeHints() {
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at,evidence_type, addedBy FROM dependency_hints WHERE type = ?";
		return jdbcTemplate.query(query,new CpeHintRowMapper(),  new Object[] { "vendor"});
	}
	
	public List<CpeHint> getAllProductrCpeHints(String evidenceType) {
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at,evidence_type, addedBy FROM dependency_hints WHERE type = ? AND evidence_type = ?";
		return jdbcTemplate.query(query,new CpeHintRowMapper(),  new Object[] { "product", evidenceType });
	}
}

class CpeHintRowMapper implements RowMapper<CpeHint> {
	@Override
	public CpeHint mapRow(ResultSet rs, int rowNum) throws SQLException {
		CpeHint hint = new CpeHint();
		hint.setId(rs.getInt("id"));
		hint.setType(rs.getString("type"));
		hint.setMatch_key(rs.getString("match_key"));
		hint.setStandardized_name(rs.getString("standardized_name"));
		hint.setConfidence(rs.getString("confidence"));
		hint.setDescription(rs.getString("description"));

		if (rs.getTimestamp("created_at") != null) {
			hint.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
		}

		if (rs.getTimestamp("updated_at") != null) {
			hint.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
		}
		hint.setEvidenceType(rs.getString("evidence_type"));
		hint.setAddedBy(HintAddedBy.fromId(rs.getInt("addedBy")));
		return hint;
	}

}
