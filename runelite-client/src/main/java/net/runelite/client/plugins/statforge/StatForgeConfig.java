package net.runelite.client.plugins.statforge;

import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup( "statforge" )
public interface StatForgeConfig extends Config {
	@ConfigItem(
			keyName = "file",
			name = "SQLite File",
			description = "The SQLite File location",
			position = 1
	)
	public default String SqliteFile () { return RuneLite.RUNELITE_DIR + "/statforge.db"; }
	
}
