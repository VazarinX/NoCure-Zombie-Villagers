package prevent.curing;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoCure extends JavaPlugin implements Listener {

    private final Map<UUID, UUID> weaknessAppliedTracker = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("[NoCure] Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("[NoCure] Plugin has been disabled.");
    }

    /**
     * Detects when a player throws a Weakness potion at a zombie villager.
     */
    @EventHandler
    public void onWeaknessPotion(PotionSplashEvent event) {
        if (event.getPotion().getEffects().stream().anyMatch(effect -> effect.getType().equals(PotionEffectType.WEAKNESS))) {
            event.getAffectedEntities().forEach(entity -> {
                if (entity instanceof ZombieVillager) {
                    Player thrower = event.getEntity().getShooter() instanceof Player ? (Player) event.getEntity().getShooter() : null;
                    if (thrower != null) {
                        weaknessAppliedTracker.put(entity.getUniqueId(), thrower.getUniqueId());
                    }
                }
            });
        }
    }

    /**
     * Cancels the curing process before it starts.
     */
    @EventHandler
    public void onGoldenAppleUse(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ZombieVillager) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.GOLDEN_APPLE) {
                event.setCancelled(true); // Block the cure attempt

                UUID zombieID = event.getRightClicked().getUniqueId();
                UUID playerID = weaknessAppliedTracker.get(zombieID);
                weaknessAppliedTracker.remove(zombieID);

                Player curingPlayer = (playerID != null) ? Bukkit.getPlayer(playerID) : player;
                String playerName = (curingPlayer != null) ? curingPlayer.getName() : "Unknown Player";
                Location loc = event.getRightClicked().getLocation();
                String coordinates = "X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();

                String message = playerName + " tried to cure a zombie villager at " + coordinates;
                String inGameMessage = ChatColor.RED + "[NoCure] " + ChatColor.YELLOW + message;

                // Log to server console
                getLogger().warning("[NoCure] " + message);

                // Send message to OP players in-game
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.isOp()) {
                        p.sendMessage(inGameMessage);
                    }
                }

                // Send message to DiscordSRV staff chat
                if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
                    TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("staff-chat");
                    if (textChannel != null) {
                        textChannel.sendMessage("**[NoCure]** " + message).queue();
                    } else {
                        getLogger().warning("[NoCure] Could not find DiscordSRV staff-chat channel.");
                    }
                }
            }
        }
    }

    //Stop the transformation if needed
    @EventHandler
    public void onZombieVillagerCure(EntityTransformEvent event) {
        if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
            if (event.getEntityType() == EntityType.ZOMBIE_VILLAGER && event.getTransformedEntity().getType() == EntityType.VILLAGER) {
                event.setCancelled(true);

            }
        }
    }
}
