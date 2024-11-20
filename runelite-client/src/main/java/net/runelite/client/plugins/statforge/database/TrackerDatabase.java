package net.runelite.client.plugins.statforge.database;

public class TrackerDatabase {
	public final UserTable table_user;
	public final SkillTable table_skill;
	public final ActionTable table_action;
	public final ActionNameTable table_action_names;
	public final XpTable table_xp;
	public final InventoryTable table_inventory;
	
	SQLiteDatabase sqlite;
	
	public TrackerDatabase ( SQLiteDatabase database ) {
		this.sqlite = database;
		
		this.table_user = new UserTable( this );
		this.table_skill = new SkillTable( this );
		this.table_action = new ActionTable( this );
		this.table_action_names = new ActionNameTable( this );
		this.table_xp = new XpTable( this );
		this.table_inventory = new InventoryTable( this );
	}
	
	public void create () {
		table_user.create();
		table_skill.create();
		table_inventory.create();
		table_action.create();
		table_action_names.create();
		table_xp.create();
	}
}
