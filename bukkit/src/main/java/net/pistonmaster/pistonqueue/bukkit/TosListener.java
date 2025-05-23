package net.pistonmaster.pistonqueue.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Dieser Listener zeigt neuen Spielern das TOS-Buch und verhindert Bewegung,
 * bis sie die Nutzungsbedingungen akzeptiert haben.
 */
public class TosListener implements Listener {
  private final PistonQueueBukkit plugin;

  public TosListener(PistonQueueBukkit plugin) {
    this.plugin = plugin;
  }

  /**
   * Öffnet beim Betreten des Servers (1 Tick später) das Buch, falls der Spieler
   * die TOS noch nicht akzeptiert hat.
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Wenn TOS bereits akzeptiert, nichts tun
    if (player.hasPermission("piston.valeqs.tos.accepted")) {
      return;
    }

    // Buch 1 Tick später öffnen, damit der Client bereit ist
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      ItemStack book = plugin.getTosBook(player);
      player.openBook(book);
    }, 1L);
  }

  /**
   * Verhindert Bewegung und erneutes Öffnen des Buchs bei Drehung,
   * solange die TOS nicht akzeptiert sind.
   */
  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();

    // Wenn TOS nicht akzeptiert, Bewegung und Drehung blockieren
    if (!player.hasPermission("piston.valeqs.tos.accepted")) {
      // Wenn Position sich ändert, abbrechen
      if (event.getFrom().distanceSquared(event.getTo()) > 0) {
        event.setCancelled(true);
      }
      // Wenn Blickrichtung (Pitch oder Yaw) sich ändert, Buch erneut öffnen
      if (event.getFrom().getPitch() != event.getTo().getPitch()
        || event.getFrom().getYaw() != event.getTo().getYaw()) {
        // Direkt im nächsten Tick, um Konflikte zu vermeiden
        Bukkit.getScheduler().runTask(plugin, () -> {
          player.openBook(plugin.getTosBook(player));
        });
      }
    }
  }
}
