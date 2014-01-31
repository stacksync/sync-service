package com.stacksync.syncservice.db;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.Workspace;

/**
 * Utility class for DAO's. This class contains commonly used DAO logic which is
 * been refactored in single static methods. As far it contains a
 * PreparedStatement values setter and several quiet close methods.
 * 
 * @author Adrian Moreno (adrian [at] morenomartinez [dot] com)
 */
public final class DAOUtil {

	// Constructors
	// -------------------------------------------------------------------------------

	private DAOUtil() {
		// Utility class, hide constructor.
	}

	// Actions
	// ------------------------------------------------------------------------------------

	/**
	 * Returns a PreparedStatement of the given connection, set with the given
	 * SQL query and the given parameter values.
	 * 
	 * @param connection
	 *            The Connection to create the PreparedStatement from.
	 * @param sql
	 *            The SQL query to construct the PreparedStatement with.
	 * @param returnGeneratedKeys
	 *            Set whether to return generated keys or not.
	 * @param values
	 *            The parameter values to be set in the created
	 *            PreparedStatement.
	 * @throws SQLException
	 *             If something fails during creating the PreparedStatement.
	 */
	public static PreparedStatement prepareStatement(Connection connection, String sql, boolean returnGeneratedKeys,
			Object... values) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(sql,
				returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
		setValues(preparedStatement, values);
		return preparedStatement;
	}

	/**
	 * Set the given parameter values in the given PreparedStatement.
	 * 
	 * @param connection
	 *            The PreparedStatement to set the given parameter values in.
	 * @param values
	 *            The parameter values to be set in the created
	 *            PreparedStatement.
	 * @throws SQLException
	 *             If something fails during setting the PreparedStatement
	 *             values.
	 */
	public static void setValues(PreparedStatement preparedStatement, Object... values) throws SQLException {
		for (int i = 0; i < values.length; i++) {
			preparedStatement.setObject(i + 1, values[i]);
		}
	}

	/**
	 * Converts the given java.util.Date to java.sql.Date.
	 * 
	 * @param date
	 *            The java.util.Date to be converted to java.sql.Date.
	 * @return The converted java.sql.Date.
	 */
	public static Date toSqlDate(java.util.Date date) {
		return (date != null) ? new Date(date.getTime()) : null;
	}

	/**
	 * Quietly close the Connection. Any errors will be printed to the stderr.
	 * 
	 * @param connection
	 *            The Connection to be closed quietly.
	 */
	public static void close(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				System.err.println("Closing Connection failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Quietly close the Statement. Any errors will be printed to the stderr.
	 * 
	 * @param statement
	 *            The Statement to be closed quietly.
	 */
	public static void close(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				System.err.println("Closing Statement failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Quietly close the ResultSet. Any errors will be printed to the stderr.
	 * 
	 * @param resultSet
	 *            The ResultSet to be closed quietly.
	 */
	public static void close(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				System.err.println("Closing ResultSet failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Quietly close the Connection and Statement. Any errors will be printed to
	 * the stderr.
	 * 
	 * @param connection
	 *            The Connection to be closed quietly.
	 * @param statement
	 *            The Statement to be closed quietly.
	 */
	public static void close(Connection connection, Statement statement) {
		close(statement);
		close(connection);
	}

	/**
	 * Quietly close the Connection, Statement and ResultSet. Any errors will be
	 * printed to the stderr.
	 * 
	 * @param connection
	 *            The Connection to be closed quietly.
	 * @param statement
	 *            The Statement to be closed quietly.
	 * @param resultSet
	 *            The ResultSet to be closed quietly.
	 */
	public static void close(Connection connection, Statement statement, ResultSet resultSet) {
		close(resultSet);
		close(statement);
		close(connection);
	}

	/**
	 * Quietly close Statement and ResultSet. Any errors will be printed to the
	 * stderr.
	 * 
	 * @param statement
	 *            The Statement to be closed quietly.
	 * @param resultSet
	 *            The ResultSet to be closed quietly.
	 */
	public static void close(Statement statement, ResultSet resultSet) {
		close(resultSet);
		close(statement);
	}

	/**
	 * Returns the Long value corresponding to the given field on the given
	 * ResultSet. If the value cannot be parsed to Long, or does not exist, it
	 * returns a null value.
	 * 
	 * @param rs
	 *            ResultSet
	 * @param field
	 *            Field we want to obtain the value from
	 * @return Long value if the field exists and can be parsed to Long. Null otherwise.
	 */
	public static Long getLongFromResultSet(ResultSet rs, String field) {

		Long result = null;

		try {
			Object value = rs.getObject(field);
			if (value != null) {
				result = (Long) value;
			}
		} catch (Exception e) {
		}
		return result;
	}
	
	public static Integer getIntFromResultSet(ResultSet rs, String field) {

		Integer result = null;

		try {
			Object value = rs.getObject(field);
			if (value != null) {
				result = (Integer) value;
			}
		} catch (Exception e) {
		}
		return result;
	}

	
	public static ItemMetadata getItemMetadataFromResultSet(ResultSet result)
			throws SQLException {

		ItemMetadata metadata = new ItemMetadata();
		metadata.setId(getLongFromResultSet(result, "item_id"));
		metadata.setParentId(getLongFromResultSet(result, "parent_id"));
		metadata.setParentVersion((getLongFromResultSet(result, "client_parent_file_version")));
		metadata.setDeviceId(result.getLong("device_id"));
		metadata.setFilename(result.getString("filename"));
		metadata.setVersion(result.getLong("version"));
		metadata.setIsFolder(result.getBoolean("is_folder"));
		metadata.setStatus(result.getString("status"));
		metadata.setMimetype(result.getString("mimetype"));
		metadata.setChecksum(result.getLong("checksum"));
		metadata.setSize(result.getLong("size"));
		metadata.setPath(result.getString("path"));
		metadata.setModifiedAt(result.getTimestamp("modified_at"));
		
		metadata.setLevel(getIntFromResultSet(result, "level"));

		if (!metadata.isFolder()) {
			Array arrayChunks = result.getArray("chunks");
			String[] chunks = (String[]) arrayChunks.getArray();

			List<String> chunksList = Arrays.asList(chunks);
			if (chunksList.contains(null)) {
				// FIXME: In this case the list contains a null parameter.
				// If this happens "parseItemMetadata" function from
				// JSONReader fails.
				chunksList = new ArrayList<String>();
			}
			metadata.setChunks(chunksList);
		}

		return metadata;
	}
	
	public static Chunk getChunkFromResultSet(ResultSet result) throws SQLException {

		Chunk chunk = new Chunk();
		chunk.setOrder(result.getInt("chunk_order"));
		chunk.setClientChunkName(result.getString("client_chunk_name"));

		return chunk;

	}
	
	public static Item getItemFromResultSet(ResultSet result) throws SQLException {

		Item item = new Item();
		item.setId(result.getLong("id"));
		item.setLatestVersion(result.getLong("latest_version"));
		item.setFilename(result.getString("filename"));
		item.setMimetype(result.getString("mimetype"));
		item.setIsFolder(result.getBoolean("is_folder"));

		Workspace w = new Workspace();
		w.setId(result.getLong("workspace_id"));
		item.setWorkspace(w);

		Item parent = new Item();
		parent.setId(result.getLong("parent_id"));
		item.setParent(parent);

		return item;
	}
	
}