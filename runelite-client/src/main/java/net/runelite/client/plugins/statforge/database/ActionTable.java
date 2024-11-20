package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;

@Slf4j
public class ActionTable implements ITable {
	TrackerDatabase database;
	
	public ActionTable ( TrackerDatabase database ) {
		this.database = database;
	}
	
	@Override
	public void create () {
		try ( Statement stmt = database.sqlite.connection.createStatement() ) {
			stmt.execute(
					"create table if not exists actions\n" +
							"(\n" +
							"    uid         text,\n" +
							"    uuid        text\n" +
							"        constraint actions_users_uuid_fk\n" +
							"            references users,\n" +
							"    time        timestamp,\n" +
							"    name        integer\n" +
							"		constraint actions_names_fk\n" +
							"			references action_names,\n" +
							"    constraint actions_pk\n" +
							"        primary key (uid, uuid, time)\n" +
							");"
			);
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	
	public String build ( long user, long now, String name ) {
		var uid = Long.toHexString( now );
		var uuid = Long.toHexString( user );
		var timestamp = dateFormat.format( now );
		var name_id = database.table_action_names.get( name );
		
		try ( PreparedStatement insert_created_action = database.sqlite.connection.prepareStatement(
				"insert into actions( uid, uuid, time, name ) VALUES( ?, ?, ?, ? );"
		) ) {
			insert_created_action.setString( 1, uid );
			insert_created_action.setString( 2, uuid );
			insert_created_action.setString( 3, timestamp );
			insert_created_action.setInt( 4, name_id );
			insert_created_action.execute();
			return uid;
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
		
		return null;
	}
	
	public String getUuid ( String action ) {
		try ( PreparedStatement get_uuid_from_action_id = database.sqlite.connection.prepareStatement(
				"SELECT uuid FROM actions WHERE uid = ?;"
		) ) {
			get_uuid_from_action_id.setString( 1, action );
			var result = get_uuid_from_action_id.executeQuery();
			
			if ( result.next() ) {
				return result.getString( "uuid" );
			}
		} catch ( SQLException e ) {
			log.error( "Error retreiving UUID: " + e.getMessage() );
		}
		
		return null;
	}
}
