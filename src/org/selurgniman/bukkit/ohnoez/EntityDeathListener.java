/**
 * 
 */
package org.selurgniman.bukkit.ohnoez;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;

/**
 * @author <a href="mailto:e83800@wnco.com">Chris Bandy</a>
 * Created on: Dec 14, 2011
 */
public class EntityDeathListener extends EntityListener
{
	private final Model model;
	public EntityDeathListener(Model model){
		this.model = model;
	}
	
	@Override
	public void onEntityDeath(EntityDeathEvent event)
	{
		Entity entity = event.getEntity();

		if (entity instanceof Player)
		{
			Player player = (Player) entity;
			model.setLastInventory(player, event.getDrops());
			String cause = getPlayerDamageCause(player.getLastDamageCause());
			
			for (Player otherPlayer : player.getWorld().getPlayers())
			{
				if (otherPlayer != player)
				{
					Integer distance = getDistance(player.getLocation(), otherPlayer.getLocation()).intValue();
					otherPlayer.sendMessage(String.format(Message.OTHER_PLAYER_DEATH_MESSAGE.toString(), player.getName(), cause, distance));
				}
				else
				{
					otherPlayer.sendMessage(String.format(Message.PLAYER_DEATH_MESSAGE.toString(), cause));
				}
			}
		}
	}

	private String getPlayerDamageCause(EntityDamageEvent event)
	{
		// ****************************************************************
		Player player = (Player) event.getEntity();
		String cause = "";
		// ****************************************************************

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
		else if (event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;

			if (evt.getDamager() instanceof Monster)
			{
				cause = ((Monster)evt.getDamager()).toString();
			}
			else if (evt.getDamager() instanceof Player)
			{
				cause = ((Player) evt.getDamager()).getName();
			}
			else if (evt.getDamager() instanceof Projectile)
			{
				cause = ((Projectile)evt.getDamager()).getShooter().toString();
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

		String configCause = Message.valueOf(cause).toString();
		if (configCause != null)
		{
			cause = configCause;
		}
		
		return cause.replace("Craft", "");
	}

	private Double getDistance(Location l1, Location l2)
	{
		double x = Math.pow(l1.getX() - l2.getX(), 2);
		double y = Math.pow(l1.getY() - l2.getY(), 2);
		double z = Math.pow(l1.getZ() - l2.getZ(), 2);
		return Math.sqrt(x + y + z);
	}
}