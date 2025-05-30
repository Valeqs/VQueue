package net.pistonmaster.pistonqueue.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

import java.util.Objects;

import org.bukkit.event.EventPriority;

/**
 * Dieser Listener zeigt neuen Spielern das TOS-Buch und verhindert Bewegung,
 * bis sie die Nutzungsbedingungen akzeptiert haben.
 */
public final class TosListener implements Listener {
  private final PistonQueueBukkit plugin;

  public TosListener(PistonQueueBukkit plugin) {
    this.plugin = plugin;
  }

  /**
   * Öffnet beim Betreten des Servers (1 Tick später) das Buch, falls der Spieler
   * die TOS noch nicht akzeptiert hat (also noch keine entsprechende Permission besitzt).
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // ─── NEU: Bukkit-Permission-Integration prüfen ───
    if (player.hasPermission("piston.valeqs.tos.accepted.bukkit")) {
      return; // schon akzeptiert → nichts weiter tun
    }

    UUID uuid = player.getUniqueId();
    boolean alreadyAccepted = player.hasPermission("piston.valeqs.tos.accepted")
      || plugin.getAcceptedMap().containsKey(uuid)
      || !plugin.getDataRoot().node("accepted", uuid.toString()).virtual();

    if (alreadyAccepted) {
      return; // Velocity-Permission, Map oder data.yml sagen: bereits akzeptiert
    }

    // Buch 1 Tick später öffnen (bestehende Logik)
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      ItemStack book = plugin.getTosBook(player);
      player.openBook(book);
    }, 1L);
  }

  /**
   * Solange die Permission fehlt, keine Bewegung erlauben
   * und das Buch nach jeder Drehung wieder öffnen.
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
  public void onMove(PlayerMoveEvent event) {
    Player p = event.getPlayer();

    // Solange noch keine TOS-Permission gesetzt wurde
    if (!(p.hasPermission("piston.valeqs.tos.accepted.bukkit")
      || p.hasPermission("piston.valeqs.tos.accepted"))) {
      // Wenn sich die Position ändert → abbrechen
      if (event.getFrom().distanceSquared(event.getTo()) > 0) {
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
          p.sendActionBar("§cDu musst auf 'Akzeptieren' drücken, um spielen zu können.");
          System.out.println("onMove ausgeführt!");
        }, 1L);
      }

      // Wenn sich die Blickrichtung ändert → Buch neu öffnen
      if (!Objects.equals(event.getFrom().getPitch(), event.getTo().getPitch())
        || !Objects.equals(event.getFrom().getYaw(),   event.getTo().getYaw())) {
        p.sendActionBar("§cDu musst auf 'Akzeptieren' drücken, um spielen zu können.");
        Bukkit.getScheduler().runTask(plugin, () -> {
          ItemStack book = plugin.getTosBook(p);
          p.openBook(book);
        });
      }
    }
  }
}
