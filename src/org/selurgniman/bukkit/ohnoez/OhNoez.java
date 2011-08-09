package org.selurgniman.bukkit.ohnoez;

/**
 * 
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class OhNoez extends JavaPlugin
{
	private final Logger log = Logger.getLogger("Minecraft");
	private static final LinkedHashMap<String, String> CONFIG_DEFAULTS = new LinkedHashMap<String, String>();

	private Hashtable<Player, List<ItemStack>> playerItems = new Hashtable<Player, List<ItemStack>>();
	private Configuration config = null;
	private JavaPlugin plugin = null;
	static
	{
		CONFIG_DEFAULTS.put("frequency", "86400");
		CONFIG_DEFAULTS.put("credits", "1");
		CONFIG_DEFAULTS.put("prefix", ChatColor.RED + "OhNoez: " + ChatColor.WHITE);
		CONFIG_DEFAULTS.put("LIST_ITEM_MESSAGE", "%1$s(" + ChatColor.GREEN + "%2$d" + ChatColor.WHITE + ")");
		CONFIG_DEFAULTS.put("NO_ITEMS_MESSAGE", "No dropped items to list!");
		CONFIG_DEFAULTS.put("AVAILABLE_CREDITS_MESSAGE", "Available credits (" + ChatColor.GREEN + "%1$d" + ChatColor.WHITE + ").");
		CONFIG_DEFAULTS.put("CREDITED_BACK_MESSAGE", "%1$s(" + ChatColor.GREEN + "%2$d" + ChatColor.WHITE + ")");
		CONFIG_DEFAULTS.put("CREDITED_USED_MESSAGE", ChatColor.RED + "%1$s" + ChatColor.WHITE + " just used an OhNoez credit for the day!");
		CONFIG_DEFAULTS.put("OTHER_PLAYER_DEATH_MESSAGE", ChatColor.AQUA
				+ "%1$s "
				+ ChatColor.WHITE
				+ "was just killed by "
				+ ChatColor.RED
				+ "%2$s"
				+ ChatColor.WHITE
				+ " %3$d blocks from you!");
		CONFIG_DEFAULTS.put("PLAYER_DEATH_MESSAGE", ChatColor.AQUA + "You" + ChatColor.WHITE + " died " + ChatColor.RED + "%1$s!");
	}

	@Override
	public void onDisable()
	{
		log.info(config.getString("prefix") + " disabled.");
	}

	@Override
	public void onEnable()
	{
		this.plugin = this;
		loadConfig();
		setupDatabase();

		PluginManager pm = getServer().getPluginManager();
		EntityDeathListener listener = new EntityDeathListener();
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, listener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DEATH, listener, Priority.Normal, this);

		this.getCommand("ohnoez").setExecutor(new CommandExecutor()
		{
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
			{
				if (sender instanceof Player)
				{
					Player player = (Player) sender;
					if (args.length > 0)
					{
						List<ItemStack> droppedItems = playerItems.get(player);
						ArrayList<String> messages = new ArrayList<String>();
						/*
						 * List items dropped from the last death
						 */
						if (args[0].toUpperCase().equals("LIST"))
						{
							if (droppedItems != null)
							{
								for (ItemStack itemStack : droppedItems)
								{
									String itemTypeName = itemStack.getType().toString();
									if (itemStack.getType() == Material.INK_SACK)
									{
										switch (itemStack.getDurability())
										{
										case 0xF:
										{
											itemTypeName = "BONEMEAL";
											break;
										}
										case 0x4:
										{
											itemTypeName = "LAPIS_LAZULI";
											break;
										}
										case 0x3:
										{
											itemTypeName = "COCOA_BEANS";
											break;
										}
										case 0x1:
										{
											itemTypeName = "ROSES";
											break;
										}
										}
									}
									messages.add(String.format(config.getString("prefix") + config.getString("LIST_ITEM_MESSAGE") + "\n", itemTypeName, itemStack.getAmount()));
								}
							}
							else
							{
								messages.add(config.getString("prefix") + config.getString("NO_ITEMS_MESSAGE"));
							}
						}

						/*
						 * Credit listing
						 */
						else if (args[0].toUpperCase().equals("CREDITS"))
						{
							if (args.length > 1)
							{
								if (args[1].toUpperCase().equals("ADD"))
								{
									if (args.length > 2)
									{

										if (player.hasPermission("ohshit.credits.adjust"))
										{
											addAvailableCredits(player, Integer.parseInt(args[2]));
										}

									}
									else
									{
										messages.add(config.getString("prefix") + config.getString("prefix") + "syntax error: /ohshit credits add #");
									}
								}
								else if (args[1].toUpperCase().equals("SET"))
								{
									if (args.length > 2)
									{

										if (player.hasPermission("ohshit.credits.adjust"))
										{
											setAvailableCredits(player, Integer.parseInt(args[2]));
										}

									}
									else
									{
										messages.add(config.getString("prefix") + config.getString("prefix") + "syntax error: /ohshit credits set #");
									}
								}
							}
							else
							{
								messages.add(String.format(config.getString("prefix") + config.getString("AVAILABLE_CREDITS_MESSAGE"), getAvailableCredits(player)));
							}
						}

						/*
						 * Claim unused credits
						 */
						else if (args[0].toUpperCase().equals("CLAIM"))
						{
							if (droppedItems != null)
							{
								if (getAvailableCredits(player) > 0)
								{
									for (ItemStack itemStack : droppedItems)
									{
										player.getInventory().addItem(itemStack);
										messages.add(String.format(
												config.getString("prefix") + config.getString("CREDITED_BACK_MESSAGE") + "\n",
												itemStack.getType().toString(),
												itemStack.getAmount()));
									}
									useAvailableCredit(player);
									playerItems.remove(player);
								}
								else
								{
									messages.add(String.format(config.getString("prefix") + config.getString("AVAILABLE_CREDITS_MESSAGE"), getAvailableCredits(player)));
								}
							}
							else
							{
								messages.add(config.getString("prefix") + config.getString("NO_ITEMS_MESSAGE"));
							}
						}

						if (!messages.isEmpty())
						{
							for (String message : messages)
							{
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
		log.info(config.getString("prefix") + config.getString("prefix") + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}

	private void useAvailableCredit(Player player)
	{
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class).where().ieq("name", name).ieq("playerName", name).findUnique();
		if (creditClass != null)
		{
			creditClass.setCredits(creditClass.getCredits() - 1);
			creditClass.setLastCredit(new Date());
			plugin.getDatabase().save(creditClass);
			player.getServer().broadcastMessage(String.format(config.getString("prefix") + config.getString("CREDITED_USED_MESSAGE"), player.getName()));
		}
	}

	private int getAvailableCredits(Player player)
	{
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class).where().ieq("name", name).ieq("playerName", name).findUnique();
		if (creditClass == null)
		{
			creditClass = new Credits();
			creditClass.setPlayer(player);
			creditClass.setName(name);
			creditClass.setCredits(Integer.parseInt(config.getString("credits", "1")));
			plugin.getDatabase().save(creditClass);
		}
		else if (creditClass.getCredits() == 0)
		{
			Date lastCreditDate = creditClass.getLastCredit();
			int duration = (int) ((new Date().getTime() - lastCreditDate.getTime()) / 1000);
			if (duration >= config.getInt("frequency", 86400))
			{
				creditClass.setCredits(Integer.parseInt(config.getString("credits", "1")));
				plugin.getDatabase().save(creditClass);
			}
		}

		return creditClass.getCredits();
	}

	private void addAvailableCredits(Player player, int count)
	{
		setAvailableCredits(player, getAvailableCredits(player) + count);
	}

	private void setAvailableCredits(Player player, int count)
	{
		String name = player.getName();
		Credits creditClass = plugin.getDatabase().find(Credits.class).where().ieq("name", name).ieq("playerName", name).findUnique();
		if (creditClass != null)
		{
			creditClass.setCredits(count);
			plugin.getDatabase().save(creditClass);
		}
	}

	private void loadConfig()
	{
		config = this.getConfiguration();
		for (Entry<String, String> entry : CONFIG_DEFAULTS.entrySet())
		{
			if (config.getProperty(entry.getKey()) == null)
			{
				config.setProperty(entry.getKey(), entry.getValue());
			}
		}
		config.save();
	}

	private void setupDatabase()
	{
		try
		{
			plugin.getDatabase().find(Credits.class).findRowCount();
		}
		catch (PersistenceException ex)
		{
			System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
			installDDL();
		}
	}

	@Override
	public List<Class<?>> getDatabaseClasses()
	{
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(Credits.class);
		return list;
	}

	private class EntityDeathListener extends EntityListener
	{
		private IdentityHashMap<Player, String> lastDamagedBy = new IdentityHashMap<Player, String>();

		/**
		 * Count player damage delt and recieved.
		 */
		@Override
		public void onEntityDamage(EntityDamageEvent event)
		{
			// ****************************************************************
			Player player = null;
			String cause = null;
			// ****************************************************************

			if (event.getEntity() instanceof Player)
			{
				player = (Player) event.getEntity();

				/**
				 * Handle damage from blocks (cactus, lava)
				 */
				if (event instanceof EntityDamageByBlockEvent)
				{
					EntityDamageByBlockEvent evt = (EntityDamageByBlockEvent) event;
					if (evt.getDamager() != null)
					{
						cause = evt.getDamager().getType().toString();
					}
					else
					{
						Material material = player.getLocation().getBlock().getType();
						if (material == Material.LAVA || material == Material.STATIONARY_LAVA)
						{
							cause = "LAVA";
						}
					}
				}
				/**
				 * Count player damage from and to other living and undead
				 * sources.
				 */
				else if (event instanceof EntityDamageByEntityEvent)
				{
					EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;

					if (evt.getDamager() instanceof Monster)
					{
						cause = evt.getDamager().toString().replace("Craft", "");
					}
					else if (evt.getDamager() instanceof Player)
					{
						cause = ((Player) evt.getDamager()).getName();
					}
				}
				/**
				 * Count player damage from and by projectiles (arrows).
				 */
				else if (event instanceof EntityDamageByProjectileEvent)
				{
					EntityDamageByProjectileEvent evt = (EntityDamageByProjectileEvent) event;

					if (evt.getDamager() instanceof Monster)
					{
						cause = evt.getDamager().toString().replace("Craft", "");
					}
					else if (evt.getDamager() instanceof Player)
					{
						cause = ((Player) evt.getDamager()).getName();
					}
				}
				else if (event.getCause() == DamageCause.FIRE_TICK)
				{
					cause = "FIRE";
				}
				else
				{

					cause = event.getCause().toString();
				}

				String configCause = config.getString(cause);
				if (configCause != null)
				{
					cause = configCause;
				}

				lastDamagedBy.put(player, cause);
			}
		}

		public void onEntityDeath(EntityDeathEvent event)
		{
			Entity entity = event.getEntity();

			if (entity instanceof Player)
			{
				Player player = (Player) entity;
				playerItems.put(player, event.getDrops());

				String cause = lastDamagedBy.get(player);
				for (Player otherPlayer : player.getWorld().getPlayers())
				{
					if (otherPlayer != player)
					{
						Integer distance = getDistance(player.getLocation(), otherPlayer.getLocation()).intValue();
						otherPlayer.sendMessage(String.format(config.getString("prefix") + config.getString("OTHER_PLAYER_DEATH_MESSAGE"), player.getName(), cause, distance));
					}
					else
					{
						otherPlayer.sendMessage(String.format(config.getString("prefix") + config.getString("PLAYER_DEATH_MESSAGE"), cause));
					}
				}
			}
		}

		private Double getDistance(Location l1, Location l2)
		{
			double x = Math.pow(l1.getX() - l2.getX(), 2);
			double y = Math.pow(l1.getY() - l2.getY(), 2);
			double z = Math.pow(l1.getZ() - l2.getZ(), 2);
			return Math.sqrt(x + y + z);
		}
	}
}
