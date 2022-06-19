package de.redfox.redfix.utils;

import java.sql.*;
import java.util.*;

public class RfSql {
	
	Connection connection;
	
	String host;
	int port;
	String username;
	String password;
	String database;
	String prefix;
	
	public RfSql(String host, int port, String username, String password, String database, String prefix) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
		this.prefix = prefix;
	}
	
	public void connect() throws SQLException {
		if (isConnected())
			disconnect();
		String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?serverTimezone=UTC";
		connection = DriverManager.getConnection(url, username, password);
	}
	
	public void disconnect() throws SQLException {
		if (isConnected())
			connection.close();
		connection = null;
	}
	
	public boolean isConnected() throws SQLException {
		return connection != null && !connection.isClosed();
	}
	
	public ResultSet select(String table) throws SQLException {
		if (!isConnected())
			connect();
		String statement = "SELECT * FROM " + prefix + table;
		PreparedStatement ps = connection.prepareStatement(statement);
		return ps.executeQuery();
	}
	
	public ResultSet select(String table, Map<String, String> conditions) throws SQLException {
		if (!isConnected())
			connect();
		StringBuilder statement = new StringBuilder("SELECT * FROM " + prefix + table + " WHERE ");
		Iterator<Map.Entry<String, String>> conditionIterator = conditions.entrySet().iterator();
		while (conditionIterator.hasNext()) {
			Map.Entry<String, String> entry = conditionIterator.next();
			statement.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
			if (conditionIterator.hasNext())
				statement.append(" AND ");
		}
		PreparedStatement ps = connection.prepareStatement(statement.toString());
		return ps.executeQuery();
	}
	
	public boolean insertInto(String table, Map<String, String> data) throws SQLException {
		if (!isConnected())
			connect();
		StringBuilder cols = new StringBuilder("(");
		StringBuilder values = new StringBuilder("(");
		
		Iterator<Map.Entry<String, String>> dataIterator = data.entrySet().iterator();
		while (dataIterator.hasNext()) {
			Map.Entry<String, String> entry = dataIterator.next();
			cols.append(entry.getKey());
			values.append("'").append(entry.getValue()).append("'");
			if (dataIterator.hasNext()) {
				cols.append(", ");
				values.append(", ");
			}
		}
		cols.append(")");
		values.append(")");
		String statement = "INSERT INTO " + prefix + table + " " + cols + " VALUES " + values;
		PreparedStatement ps = connection.prepareStatement(statement);
		return ps.execute();
	}
	
	public boolean update(String table, Map<String, String> data, Map<String, String> conditions) throws SQLException {
		if (!isConnected())
			connect();
		
		StringBuilder statement = new StringBuilder("UPDATE " + prefix + table + " SET ");
		Iterator<Map.Entry<String, String>> dataIterator = data.entrySet().iterator();
		while (dataIterator.hasNext()) {
			Map.Entry<String, String> entry = dataIterator.next();
			statement.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
			if (dataIterator.hasNext()) {
				statement.append(", ");
			}
		}
		statement.append(" WHERE ");
		Iterator<Map.Entry<String, String>> conditionIterator = conditions.entrySet().iterator();
		while (conditionIterator.hasNext()) {
			Map.Entry<String, String> entry = conditionIterator.next();
			statement.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
			if (conditionIterator.hasNext())
				statement.append(" AND ");
		}
		PreparedStatement ps = connection.prepareStatement(statement.toString());
		return ps.execute();
	}
	
	public boolean delete(String table, Map<String, String> conditions) throws SQLException {
		if (!isConnected())
			connect();
		StringBuilder statement = new StringBuilder("DELETE FROM " + prefix + table + " WHERE ");
		Iterator<Map.Entry<String, String>> conditionIterator = conditions.entrySet().iterator();
		while (conditionIterator.hasNext()) {
			Map.Entry<String, String> entry = conditionIterator.next();
			statement.append(entry.getKey()).append("='").append(entry.getValue()).append("'");
			if (conditionIterator.hasNext())
				statement.append(" AND ");
		}
		PreparedStatement ps = connection.prepareStatement(statement.toString());
		return ps.execute();
	}
	
	public boolean createTable(String table, List<ColumnData> cols, String primaryKey) throws SQLException {
		if (!isConnected())
			connect();
		ListIterator<ColumnData> colIterator = cols.listIterator();
		StringBuilder statement = new StringBuilder("CREATE TABLE IF NOT EXISTS " + prefix + table + " (");
		while (colIterator.hasNext()) {
			statement.append(colIterator.next());
			if (colIterator.hasNext())
				statement.append(", ");
		}
		if (primaryKey != null)
			statement.append(", ").append("PRIMARY KEY (").append(primaryKey).append(")");
		statement.append(")");
		PreparedStatement ps = connection.prepareStatement(statement.toString());
		return ps.execute();
	}
	
	public boolean createIndex(String table, String key, boolean unique, Set<String> cols) throws SQLException {
		if (!isConnected())
			connect();
		StringBuilder statement = new StringBuilder("CREATE ");
		if (unique)
			statement.append("UNIQUE ");
		statement.append("INDEX ").append(key).append(" ON ").append(prefix).append(table).append(" (");
		Iterator<String> colIterator = cols.iterator();
		while (colIterator.hasNext()) {
			statement.append(colIterator.next());
			if (colIterator.hasNext())
				statement.append(", ");
		}
		statement.append(")");
		PreparedStatement ps = connection.prepareStatement(statement.toString());
		return ps.execute();
	}
	
	public static class ColumnData {
		
		public String name;
		public String type;
		public boolean notNull;
		public boolean unique;
		public String defaultValue = null;
		public boolean autoIncrement;
		
		public ColumnData(String name, String type) {
			this.name = name;
			this.type = type;
		}
		
		public ColumnData setAutoIncrement(boolean autoIncrement) {
			this.autoIncrement = autoIncrement;
			return this;
		}
		
		public ColumnData setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}
		
		public ColumnData setNotNull(boolean notNull) {
			this.notNull = notNull;
			return this;
		}
		
		public ColumnData setUnique(boolean unique) {
			this.unique = unique;
			return this;
		}
		
		public ColumnData setAutoIncrement() {
			this.autoIncrement = true;
			return this;
		}
		
		public ColumnData setNotNull() {
			this.notNull = true;
			return this;
		}
		
		public ColumnData setUnique() {
			this.unique = true;
			return this;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(name + " " + type + " ");
			if (notNull)
				sb.append("NOT NULL ");
			if (unique)
				sb.append("UNIQUE ");
			if (autoIncrement)
				sb.append("AUTO_INCREMENT ");
			if (defaultValue != null)
				sb.append("DEFAULT '").append(defaultValue).append("' ");
			return sb.toString();
		}
		
	}
	
}
