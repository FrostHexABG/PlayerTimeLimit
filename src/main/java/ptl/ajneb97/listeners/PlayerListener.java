package ptl.ajneb97.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import ptl.ajneb97.PlayerTimeLimit;
import ptl.ajneb97.configs.MainConfigManager;
import ptl.ajneb97.managers.MensajesManager;
import ptl.ajneb97.managers.PlayerManager;
import ptl.ajneb97.model.TimeLimitPlayer;

public class PlayerListener implements Listener{

	private PlayerTimeLimit plugin;
	public PlayerListener(PlayerTimeLimit plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (plugin.getConfigsManager().getMainConfigManager().isDisabled()) {
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					player.setGameMode(GameMode.ADVENTURE);
				}
			}, 2);
		} else {
			PlayerManager playerManager = plugin.getPlayerManager();
			TimeLimitPlayer p = playerManager.getPlayerByUUID(player.getUniqueId().toString());
			if (p == null) {
				p = playerManager.createPlayer(player);
			}
			p.setPlayer(player);
			p.setName(player.getName());

			// Remove spectator on join if the player has time left
			if (playerManager.hasTimeLeft(p)) {
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						player.setGameMode(GameMode.ADVENTURE);
						plugin.getMensajesManager().enviarMensaje(player, "&aSet your gamemode to ADVENTURE.", true);
					}
				}, 2);
			} else {
				// Set the player to spectator gamemode instead of kicking them
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						player.setGameMode(GameMode.SPECTATOR);
						plugin.getMensajesManager().enviarMensaje(player, "&cYour practice time has expired. Your gamemode has been set to SPECTATOR.", true);
					}
				}, 2);

			}
		}
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		PlayerManager playerManager = plugin.getPlayerManager();
		TimeLimitPlayer p = playerManager.getPlayerByUUID(player.getUniqueId().toString());
		if(p != null) {
			p.setPlayer(null);
			p.eliminarBossBar();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onTeleport(PlayerTeleportEvent event) {
		TeleportCause cause = event.getCause();
		if(cause.equals(TeleportCause.PLUGIN) || cause.equals(TeleportCause.COMMAND)) {
			Player player = event.getPlayer();
			PlayerManager playerManager = plugin.getPlayerManager();
			TimeLimitPlayer p = playerManager.getPlayerByUUID(player.getUniqueId().toString());
			if(p == null) {
				return;
			}
			
			MainConfigManager mainConfig = plugin.getConfigsManager().getMainConfigManager();
			if(!mainConfig.isWorldWhitelistEnabled()) {
				return;
			}
			
			//Revisar si el mundo donde va esta activado
			World worldTo = event.getTo().getWorld();
			List<String> worlds = mainConfig.getWorldWhitelistWorlds();
			if(!worlds.contains(worldTo.getName())) {
				return;
			}
			
			//Revisar si se le ha acabado el tiempo
			if(!playerManager.hasTimeLeft(p)) {
				FileConfiguration messages = plugin.getMessages();
				List<String> msg = messages.getStringList("joinErrorMessage");
				for(String m : msg) {
					player.sendMessage(MensajesManager.getMensajeColor(m));
				}
				event.setCancelled(true);
				return;
			}
		}
	}
}
