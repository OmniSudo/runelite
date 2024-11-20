package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class XpTable implements ITable {
	TrackerDatabase database;
	
	public XpTable ( TrackerDatabase database ) {
		this.database = database;
		
	}
	
	@Override
	public void create () {
		try ( Statement stmt = database.sqlite.connection.createStatement() ) {
			stmt.execute(
					"create table if not exists xp\n" +
							"(\n" +
							"    action text\n" +
							"        constraint xp_actions_uid_fk\n" +
							"            references actions,\n" +
							"    skill  integer\n" +
							"        constraint xp_skills_uid_fk\n" +
							"            references skills,\n" +
							"    level  integer,\n" +
							"    total  integer,\n" +
							"    delta  integer,\n" +
							"    constraint xp_pk\n" +
							"        primary key (action)\n" +
							");"
			);
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	
	public void post ( String action, Skill skill, int level, int total ) {
		var skill_id = skill.ordinal();
		var delta = -1;
		var user = database.table_action.getUuid( action );
		
		try ( PreparedStatement get_previous_xp = database.sqlite.connection.prepareStatement(
				"select xp.total from xp join main.actions as actions where actions.uuid=? and skill=? ORDER BY actions.time DESC LIMIT 1;\n"
		) ) {
			get_previous_xp.setString( 1, user );
			get_previous_xp.setInt( 2, skill_id );
			var result = get_previous_xp.executeQuery();

			if ( result.next() ) {
				delta = total - result.getInt( "total" );
			}

			if ( delta > 0 || delta == -1 ) {
				var insert_current_drop = database.sqlite.connection.prepareStatement(
						"insert into xp ( action, skill, level, total, delta )" +
								"VALUES( ?, ?, ?, ?, ? );"
				);

				insert_current_drop.setString( 1, action );
				insert_current_drop.setInt( 2, skill_id );
				insert_current_drop.setInt( 3, level );
				insert_current_drop.setInt( 4, total );
				insert_current_drop.setInt( 5, delta );
				insert_current_drop.execute();
			}
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	public double[][] get ( String user, Skill skill ) {
		var skill_id = skill.ordinal();
		var delta = -1;
		
		List< double[] > ret = new ArrayList<>();
		
		try ( PreparedStatement get_previous_xp = database.sqlite.connection.prepareStatement(
				"select xp.total, actions.time from xp join main.actions as actions where uuid=? and skill=? ORDER BY actions.time DESC;\n"
		) ) {
			get_previous_xp.setString( 1, user );
			get_previous_xp.setInt( 2, skill_id );
			var result = get_previous_xp.executeQuery();
			
			var meta = result.getMetaData();
			
			while ( result.next() ) {
				ret.add( new double[]{
						( double ) result.getInt( result.findColumn( "total" ) ),
				} );
				log.info( Double.toString( ret.get( ret.size() - 1 )[ 0 ] ) );
			}
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
		return ( ( double[][] ) ( ret.toArray() ));
	}
}
