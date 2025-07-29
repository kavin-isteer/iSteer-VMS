package com.isteer.vms.core.engine.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.isteer.vms.core.engine.enums.HintAddedBy;
import com.isteer.vms.core.engine.model.CpeHint;

import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class CpeHintDao {

	private final NamedParameterJdbcTemplate namedJdbcTemplate;

	public CpeHintDao(NamedParameterJdbcTemplate namedJdbcTemplate) {
		this.namedJdbcTemplate = namedJdbcTemplate;
	}

	/**
	 * Retrieves all CPE hints.
	 */
	public List<CpeHint> getAllCpeHints() {
		log.info("Retrieving all CPE hints from the database");
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at, evidence_type, addedBy FROM cpe_hints";
		try {
			return namedJdbcTemplate.query(query, new MapSqlParameterSource(), new CpeHintRowMapper());
		} catch (DataAccessException e) {
			log.error("Failed to retrieve all CPE hints with error message: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves all vendor CPE hints filtered by evidence type.
	 */
	public List<CpeHint> getAllVendorCpeHints(String evidenceType) {
		log.info("Retrieving vendor CPE hints for evidence type: {}", evidenceType);
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at, evidence_type, addedBy "
				+ "FROM cpe_hints WHERE type = :type AND evidence_type = :evidenceType";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("type", "vendor").addValue("evidenceType",
				evidenceType);

		try {
			return namedJdbcTemplate.query(query, params, new CpeHintRowMapper());
		} catch (DataAccessException e) {
			log.error("Failed to retrieve vendor CPE hints for evidenceType: {} with error message: {}", evidenceType, e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves all vendor CPE hints.
	 */
	public List<CpeHint> getAllVendorCpeHints() {
		log.info("Retrieving all vendor CPE hints from the database");
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at, evidence_type, addedBy "
				+ "FROM cpe_hints WHERE type = :type";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("type", "vendor");

		try {
			return namedJdbcTemplate.query(query, params, new CpeHintRowMapper());
		} catch (DataAccessException e) {
			log.error("Failed to retrieve all vendor CPE hints with error message: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves all product CPE hints filtered by evidence type.
	 */
	public List<CpeHint> getAllProductCpeHints(String evidenceType) {
		log.info("Retrieving product CPE hints for evidence type: {}", evidenceType);
		String query = "SELECT id, type, match_key, standardized_name, confidence, description, created_at, updated_at, evidence_type, addedBy "
				+ "FROM cpe_hints WHERE type = :type AND evidence_type = :evidenceType";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("type", "product").addValue("evidenceType",
				evidenceType);

		try {
			return namedJdbcTemplate.query(query, params, new CpeHintRowMapper());
		} catch (DataAccessException e) {
			log.error("Failed to retrieve product CPE hints for evidence type: {}, with error message: {}", evidenceType, e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Adds or updates a dependency hint in the database.
	 */
	public int addDependencyHint(CpeHint hint) {
		log.info("Adding or updating hint for matchKey: {}", hint.getMatchKey());
		String checkQuery = "SELECT COUNT(*) FROM cpe_hints WHERE type = :type AND match_key = :matchKey AND evidence_type = :evidenceType";
		String insertQuery = "INSERT INTO cpe_hints (type, match_key, standardized_name, confidence, description, evidence_type, addedBy) "
				+ "VALUES (:type, :matchKey, :standardizedName, :confidence, :description, :evidenceType, :addedBy)";
		String updateQuery = "UPDATE cpe_hints SET standardized_name = :standardizedName, confidence = :confidence, "
				+ "description = :description, evidence_type = :evidenceType, addedBy = :addedBy "
				+ "WHERE type = :type AND match_key = :matchKey";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("type", hint.getType())
				.addValue("matchKey", hint.getMatchKey()).addValue("evidenceType", hint.getEvidenceType())
				.addValue("standardizedName", hint.getStandardizedName()).addValue("confidence", hint.getConfidence())
				.addValue("description", hint.getDescription()).addValue("addedBy", hint.getAddedBy().getId());

		try {
			Integer count = namedJdbcTemplate.queryForObject(checkQuery, params, Integer.class);
			if (count != null && count > 0) {
				log.debug("Updating existing hint for matchkey: {}", hint.getMatchKey());
				return namedJdbcTemplate.update(updateQuery, params);
			} else {
				log.debug("Inserting new hint for matchKey: {}", hint.getMatchKey());
				return namedJdbcTemplate.update(insertQuery, params);
			}
		} catch (DataAccessException e) {
			log.error("Failed to add or update dependency hint for matchKey: {}, with error message: {}", hint.getMatchKey(), e.getMessage());
			return 0;
		}
	}

	/**
	 * Retrieves the standardized name for a given match key and type.
	 */
	public String getStandardisedNameForMatchKey(String matchKey, String type) {
		log.info("Retrieving standardized name for matchkey: {} and type: {}", matchKey, type);
		String query = "SELECT standardized_name FROM cpe_hints WHERE match_key = :matchKey AND evidence_type = :evidenceType AND type = :type";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("matchKey", matchKey)
				.addValue("evidenceType", "APPLICATION").addValue("type", type);

		try {
			return namedJdbcTemplate.queryForObject(query, params, String.class);
		} catch(IncorrectResultSizeDataAccessException e) {
			log.error("Multiple standardized names found for matchKey={} and type={}. Returning null.", matchKey, type);
			return null;
		} catch (BadSqlGrammarException e) {
			log.error("SQL syntax error while retrieving standardized name for matchkey={} and type={}, with error message: {}", matchKey, type, e.getMessage());
			return null;
		} catch (DataAccessException e) {
			log.warn("No standardized name found for matchKey={} and type={}", matchKey, type);
			return null;
		}
	}

	/**
	 * RowMapper implementation for CpeHint entity.
	 */
	static class CpeHintRowMapper implements RowMapper<CpeHint> {
		@Override
		public CpeHint mapRow(ResultSet rs, int rowNum) throws SQLException {
			CpeHint hint = new CpeHint();
			hint.setId(rs.getInt("id"));
			hint.setType(rs.getString("type"));
			hint.setMatchKey(rs.getString("match_key"));
			hint.setStandardizedName(rs.getString("standardized_name"));
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
}
