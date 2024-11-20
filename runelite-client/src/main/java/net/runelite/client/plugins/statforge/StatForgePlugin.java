package net.runelite.client.plugins.statforge;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptEvent;
import net.runelite.api.Skill;
import net.runelite.api.events.*;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.statforge.database.SQLiteDatabase;
import net.runelite.client.plugins.statforge.database.TrackerDatabase;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@PluginDescriptor(
        name = "StatForge",
        description = "Track all the things!"
)
@Slf4j
public class StatForgePlugin extends Plugin {
    @Inject
    public StatForgeConfig Config;
    
    @Inject
    private Client client;
    
    @Provides
    StatForgeConfig provideConfig ( ConfigManager configManager ) {
        return configManager.getConfig( StatForgeConfig.class );
    }
    
    TrackerDatabase database;
    
    @Override
    protected void startUp () throws Exception {
        super.startUp();
        
        connect();
        create();
    }
    
    private void connect () {
        database = new TrackerDatabase( new SQLiteDatabase( new File( Config.SqliteFile() ) ) );
    }
    
    private void create () {
        database.create();
        
        database.table_skill.update();
    }
    
    @Override
    protected void shutDown () throws Exception {
        super.shutDown();
    }
    
    private String trackingUser = null;
    
    @Subscribe
    public void onGameStateChanged ( GameStateChanged gameStateChanged ) {
        switch ( gameStateChanged.getGameState() ) {
            case LOGGED_IN:
                if ( client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null ||
                     client.getLocalPlayer().getName() == trackingUser ) { return; }
                database.table_user.track( client.getAccountHash(), trackingUser = client.getLocalPlayer().getName() );
                break;
        }
    }
    
    @Subscribe
    public void onStatChanged ( StatChanged statChanged ) {
        final var skill        = statChanged.getSkill();
        final var currentXp    = statChanged.getXp();
        final var currentLevel = statChanged.getLevel();
        final var timestamp    = System.currentTimeMillis();
        final var user         = client.getAccountHash();
        new Thread( () -> {
            final var action = database.table_action.build( user, timestamp, "xp" );
            
            database.table_xp.post( action, skill, currentLevel, currentXp );
        } ).start();
    }
    
    @Subscribe
    public void onItemContainerChanged ( ItemContainerChanged event ) {
        final var user      = client.getAccountHash();
        final var timestamp = System.currentTimeMillis();
        new Thread( () -> {
            final var action = database.table_action.build( user, timestamp, "item" );
            
            database.table_inventory.update( action, event.getItemContainer() );
        } ).start();
    }
    
    @Subscribe
    public void onMenuOpened ( MenuOpened event ) {
    
    }
    
    @Subscribe
    public void onMenuEntryAdded ( MenuEntryAdded event ) {
        var widgetID    = event.getActionParam1();
        int widgetIndex = event.getActionParam0();
        if ( widgetID == 0 ) return;
        if ( skill_widgets.containsKey( widgetID ) ) {
            var skill = skill_widgets.get( widgetID );
            var name  = skill.getName().toLowerCase();
            name = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
            
            client.createMenuEntry( -1 )
                  .setOption( name + " Graph" )
                  .setTarget( event.getTarget() )
                  .setType( MenuAction.RUNELITE )
                  .setIdentifier( event.getIdentifier() )
                  .onClick( e -> {
                                toggleStatsWindow( skill );
                            }
                  );
        }
    }
    
    private final int stats_widget_parent = 35913770;
    
    private final Map< Integer, Skill > skill_widgets = new HashMap<>() {{
        int i = 20971521;
        put( i++, Skill.ATTACK );
        put( i++, Skill.STRENGTH );
        put( i++, Skill.DEFENCE );
        put( i++, Skill.RANGED );
        put( i++, Skill.PRAYER );
        put( i++, Skill.MAGIC );
        put( i++, Skill.RUNECRAFT );
        put( i++, Skill.CONSTRUCTION );
        put( i++, Skill.HITPOINTS );
        put( i++, Skill.AGILITY );
        put( i++, Skill.HERBLORE );
        put( i++, Skill.THIEVING );
        put( i++, Skill.CRAFTING );
        put( i++, Skill.FLETCHING );
        put( i++, Skill.SLAYER );
        put( i++, Skill.HUNTER );
        put( i++, Skill.MINING );
        put( i++, Skill.SMITHING );
        put( i++, Skill.FISHING );
        put( i++, Skill.COOKING );
        put( i++, Skill.FIREMAKING );
        put( i++, Skill.WOODCUTTING );
        put( i++, Skill.FARMING );
    }};
    
    public void toggleStatsWindow ( Skill skill ) {
        var statsWindow = client.getWidget( stats_widget_parent );
        
        if ( skill == null || activeWindow == skill ) {
            statsWindow.deleteAllChildren();
            activeWindow = null;
            return;
        }
        
        if ( statsWindow.getChildren() != null &&
             Arrays.stream( statsWindow.getChildren() ).anyMatch( w -> w != null )
        ) {
            statsWindow.deleteAllChildren();
        }
        
        log.info( "Creating " + skill.getName() + " stats window" );
        activeWindow = skill;
        
        statsWindow.deleteAllChildren();
        
        var exit = statsWindow.createChild( 28, WidgetType.GRAPHIC );
        exit.setModelZoom( 100 );
        exit.setSpriteId( 542 );
        exit.setOriginalX( 482 );
        exit.setOriginalY( 6 );
        exit.setOriginalWidth( 26 );
        exit.setOriginalHeight( 23 );
        exit.setHasListener( true );
        exit.setOnClickListener( ( JavaScriptCallback ) this::exit );
        exit.setNoClickThrough( true );
        
        var backgroundCenter = statsWindow.createChild( 3, WidgetType.MODEL );
        backgroundCenter.setModelId( 20836 );
        backgroundCenter.setRotationX( 512 );
        backgroundCenter.setModelType( 1 );
        backgroundCenter.setOriginalX( 240 );
        backgroundCenter.setOriginalY( 161 );
        backgroundCenter.setOriginalWidth( 32 );
        backgroundCenter.setOriginalHeight( 32 );
        backgroundCenter.setModelZoom( 1393 );
        backgroundCenter.setNoClickThrough( true );
        backgroundCenter.setNoScrollThrough( true );
        
        var banner = statsWindow.createChild( 2, WidgetType.MODEL );
        banner.setModelId( 20852 );
        banner.setRotationX( 512 );
        banner.setRotationZ( 1024 );
        banner.setModelType( 1 );
        banner.setOriginalX( 162 );
        banner.setOriginalY( 13 );
        banner.setOriginalWidth( 32 );
        banner.setOriginalHeight( 32 );
        banner.setModelZoom( 1357 );
        banner.setNoClickThrough( true );
        banner.setNoScrollThrough( true );
        
        var label = statsWindow.createChild( 7, WidgetType.TEXT );
        label.setText( skill.getName().substring( 0, 1 ).toUpperCase().concat( skill.getName().toLowerCase().substring(
                1 ) ).concat( " Stats" ) );
        label.setTextColor( 0x46320A );
        label.setFontId( 645 );
        label.setTextShadowed( false );
        label.setOriginalX( 35 );
        label.setOriginalY( 7 );
        label.setOriginalWidth( 291 );
        label.setOriginalHeight( 34 );
        label.setXTextAlignment( WidgetTextAlignment.CENTER );
        label.setYTextAlignment( WidgetTextAlignment.TOP );
        
        statsWindow.setHidden( false );
        for ( var widget : statsWindow.getChildren() ) {
            if ( widget == null ) continue;
            widget.revalidate();
        }
        statsWindow.revalidate();
    }
    
    Skill activeWindow = null;
    
    protected void exit ( ScriptEvent ev ) {
        toggleStatsWindow( activeWindow );
    }
}
