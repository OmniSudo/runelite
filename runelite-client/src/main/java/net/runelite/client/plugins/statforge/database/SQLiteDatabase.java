package net.runelite.client.plugins.statforge.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabase {
	public Connection connection = null;
	
	public SQLiteDatabase ( File file ) {
		String url = "jdbc:sqlite:" + file.getAbsolutePath();
		try {
			connection = DriverManager.getConnection( url );
		} catch ( SQLException e ) {
			throw new RuntimeException( e );
		}
	}
	
	
}
