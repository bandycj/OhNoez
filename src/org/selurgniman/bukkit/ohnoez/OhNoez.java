package org.selurgniman.bukkit.ohnoez;

/**
 * 
 */

import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;

public class OhNoez extends JavaPlugin {
	private final Logger log = Logger.getLogger("Minecraft");
	private static final MemoryConfiguration CONFIG_DEFAULTS = new MemoryConfiguration();
	
	private Model model = null;

	static {
		CONFIG_DEFAULTS.addDefault("frequency", 86400);
		CONFIG_DEFAULTS.addDefault("credits", 1);
		for (Entry<String, Message> entry : Message.values()) {
			CONFIG_DEFAULTS.addDefault(entry.getKey(), entry.getValue().toString());
		}
	}

	@Override
	public void onDisable() {
		this.saveConfig();
		log.info(Message.PREFIX + " disabled.");
	}

	@Override
	public void onEnable() {
		this.getConfig().setDefaults(CONFIG_DEFAULTS);
		Message.setConfig(this.getConfig());
		
		this.model = new Model(this);
		setupDatabase();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.ENTITY_DEATH, new EntityDeathListener(model), Priority.Highest, this);

		this.getCommand("ohnoez").setExecutor(new OhnoezCommandExecutor(model));

		PluginDescriptionFile pdfFile = this.getDescription();
		log.info(Message.PREFIX + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
	}

	private void setupDatabase() {
		try {
			model.getDatabase().find(Credits.class).findRowCount();
			model.getDatabase().find(Drop.class).findRowCount();
		} catch (PersistenceException ex) {
			System.out.println("Installing database for " + getDescription().getName() + " due to first time usage");
			installDDL();
		}
	}

	@Override
	public EbeanServer getDatabase() {
		return model.getDatabase();
	}
}
