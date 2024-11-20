package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.*;
import java.time.LocalDateTime;
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
        Integer delta    = null;
        var user     = database.table_action.getUuid( action );
        
        try (
                PreparedStatement get_previous_xp = database.sqlite.connection.prepareStatement(
                        "select xp.total from xp join main.actions as actions where actions.uuid=? and skill=? ORDER BY xp.total DESC LIMIT 1;"
                )
        ) {
            get_previous_xp.setString( 1, user );
            get_previous_xp.setInt( 2, skill_id );
            var result = get_previous_xp.executeQuery();
            
            if ( result.next() ) {
                delta = total - result.getInt( "total" );
            }
            
            if ( delta == null || delta >= 0 ) {
                var insert_current_drop = database.sqlite.connection.prepareStatement(
                        "insert into xp ( action, skill, level, total, delta )" +
                        "VALUES( ?, ?, ?, ?, ? );"
                );
                
                insert_current_drop.setString( 1, action );
                insert_current_drop.setInt( 2, skill_id );
                insert_current_drop.setInt( 3, level );
                insert_current_drop.setInt( 4, total );
                insert_current_drop.setObject( 5, delta );
                insert_current_drop.execute();
            }
        } catch ( SQLException e ) {
            log.error( e.getMessage() );
        }
    }
    
    public double[][] get ( String user, Skill skill, LocalDateTime begin, LocalDateTime end ) {
        var skill_id = skill.ordinal();
        var delta    = -1;
        
        List< double[] > ret = new ArrayList<>();
        
        try (
                PreparedStatement get_previous_xp = database.sqlite.connection.prepareStatement(
                        "select xp.total, actions.time from xp join main.actions as actions where actions.uuid=? and xp.skill=? and xp.action=actions.uid and actions.time between ? and ? ORDER BY xp.total ASC;\n"
                )
        ) {
            get_previous_xp.setString( 1, user );
            get_previous_xp.setInt( 2, skill_id );
            get_previous_xp.setTimestamp( 3, Timestamp.valueOf( begin ) );
            get_previous_xp.setTimestamp( 4, Timestamp.valueOf( end ) );
            var result = get_previous_xp.executeQuery();
            
            var meta = result.getMetaData();
            
            while ( result.next() ) {
                ret.add(
                        new double[]{
                                ( double ) result.getInt( "time" ),
                                ( double ) result.getInt( result.findColumn( "total" ) )
                        }
                );
            }
        } catch ( SQLException e ) {
            log.error( e.getMessage() );
        }
       
        var doubleret = new double[ ret.size() ][ 2 ];
        for ( int i = 0; i < ret.size(); i++ ) {
            log.info( "" + ret.get( i )[ 0 ] + " : " + ret.get( i )[ 1 ] );
            doubleret[ i ][ 0 ] = ret.get( i )[ 0 ];
            doubleret[ i ][ 1 ] = ret.get( i )[ 1 ];
        }
        
        return doubleret;
    }
}
