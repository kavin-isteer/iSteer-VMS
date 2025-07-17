package com.isteer.vms.core.engine.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.isteer.vms.core.engine.model.CpeEntry;

import lombok.extern.log4j.Log4j2;

@Repository
@Log4j2
public class CPEEntriesDao {

	private final JdbcTemplate jdbcTemplate;

	public CPEEntriesDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Retrieves a list of distinct vendors from the cpe_dictionary table.
	 *
	 * @return List of unique vendor names
	 */
	public List<String> getDistinctVendorsList() {
		String sql = "SELECT DISTINCT(vendor) FROM cpe_dictionary";
		try {
			return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("vendor"));
		} catch (DataAccessException e) {
			log.error("Failed to fetch distinct vendors list", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Fetches CPE entries for the given set of vendors by using a temporary table
	 * for filtering.
	 *
	 * @param vendors Set of vendor names to filter by
	 * @return List of CpeEntry objects matching the given vendors
	 */
	public List<CpeEntry> getCpeEntriesForVendor(Set<String> vendors) {
		if (vendors == null || vendors.isEmpty()) {
			return Collections.emptyList();
		}

		try {
			// Drop and create temporary table
			String dropTableSql = "DROP TABLE IF EXISTS temp_vendors";
			String createTableSql = "CREATE TEMPORARY TABLE temp_vendors (vendors VARCHAR(255) PRIMARY KEY)";
			jdbcTemplate.execute(dropTableSql);
			jdbcTemplate.execute(createTableSql);

			// Batch insert vendors
			String insertSql = "INSERT INTO temp_vendors (vendors) VALUES (?)";
			jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
				@Override
				public int getBatchSize() {
					return vendors.size();
				}

				@Override
				public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
					ps.setString(1, (String) vendors.toArray()[i]);
				}
			});

			// Query using join with temp_vendors
			String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
					+ "FROM cpe_dictionary c " + "JOIN temp_vendors v ON c.vendor = v.vendors";

			return jdbcTemplate.query(sql, new CpeEntryRowMapper());

		} catch (DataAccessException e) {
			log.error("Failed to fetch CPE entries for vendors", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves all CPE entries from the cpe_dictionary table.
	 *
	 * @return List of all CpeEntry records
	 */
	public List<CpeEntry> getAllCpeEntries() {
		String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
				+ "FROM cpe_dictionary";
		try {
			return jdbcTemplate.query(sql, new CpeEntryRowMapper());
		} catch (DataAccessException e) {
			log.error("Failed to fetch all CPE entries", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves CPE entries where the ID is greater than the given lastId, ordered
	 * by ID.
	 *
	 * @param lastId The last ID to compare against
	 * @return List of CpeEntry records with ID > lastId
	 */
	public List<CpeEntry> getCpeEntries(int lastId) {
		String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
				+ "FROM cpe_dictionary WHERE id > ? ORDER BY id LIMIT 100000";
		try {
			return jdbcTemplate.query(sql, new CpeEntryRowMapper(), lastId);
		} catch (DataAccessException e) {
			log.error("Failed to fetch CPE entries after ID: {}", lastId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Retrieves paginated CPE entries using offset and limit.
	 *
	 * @param offset Number of records to skip
	 * @param limit  Number of records to retrieve
	 * @return Paginated list of CpeEntry objects
	 */
	public List<CpeEntry> getAllCpeEntriesWithOffset(int offset, int limit) {
		String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
				+ "FROM cpe_dictionary ORDER BY id LIMIT ? OFFSET ?";
		try {
			return jdbcTemplate.query(sql, new CpeEntryRowMapper(), limit, offset);
		} catch (DataAccessException e) {
			log.error("Failed to fetch CPE entries with offset={} and limit={}", offset, limit, e);
			return Collections.emptyList();
		}
	}

	/**
	 * RowMapper implementation for mapping ResultSet rows to CpeEntry objects.
	 */
	class CpeEntryRowMapper implements RowMapper<CpeEntry> {
		@Override
		public CpeEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
			CpeEntry entry = new CpeEntry();
			entry.setEntryId(rs.getInt("id"));
			entry.setCpeName(rs.getString("cpe_name"));
			entry.setCpeTitle(rs.getString("cpe_title"));
			entry.setVendor(rs.getString("vendor"));
			entry.setProduct(rs.getString("product"));
			entry.setVersion(rs.getString("version"));
			entry.setUpdatedDate(rs.getTimestamp("update_date").toLocalDateTime());
			entry.setDeprecated(rs.getBoolean("deprecated"));
			return entry;
		}
	}
}
