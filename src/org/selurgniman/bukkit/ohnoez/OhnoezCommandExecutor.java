/**
 * 
 */
package org.selurgniman.bukkit.ohnoez;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author <a href="mailto:e83800@wnco.com">Chris Bandy</a> Created on: Dec 14,
 *         2011
 */
public class OhnoezCommandExecutor implements CommandExecutor {

	private final Model model;

	public OhnoezCommandExecutor(Model model) {
		this.model = model;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args.length > 0) {

				ArrayList<String> messages = new ArrayList<String>();
				/*
				 * List items dropped from the last death
				 */
				if (args[0].toUpperCase().equals("LIST")) {
					messages.addAll(listCommand(player));
				}

				/*
				 * Credit listing
				 */
				else if (args[0].toUpperCase().equals("CREDITS")) {
					messages.addAll(creditsCommand(player, args));
				}

				/*
				 * Claim unused credits
				 */
				else if (args[0].toUpperCase().equals("CLAIM")) {
					messages.addAll(claimCommand(player));
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

	private List<String> listCommand(Player player) {
		List<String> messages = new ArrayList<String>();
		List<ItemStack> droppedItems = model.getLastInventory(player);
		if (droppedItems.size() > 0) {
			for (ItemStack itemStack : droppedItems) {
				String itemTypeName = itemStack.getType().toString();
				if (itemStack.getType() == Material.INK_SACK) {
					switch (itemStack.getDurability()) {
					case 0xF: {
						itemTypeName = "BONEMEAL";
						break;
					}
					case 0x4: {
						itemTypeName = "LAPIS_LAZULI";
						break;
					}
					case 0x3: {
						itemTypeName = "COCOA_BEANS";
						break;
					}
					case 0x1: {
						itemTypeName = "ROSES";
						break;
					}
					}
				}
				messages.add(String.format(Message.LIST_ITEM_MESSAGE + "\n", itemTypeName, itemStack.getAmount()));
			}
			messages.add(String.format(Message.LIST_ITEM_MESSAGE + "\n", "Experience levels", model.getLastExperience(player)));
		} else {
			messages.add(Message.NO_ITEMS_MESSAGE + "\n");
		}

		return messages;
	}

	private List<String> creditsCommand(Player player, String... args) {
		List<String> messages = new ArrayList<String>();
		if (args.length > 1) {
			if (args[1].equalsIgnoreCase("ADD")) {
				if (args.length > 2 && player.isOp()) {
					model.addAvailableCredits(player, Integer.parseInt(args[2]));
					messages.add(String.format(Message.AVAILABLE_CREDITS_MESSAGE + "\n", player.getName(), model.getAvailableCredits(player)));
				} else {
					messages.add(Message.PREFIX + "syntax error: /ohnoez credits add #\n");
				}
			} else if (args[1].equalsIgnoreCase("SET")) {
				if (args.length > 2 && player.isOp()) {
					model.setAvailableCredits(player, Integer.parseInt(args[2]));
					messages.add(String.format(Message.AVAILABLE_CREDITS_MESSAGE + "\n", player.getName(), model.getAvailableCredits(player)));
				} else {
					messages.add(Message.PREFIX + "syntax error: /ohnoez credits set #\n");
				}
			}
		} else {
			messages.add(String.format(Message.AVAILABLE_CREDITS_MESSAGE + "\n", player.getName(), model.getAvailableCredits(player)));
		}

		return messages;
	}

	private List<String> claimCommand(Player player) {
		List<String> messages = new ArrayList<String>();
		List<ItemStack> droppedItems = model.getLastInventory(player);
		Integer droppedExp = model.getLastExperience(player);
		
		if (droppedItems != null) {
			if (model.getAvailableCredits(player) > 0) {
				for (ItemStack itemStack : droppedItems) {
					player.getInventory().addItem(itemStack);
					messages.add(String.format(Message.CREDITED_BACK_MESSAGE + "\n", itemStack.getType().toString(), itemStack.getAmount()));
				}
				player.setLevel(player.getLevel()+droppedExp);
				
				model.useAvailableCredit(player);
				model.setLastInventory(player, new ArrayList<ItemStack>(), 0);
			} else {
				messages.add(String.format(Message.AVAILABLE_CREDITS_MESSAGE + "\n", player.getName(), model.getAvailableCredits(player)));
			}
		} else {
			messages.add(Message.NO_ITEMS_MESSAGE + "\n");
		}

		return messages;
	}
}
