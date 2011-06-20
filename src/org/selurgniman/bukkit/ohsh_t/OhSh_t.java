/**
 * 
 */
package org.selurgniman.bukkit.ohsh_t;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class OhSh_t extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");
	private static final Map<String, Object> CONFIG_DEFAULTS = new HashMap<String, Object>();
	
	private Hashtable<Player, List<ItemStack>> playerItems = new Hashtable<Player, List<ItemStack>>();
	private Configuration config = null;
	private GroupManager gm = null;
	private JavaPlugin plugin = null;
	static {
        CONFIG_DEFAULTS.put("credits", 1);
    }
	
	@Override
	public void onDisable() {
		config.save();
		gm.getWorldsHolder().saveChanges();
		log.info(Messages.prefix+" disabled.");
	}

	@Override
	public void onEnable() {
		this.plugin=this;
		setupPermissions();
		loadConfig();
		setupDatabase();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ENTITY_DEATH, new EntityDeathListener(),
				Priority.Normal, this);

		this.getCommand("ohshit").setExecutor(new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command,
					String label, String[] args) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					if (args.length > 0) {
						List<ItemStack> droppedItems = playerItems.get(player);
						ArrayList<String> messages = new ArrayList<String>();
						/*
						 * List items dropped from the last death
						 */
						if (args[0].toUpperCase().equals("LIST")) {
							if (droppedItems != null) {
								for (ItemStack itemStack : droppedItems) {
									String itemTypeName = itemStack.getType().toString(); 
									if (itemStack.getType() == Material.INK_SACK){
										switch (itemStack.getDurability()){
											case 0xF:{ itemTypeName = "BONEMEAL"; break; }
											case 0x4:{ itemTypeName = "LAPIS_LAZULI"; break; }
											case 0x3:{ itemTypeName = "COCOA_BEANS"; break; }
											case 0x1:{ itemTypeName = "ROSES"; break; }
										}
									}
									messages.add(String.format(
												Messages.LIST_ITEM, 
												itemTypeName,
												itemStack.getAmount()));
								}
							} else {
								messages.add(Messages.NO_ITEMS);
							}
						}
						
						/*
						 * Credit listing
						 */
						if (args[0].toUpperCase().equals("CREDITS")) {
							if (args.length>1){
								if (args[1].toUpperCase().equals("ADD")){
									if (args.length>2){
										if (gm.getWorldsHolder().getWorldPermissions(player).has(player,"ohshit.credits.add")){
											addAvailableCredits(player,Integer.parseInt(args[2]));
										} else {
											messages.add(Messages.prefix+"syntax error: /ohshit credits add #");
										}
									}
								}
							}
							messages.add(String.format(
									Messages.AVAILABLE_CREDITS,
									getAvailableCredits(player)));
						}
						
						/*
						 * Claim unused credits
						 */
						if (args[0].toUpperCase().equals("CLAIM")){
							if (droppedItems != null){
								if (getAvailableCredits(player)>0){
									for (ItemStack itemStack:droppedItems){
										player.getInventory().addItem(itemStack);
										messages.add(String.format(
												Messages.CREDITED_BACK,
												itemStack.getType().toString(), 
												itemStack.getAmount()));
									}
									useAvailableCredit(player);
									playerItems.remove(player);
								} else {
									messages.add(String.format(
											Messages.AVAILABLE_CREDITS,
											getAvailableCredits(player)));
								}
							} else {
								messages.add(Messages.NO_ITEMS);
							}
						}
						
						if (!messages.isEmpty()) {
							for (String message : messages) {
								player.sendMessage(message);
							}
							return true;
						}
					}
				}

				return false;
			}
		});
		
		PluginDescriptionFile pdfFile = this.getDescription();
        log.info(Messages.prefix+pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
	}
	
	private void useAvailableCredit(Player player){
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class)
				.where().ieq("name", name)
				.ieq("playerName", name).findUnique();
		if (creditClass!=null){
			creditClass.setCredits(creditClass.getCredits()-1);
			creditClass.setLastCredit(new Date());
			plugin.getDatabase().save(creditClass);
			player.getServer().broadcastMessage(String.format(Messages.CREDITED_USED, player.getName()));
		}
	}
	
	private int getAvailableCredits(Player player){
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class)
				.where().ieq("name", name)
				.ieq("playerName", name).findUnique();
		if (creditClass == null) {
			creditClass = new Credits();
			creditClass.setPlayer(player);
			creditClass.setName(name);
			creditClass.setCredits(config.getInt("credits", 1));
			plugin.getDatabase().save(creditClass);
		} else if (creditClass.getCredits() == 0) {
			Date lastCreditDate = creditClass.getLastCredit();
			int days = (int)( (new Date().getTime() - lastCreditDate.getTime()) / (1000 * 60 * 60 * 24));
			if (days >= 1){
				creditClass.setCredits(config.getInt("credits", 1));
				plugin.getDatabase().save(creditClass);
			}
		}
		
		return creditClass.getCredits();
	}
	
	private void addAvailableCredits(Player player, int count){
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class)
				.where().ieq("name", name)
				.ieq("playerName", name).findUnique();
		if (creditClass!=null){
			creditClass.setCredits(creditClass.getCredits()+count);
			plugin.getDatabase().save(creditClass);
		}
	}

	private void setupPermissions() {
		Plugin p = this.getServer().getPluginManager()
				.getPlugin("GroupManager");
		if (p != null) {
			if (!this.getServer().getPluginManager().isPluginEnabled(p)) {
				this.getServer().getPluginManager().enablePlugin(p);
			}
			this.gm = (GroupManager) p;
		} else {
			this.getPluginLoader().disablePlugin(this);
		}
	}
	
	private void loadConfig() {
		File configFile = new File(this.getDataFolder(), "config.yml");
		if (configFile.exists()) {
			config = new Configuration(configFile);
			config.load();
			config.setHeader("# Configuration file for OhSh_t\r\n#");
			for (String prop : CONFIG_DEFAULTS.keySet()) {
				if (config.getProperty(prop) == null) {
					config.setProperty(prop, CONFIG_DEFAULTS.get(prop));
				}
			}
		} else {
			try {
				this.getDataFolder().mkdir();
				configFile.createNewFile();
				config = new Configuration(configFile);
				// default values
				config.setHeader("# Configuration file for OhSh_t\r\n#");
				for (String prop : CONFIG_DEFAULTS.keySet()) {
					config.setProperty(prop, CONFIG_DEFAULTS.get(prop));
				}
				config.save();
			} catch (IOException e) {
				log.info(e.toString());
			}
		}
	}

	private void setupDatabase() {
		try {
			plugin.getDatabase().find(Credits.class).findRowCount();
		} catch (PersistenceException ex) {
			System.out.println("Installing database for "
					+ getDescription().getName() + " due to first time usage");
			installDDL();
		}
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(Credits.class);
		return list;
	}

	private class EntityDeathListener extends EntityListener {
		public void onEntityDeath(EntityDeathEvent event) {
			Entity entity = event.getEntity();

			if (entity instanceof Player) {
				Player player = (Player) entity;
				playerItems.put(player, event.getDrops());
			}
		}
	}

	private static class Messages {
		private static final String prefix = 
				ChatColor.RED + "OhSh_T: " + ChatColor.WHITE;
		protected static final String LIST_ITEM = 
				prefix + 
				"%1$s(" + 
				ChatColor.GREEN + "%2$d" + ChatColor.WHITE + ")\n";
		protected static final String NO_ITEMS = 
				prefix +
				"No dropped items to list!";
		protected static final String AVAILABLE_CREDITS = 
				prefix + 
				"Available credits (" + ChatColor.GREEN + "%1$d" + 
				ChatColor.WHITE + ").";
		protected static final String CREDITED_BACK = 
				prefix + 
				"%1$s(" +
				ChatColor.GREEN + "%2$d" + ChatColor.WHITE + ")\n";
		protected static final String CREDITED_USED = 
				prefix + 
				ChatColor.RED+"%1$s" + ChatColor.WHITE+ " just used an OhSh_t credit for the day!";
	}
}
