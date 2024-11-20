package net.runelite.client.plugins.statforge.database;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class InventoryTable implements ITable {
    TrackerDatabase database;

    public InventoryTable(TrackerDatabase database) {
        this.database = database;
    }

    @Override
    public void create() {
        try {
            Statement stmt = database.sqlite.connection.createStatement();
            stmt.execute(
                    "create table if not exists inventory\n" +
                            "(\n" +
                            "    action text\n" +
                            "        constraint inventory_actions_uid_fk\n" +
                            "            references actions (uid),\n" +
                            "    id     integer,\n" +
                            "    item   integer,\n" +
                            "    total  integer,\n" +
                            "    delta  integer,\n" +
                            "    constraint inventory_pk\n" +
                            "        primary key (action, id, item)\n" +
                            ");\n" +
                            "\n"
            );

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    Map< Integer, Item[] > inventories = new HashMap<>();
    
    public void update(String action, ItemContainer container) {
        var current = container.getItems();
        var items = Arrays.copyOf( current, current.length );
        var user = database.table_action.getUuid( action );
        
        var totals = new HashMap< Integer, Item >();
        var prev_inv = inventories.getOrDefault( container.getId(), null );
        
        for ( var item : items ) {
            var stack = totals.getOrDefault( item.getId(), new Item( item.getId(), 0 ) );
            totals.put( item.getId(), new Item( item.getId(), stack.getQuantity() + item.getQuantity() ) );
        }
        
        if ( prev_inv != null ) {
            for ( var item : prev_inv ) {
                if ( !totals.containsKey( item.getId() ) ) {
                    totals.put( item.getId(), new Item( item.getId(), 0 ) );
                }
            }
        }
        
        inventories.put( container.getId(), totals.values().stream().filter( item -> { return item.getQuantity() != 0; } ).toArray( Item[]::new ) );
        
        
        try {
            for ( var item : totals.values() ) {
                if ( item.getId() == -1 ) continue;
                var get_previous_total = database.sqlite.connection.prepareStatement(
                        "select inventory.total from inventory INNER JOIN main.actions as actions ON actions.uid = action where actions.uuid=? and id=? and item=? ORDER BY actions.time DESC LIMIT 1;\n"
                );
                get_previous_total.setString( 1, user );
                get_previous_total.setInt( 2, container.getId() );
                get_previous_total.setInt( 3, item.getId() );
                var result = get_previous_total.executeQuery();
                
                var prev = 0;
                if ( result.next() ) {
                    prev = result.getInt( "total" );
                }
                
                var delta = item.getQuantity() - prev;
                if ( delta == 0 ) continue;
                
                var insert_current_action = database.sqlite.connection.prepareStatement(
                        "insert into inventory ( action, id, item, total, delta ) " +
                                "VALUES( ?, ?, ?, ?, ? );"
                );
                
                insert_current_action.setString( 1, action );
                insert_current_action.setInt( 2, container.getId() );
                insert_current_action.setInt( 3, item.getId() );
                insert_current_action.setInt( 4, item.getQuantity() );
                insert_current_action.setInt( 5, delta );
                insert_current_action.execute();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }
}
