package au.com.addstar.unscramble;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import au.com.addstar.unscramble.prizes.Prize;
import au.com.addstar.unscramble.prizes.Prizes;
import au.com.addstar.unscramble.prizes.SavedPrize;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class UnscrambleCommand extends Command
{
	public UnscrambleCommand()
	{
		super("unscramble", null, "us");
	}
	
	private boolean permTest(CommandSender sender, String perm)
	{
		if(!sender.hasPermission(perm))
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "You do not have permission to perform that command."));
			return false;
		}
		
		return true;
	}
	
	@Override
	public void execute( CommandSender sender, String[] args )
	{
		if(args.length == 0)
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.DARK_PURPLE + "====================================================="));
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + " Welcome to " + ChatColor.RED + "Unscramble " + ChatColor.GREEN + "Plugin " + ChatColor.BLUE + "(" + Unscramble.instance.getDescription().getVersion() + ")"));
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + " Help is available with " + ChatColor.GOLD + "/unscramble help"));
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.DARK_PURPLE + "====================================================="));
		}
		else
		{
			if(args[0].equalsIgnoreCase("help"))
			{
				commandHelp(sender);
			}
			else if(args[0].equalsIgnoreCase("claim"))
			{
				commandClaim(sender);
			}
			else if(args[0].equalsIgnoreCase("reload"))
			{
				if(!permTest(sender, "unscramble.reload"))
					return;
				
				commandReload(sender);
			}
			else if(args[0].equalsIgnoreCase("hint"))
			{
				if(!permTest(sender, "unscramble.hint"))
					return;
				
				commandHint(sender);
			}
			else if(args[0].equalsIgnoreCase("cancel"))
			{
				if(!permTest(sender, "unscramble.cancel"))
					return;
				
				commandCancel(sender);
			}
			else if(args[0].equalsIgnoreCase("newgame"))
			{
				if(!permTest(sender, "unscramble.newgame"))
					return;
				
				commandNewgame(sender, Arrays.copyOfRange(args, 1, args.length));
			}
			else
			{
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Unknown command /unscramble " + args[0]));
			}
		}
	}
	
	private void commandHelp(CommandSender sender)
	{
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.DARK_PURPLE + "=================" + ChatColor.RED + " [ Unscramble Help ] " + ChatColor.DARK_PURPLE + "=================="));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.YELLOW + "- States the general info"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "claim " + ChatColor.YELLOW + "- Claims any prizes you have won"));
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "help " + ChatColor.YELLOW + "- Shows this screen"));
		
		if(sender.hasPermission("unscramble.reload"))
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "reload " + ChatColor.YELLOW + "- Reloads the config"));
		
		if(sender.hasPermission("unscramble.hint"))
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "hint " + ChatColor.YELLOW + "- Gives a hint on the current word"));
		
		if(sender.hasPermission("unscramble.cancel"))
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "cancel " + ChatColor.YELLOW + "- Cancels any currently running game"));
		
		if(sender.hasPermission("unscramble.newgame")) {
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "/unscramble " + ChatColor.GRAY + "newgame w:[word] t:[time] h:[hint-interval] [prize] " + ChatColor.YELLOW + "- Starts a new game with the given details"));
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + " [prize] examples: " + ChatColor.GOLD  + "item diamond 1" + ChatColor.GREEN + " or " + ChatColor.GOLD  + "$150"));
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + " Note: Underscores (_) in [word] will be changed into spaces"));
		}

		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.DARK_PURPLE + "====================================================="));
	}
	
	private void commandClaim(CommandSender sender)
	{
		if(sender instanceof ProxiedPlayer)
		{
			final ProxiedPlayer player = (ProxiedPlayer)sender;

			List<String> claimServers = Unscramble.instance.getConfig().claimServers;
			if(!claimServers.isEmpty() && !claimServers.contains(player.getServer().getInfo().getName()))
			{
				String serverString = "";
				for(int i = 0; i < claimServers.size(); ++i)
				{
					String server = claimServers.get(i);
					
					if(i != 0)
					{
						if(i != claimServers.size()-1)
							serverString += ", ";
						else
							serverString += " or ";
					}
					
					serverString += server;
				}
				
				if(claimServers.size() > 1)
					serverString = "either " + serverString;
					
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + "[Unscramble] " + ChatColor.RED + "You may not claim your prizes here. Please go to " + serverString));
				return;
			}
			
			List<SavedPrize> prizes = Unscramble.instance.getPrizes(player, true);
			if(prizes == null || prizes.isEmpty())
			{
				sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + "[Unscramble] " + ChatColor.RED + "No prizes found under your name."));
				return;
			}

			// Keep track of the number of prizes awarded
			// Allow the player to claim up to 10 at a time to avoid too many simultaneous sessions or
			// spamming the user with "Your inventory was full" messages if they run out of room
			int prizesAwarded = 0;

			for(SavedPrize prize : prizes)
			{
				int session = prize.prize.award(player);
				Unscramble.instance.startPrizeSession(session, player, prize.prize);

				if(++prizesAwarded >= 10) {
					if(prizesAwarded < prizes.size()) {

						// More prizes remain; inform the player
						// Using a 2 second delay to prevent the message from appearing before the messages regarding awarded prizes

						Runnable task = new Runnable() {
							@Override
							public void run() {

								player.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + "[Unscramble] " + ChatColor.DARK_AQUA + "You have more prizes to claim. Make sure you have free inventory space then use " + ChatColor.GOLD + "/us claim" + ChatColor.DARK_AQUA + " again"));
							}
						};

						Unscramble.instance.getProxy().getScheduler().schedule(Unscramble.instance, task, 2, TimeUnit.SECONDS);

						break;
					}
				}
			}
		}
		else
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + "[Unscramble] " + ChatColor.RED + "No prizes found under your name."));
	}
	
	private void commandReload(CommandSender sender)
	{
		Unscramble.instance.reload();
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GREEN + "Reloaded unscramble configs"));
	}
	
	private void commandHint(CommandSender sender)
	{
		if(!Unscramble.instance.isSessionRunning())
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "There is no unscramble game running."));
			return;
		}
		
		Unscramble.instance.getSession().doHint();
	}
	
	private void commandCancel(CommandSender sender)
	{
		if(!Unscramble.instance.isSessionRunning())
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "There is no unscramble game running."));
			return;
		}
		
		Unscramble.instance.getSession().stop();
		sender.sendMessage(TextComponent.fromLegacyText(ChatColor.GOLD + "Game cancelled."));
	}
	
	private void commandNewgame(CommandSender sender, String[] args)
	{
		if(Unscramble.instance.isSessionRunning())
		{
			sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "There is already an unscramble game running."));
			return;
		}
		
		String word = "";
		int hints = 0;
		int time = 30000;
		Prize prize = null;
		
		for(int i = 0; i < args.length; ++i)
		{
			String arg = args[i].toLowerCase();
			
			if(arg.startsWith("w:"))
				word = arg.substring(2).replace('_', ' ');
			else if(arg.startsWith("t:"))
			{
				try
				{
					if(arg.substring(2).endsWith("s")) {
						// User entered something like t:30s
						time = Integer.parseInt(arg.substring(2, arg.length() - 1));
					} else {
						time = Integer.parseInt(arg.substring(2));
					}

					if(time <= 0)
					{
						sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Time must be 1 or greater (seconds)"));
						return;
					}
					
					time *= 1000;
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Time must be 1 or greater (seconds)"));
					return;
				}
			}
			else if(arg.startsWith("h:"))
			{
				try
				{
					if(arg.substring(2).endsWith("s")) {
						// User entered something like h:10s
						hints = Integer.parseInt(arg.substring(2, arg.length() - 1));
					} else {
						hints = Integer.parseInt(arg.substring(2));
					}

					if(hints <= 0)
					{
						sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Hint interval must be 1 or greater (seconds)"));
						return;
					}
					
					hints *= 1000;
				}
				catch(NumberFormatException e)
				{
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Hint interval must be 1 or greater (seconds)"));
					return;
				}
			}
			else
			{
				String prizeString = "";
				for(int j = i; j < args.length; ++j)
				{
					if(!prizeString.isEmpty())
						prizeString += " ";
					prizeString += args[j];
				}
				
				try
				{
					prize = Prizes.parse(prizeString);
					if(prize == null)
					{
						sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + "Cannot create prize from '" + prizeString + "'. Please check your typing."));
						return;
					}
				}
				catch(IllegalArgumentException e)
				{
					sender.sendMessage(TextComponent.fromLegacyText(ChatColor.RED + e.getMessage()));
					return;
				}
				
				break;
			}
		}
		
		Unscramble.instance.newSession(word, time, hints, prize);
	}

}
