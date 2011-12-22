/**
 * 
 */
package org.selurgniman.bukkit.ohnoez;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.lennardf1989.bukkitex.MyDatabase;

/**
 * @author <a href="mailto:e83800@wnco.com">Chris Bandy</a> Created on: Dec 14,
 *         2011
 */
public class Model {
	private final MyDatabase myDatabase;
	private final EbeanServer database;
	private final JavaPlugin plugin;

	private static final String DATABASE_DRIVER = "database.driver";
	private static final String DATABASE_URL = "database.url";
	private static final String DATABASE_USER_NAME = "database.username";
	private static final String DATABASE_PASSWORD = "database.password";
	private static final String DATABASE_ISOLATION = "database.isolation";
	private static final String DATABASE_LOGGING = "database.logging";
	private static final String DATABASE_REBUILD = "database.rebuild";
	
	public Model(JavaPlugin plugin) {
		this.plugin = plugin;
		this.plugin.getConfig().addDefault(DATABASE_DRIVER, "org.sqlite.JDBC");
		this.plugin.getConfig().addDefault(DATABASE_URL, "jdbc:sqlite:"+plugin.getDataFolder().getAbsolutePath()+"\\"+plugin.getDescription().getName()+".db");
		this.plugin.getConfig().addDefault(DATABASE_USER_NAME, "bukkit");
		this.plugin.getConfig().addDefault(DATABASE_PASSWORD, "walrus");
		this.plugin.getConfig().addDefault(DATABASE_ISOLATION, "SERIALIZABLE");
		this.plugin.getConfig().addDefault(DATABASE_LOGGING, Boolean.FALSE);
		this.plugin.getConfig().addDefault(DATABASE_REBUILD, Boolean.TRUE);
		
		this.myDatabase = new MyDatabase(plugin) {
			@Override
			protected java.util.List<Class<?>> getDatabaseClasses() {
				List<Class<?>> list = new ArrayList<Class<?>>();
				list.add(Credits.class);
				list.add(Drop.class);
				return list;
			};
		};
		this.myDatabase.initializeDatabase(plugin.getConfig().getString(DATABASE_DRIVER), plugin.getConfig().getString(DATABASE_URL), plugin.getConfig()
				.getString(DATABASE_USER_NAME), plugin.getConfig().getString(DATABASE_PASSWORD), plugin.getConfig().getString(DATABASE_ISOLATION), plugin
				.getConfig().getBoolean(DATABASE_LOGGING, false), plugin.getConfig().getBoolean(DATABASE_REBUILD, true));
		this.plugin.getConfig().set("database.rebuild", false);
		this.plugin.saveConfig();

		this.database = myDatabase.getDatabase();
	}

	public EbeanServer getDatabase() {
		return this.myDatabase.getDatabase();
	}

	public void useAvailableCredit(Player player) {
		Credits creditClass = getRecordForPlayer(player);
		if (getAvailableCredits(player) > 0) {
			creditClass.setCredits(creditClass.getCredits() - 1);
			creditClass.setLastCredit(new Date());
			database.delete(creditClass.getDrops());
			database.save(creditClass);
			player.getServer().broadcastMessage(String.format(Message.CREDIT_USED_MESSAGE.toString(), player.getName()));
		}
	}

	public int getAvailableCredits(Player player) {
		Credits creditClass = getRecordForPlayer(player);
		if (creditClass.getCredits() == 0) {
			Date lastCreditDate = creditClass.getLastCredit();
			int duration = (int) ((new Date().getTime() - lastCreditDate.getTime()) / 1000);
			if (duration >= plugin.getConfig().getInt("frequency", 86400)) {
				creditClass.setCredits(Integer.parseInt(plugin.getConfig().getString("credits", "1")));
				database.save(creditClass);
			}
		}

		return creditClass.getCredits();
	}
	public int getLastExperience(Player player){
		Credits creditClass = getRecordForPlayer(player);
		for (Drop drop : creditClass.getDrops()) {
			if (drop.getItemId() == -2) {
				return drop.getItemCount();
			} 
		}
		
		return 0;
	}
	public List<ItemStack> getLastInventory(Player player) {
		Credits creditClass = getRecordForPlayer(player);
		ArrayList<ItemStack> items = new ArrayList<ItemStack>();
		for (Drop drop : creditClass.getDrops()) {
			if (drop.getItemId() != -2) {
				items.add(new ItemStack(Material.getMaterial(drop.getItemId()), drop.getItemCount(),drop.getItemDurability().shortValue(),drop.getItemData().byteValue()));
			} 
		}
		return items;
	}

	public void addAvailableCredits(Player player, int count) {
		setAvailableCredits(player, getAvailableCredits(player) + count);
	}

	public void setAvailableCredits(Player player, int count) {
		Credits creditClass = getRecordForPlayer(player);
		creditClass.setCredits(count);

		database.save(creditClass);
	}

	public void setLastInventory(Player player, List<ItemStack> itemsList, Integer droppedExp) {
		Credits creditClass = getRecordForPlayer(player);
		ArrayList<Drop> drops = new ArrayList<Drop>();
		Drop drop = new Drop();
		drop.setItemId(-2);
		System.out.println("level: "+player.getLevel()+":"+droppedExp);
		drop.setItemCount(player.getLevel());
		drop.setItemData(0);
		drop.setItemDurability(0);
		drop.setCredit(creditClass);
		drops.add(drop);
		
		for (ItemStack item : itemsList) {
			Byte data = item.getData().getData();
			drop = new Drop();
			drop.setItemId(item.getTypeId());
			drop.setItemCount(item.getAmount());
			drop.setItemData(data.intValue());
			drop.setItemDurability(new Integer(item.getDurability()));
			drop.setCredit(creditClass);
			drops.add(drop);
		}

		database.save(drops);
	}
	
	

	private Credits getRecordForPlayer(Player player) {
		String name = player.getName();
		Credits creditClass = database.find(Credits.class).where().ieq("playerName", name).findUnique();
		if (creditClass == null) {
			creditClass = new Credits();
			creditClass.setPlayer(player);
			creditClass.setCredits(plugin.getConfig().getInt("credits", 1));
			
			database.save(creditClass);
		}

		return creditClass;
	}
}
