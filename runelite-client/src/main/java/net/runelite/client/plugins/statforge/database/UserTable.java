package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class UserTable implements ITable {
	TrackerDatabase database;
	
	public UserTable ( TrackerDatabase database ) {
		this.database = database;
	}
	
	@Override
	public void create () {
		try ( Statement stmt = database.sqlite.connection.createStatement() ) {
			stmt.execute(
					"create table if not exists users\n" +
							"(\n" +
							"    uuid     text\n" +
							"        constraint users_pk\n" +
							"            primary key,\n" +
							"    username text\n" +
							");"
			);
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	public void track ( long accountHash, String username ) {
		var uuid = Long.toHexString( accountHash );
		
		try ( PreparedStatement stmt = database.sqlite.connection.prepareStatement(
				"insert into users( uuid, username ) VALUES( ?, ? )" +
						"on conflict( uuid ) do update set username = ?;"
		) ) {
			stmt.setString( 1, uuid );
			stmt.setString( 2, username );
			stmt.setString( 3, username );
			stmt.execute();
			log.info( "Tracking user " + username );
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
}
