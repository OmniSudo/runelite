package net.runelite.client.plugins.statforge;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.statforge.database.SQLiteDatabase;
import net.runelite.client.plugins.statforge.database.TrackerDatabase;


import java.io.File;

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
    StatForgeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(StatForgeConfig.class);
    }

    TrackerDatabase database;

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        connect();
        create();
    }

    private void connect() {
        database = new TrackerDatabase(new SQLiteDatabase(new File(Config.SqliteFile())));
    }

    private void create() {
        database.create();

        database.table_skill.update();
    }

    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
    }

    private String trackingUser = null;

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        switch (gameStateChanged.getGameState()) {
            case LOGGED_IN:
                if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null || client.getLocalPlayer().getName() == trackingUser)
                    return;
                database.table_user.track(client.getAccountHash(), trackingUser = client.getLocalPlayer().getName());
                break;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        final var skill = statChanged.getSkill();
        final var currentXp = statChanged.getXp();
        final var currentLevel = statChanged.getLevel();
        final var timestamp = System.currentTimeMillis();
        final var user = client.getAccountHash();

        final var action = database.table_action.build(user, timestamp, "xp");

        database.table_xp.post(action, skill, currentLevel, currentXp);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        final var user = client.getAccountHash();
        final var timestamp = System.currentTimeMillis();

        final var action = database.table_action.build(user, timestamp, "item");

        database.table_inventory.update(action, event.getItemContainer());
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {

    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        var widgetID = event.getActionParam1();
        int widgetIndex = event.getActionParam0();
        if (widgetID == 0) return;
        if (false) {
            client.createMenuEntry(-1)
                    .setOption("Stats Tracker")
                    .setTarget(event.getTarget())
                    .setType(MenuAction.RUNELITE)
                    .setIdentifier(event.getIdentifier())
                    .onClick(e -> {
                                toggleStatsWindow();
                            }
                    );
        }
    }

    Widget statsWindow = null;

    private final int stats_widget_parent = 10551372;

    public void toggleStatsWindow() {
        var parent = client.getWidget(stats_widget_parent);

        log.info("Creating stats window");

        statsWindow = parent.createChild(100, WidgetType.RECTANGLE);
    }
}
