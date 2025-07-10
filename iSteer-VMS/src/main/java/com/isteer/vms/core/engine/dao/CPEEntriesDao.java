package com.isteer.vms.core.engine.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.isteer.vms.core.engine.model.CpeEntry;

@Repository
public class CPEEntriesDao {
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	public List<String> getDistinctVendorsList()  {
		String sql = "select distinct(vendor) from cpe_dictionary";
		 return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("vendor"));
	}

	/**
	 * public List<CpeEntryModel> getCpeEntriesForVendor(Connection con,
	 * List<String> vendors) throws SQLException { String sql = "SELECT id,
	 * cpe_name, cpe_title, vendor, product, version, update_date, deprecated FROM
	 * cpe_entries WHERE vendor IN ("; for (int i = 0; i < vendors.size(); i++) {
	 * sql += "'" + vendors.get(i) + "'"; if (i != vendors.size() - 1) { sql += ",";
	 * } } sql += ")"; // Engine.getMavenLog().info(sql); try (Statement st =
	 * con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
	 * List<CpeEntryModel> entries = new ArrayList<>(); while (rs.next()) {
	 * CpeEntryModel entry = new CpeEntryModel(); entry.setEntryId(rs.getInt(1));
	 * entry.setCpeName(rs.getString(2)); entry.setCpeTitle(rs.getString(3));
	 * entry.setVendor(rs.getString(4)); entry.setProduct(rs.getString(5));
	 * entry.setVersion(rs.getString(6));
	 * entry.setUpdatedDate(rs.getTimestamp(7).toLocalDateTime());
	 * entry.setDeprecated(rs.getBoolean(8)); entries.add(entry); } return entries;
	 * }
	 **/

	public List<CpeEntry> getCpeEntriesForVendor(Set<String> vendors) throws SQLException {
		 if (vendors == null || vendors.isEmpty()) {
	            return Collections.emptyList(); // Return empty if no vendors
	        }

		// Step 1: Create temporary table using jdbcTemplate
	        String dropTableSql = "DROP TABLE IF EXISTS temp_vendors";
	        String createTableSql = "CREATE TEMPORARY TABLE temp_vendors (vendors VARCHAR(255) PRIMARY KEY)";
	        jdbcTemplate.execute(dropTableSql);
	        jdbcTemplate.execute(createTableSql);

	     // Step 2: Insert vendors into temp_vendors table in a batch
	        String insertSql = "INSERT INTO temp_vendors (vendors) VALUES (?)";
	        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
	            @Override
	            public int getBatchSize() {
	                return vendors.size();
	            }

	            @Override
	            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
	                ps.setString(1, (String) vendors.toArray()[i]);
	            }
	        });

	     // Step 3: Execute the main JOIN query and map the result set to CpeEntryModel
	        String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
	                + "FROM cpe_dictionary c "
	                + "JOIN temp_vendors v ON c.vendor = v.vendors";

	        // Using JdbcTemplate to execute the query and map the result
	        return jdbcTemplate.query(sql, new CpeEntryRowMapper());
	}

	public List<CpeEntry> getAllCpeEntries() {
		String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
				+ "FROM cpe_dictionary c ";
		return jdbcTemplate.query(sql,new CpeEntryRowMapper());
	}

	public List<CpeEntry> getCpeEntries(int lastId) {
		String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated FROM cpe_dictionary WHERE id > ? ORDER BY id LIMIT 100000";
		return jdbcTemplate.query(sql, new CpeEntryRowMapper(),new Object[]{lastId});
	}
	
	public List<CpeEntry> getAllCpeEntriesWithOffset(int offset, int limit) throws SQLException  {
	    String sql = "SELECT id, cpe_name, cpe_title, vendor, product, version, update_date, deprecated "
	               + "FROM cpe_dictionary "
	               + "ORDER BY id "
	               + "LIMIT ? OFFSET ?";

	    return jdbcTemplate.query(sql, new CpeEntryRowMapper(),new Object[] {limit,offset});
	}
	
	class CpeEntryRowMapper implements RowMapper<CpeEntry>{

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
