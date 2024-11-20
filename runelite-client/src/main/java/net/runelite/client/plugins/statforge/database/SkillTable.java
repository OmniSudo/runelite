package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class SkillTable implements ITable {
	TrackerDatabase database;
	
	public SkillTable ( TrackerDatabase database ) {
		this.database = database;
	}
	
	@Override
	public void create () {
		try ( Statement stmt = database.sqlite.connection.createStatement() ) {
			stmt.execute(
					"create table if not exists skills\n" +
							"(\n" +
							"    uid   integer\n" +
							"        constraint skills_pk\n" +
							"            primary key,\n" +
							"    skill text\n" +
							");"
			);
		} catch ( SQLException e ) {
			log.error( e.getMessage() );
		}
	}
	
	public void update () {
			for ( var skill : Skill.values() ) {
				try ( PreparedStatement stmt = database.sqlite.connection.prepareStatement(
						"insert into skills( uid, skill ) VALUES( ?, ? )" +
								"on conflict( uid ) do update set skill = ?;"
				) ) {
					stmt.setInt( 1, skill.ordinal() );
					stmt.setString( 2, skill.getName() );
					stmt.setString( 3, skill.getName() );
					stmt.execute();
				} catch ( SQLException e ) {
					log.error( e.getMessage() );
				}
			}
	}
}
