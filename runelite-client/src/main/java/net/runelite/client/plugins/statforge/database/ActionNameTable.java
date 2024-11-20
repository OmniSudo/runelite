package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

@Slf4j
public class ActionNameTable implements ITable {
	TrackerDatabase database;
	
	public ActionNameTable ( TrackerDatabase database ) {
		this.database = database;
	}
	
	@Override
	public void create () {
		try ( Statement stmt = database.sqlite.connection.createStatement() ) {
			stmt.execute(
					"create table if not exists action_names\n" +
							"(\n" +
							"    id         integer,\n" +
							"    name        text,\n" +
							"    constraint actions_names_pk\n" +
							"        primary key (id)\n" +
							");"
			);
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	
	public int get ( String name ) {
		int id = -1;
		
		try ( PreparedStatement try_get_type_name = database.sqlite.connection.prepareStatement(
				"SELECT id FROM action_names WHERE name = ?;"
		) ) {
			try_get_type_name.setString( 1, name );
			var result = try_get_type_name.executeQuery();
			
			if ( result.next() ) {
				id = result.getInt( "id" );
			} else {
				id = name.hashCode();
			}
			
			PreparedStatement upsert_name = database.sqlite.connection.prepareStatement(
					"INSERT INTO action_names ( id, name ) VALUES( ?, ? )" +
							"ON CONFLICT( id ) do update set name = ?;"
			);
			upsert_name.setInt( 1, id );
			upsert_name.setString( 2, name );
			upsert_name.execute();
		} catch ( SQLException e ) {
			log.error( "Error documenting action name " + e.getMessage() );
		}
		
		return id;
	}
}
