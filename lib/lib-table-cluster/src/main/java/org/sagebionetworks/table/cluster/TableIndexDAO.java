package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * This is an abstraction for table index CRUD operations.
 * @author John
 *
 */
public interface TableIndexDAO {
	
	/**
	 * Create or update a table with the given schema.
	 * 
	 * @param connection
	 * @param schema
	 * @param tableId
	 */
	public boolean createOrUpdateTable(List<ColumnModel> schema, String tableId);
	
	/**
	 * 
	 * @param connection
	 * @param tableId
	 * @return
	 */
	public boolean deleteTable(String tableId); 
	
	/**
	 * Get the current columns of a table in the passed database connection.
	 * @param tableId
	 * @return
	 */
	public List<String> getCurrentTableColumns(String tableId);
	
	/**
	 * Create or update the rows passed in the given RowSet.
	 * 
	 * Note: The passed RowSet is not required to match the current schema.
	 * Columns in the Rowset that are not part of the current schema will be ignored.
	 * Columns in the current schema that are not part of the RowSet will be set to
	 * the default value of the column.
	 * @param connection
	 * @param rowset
	 * @return
	 */
	int[] createOrUpdateRows(RowSet rowset, List<ColumnModel> currentSchema);
	
	/**
	 * Query a RowSet from the table.
	 * @param query
	 * @return
	 */
	public RowSet query(SqlQuery query);
	
	/**
	 * Get the row count for this table.
	 * 
	 * @param tableId
	 * @return The row count of the table. If the table does not exist then null.
	 */
	public Long getRowCountForTable(String tableId);
	
	/**
	 * Get the max version we currently have for this table.
	 * 
	 * @param tableId
	 * @return The max version of the table. If the table does not exist then null.
	 */
	public Long getMaxVersionForTable(String tableId);
	
	/**
	 * Get the connection
	 * @return
	 */
	public SimpleJdbcTemplate getConnection();
}
