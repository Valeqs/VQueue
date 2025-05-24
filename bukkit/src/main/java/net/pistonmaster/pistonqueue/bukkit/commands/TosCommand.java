package net.pistonmaster.pistonqueue.bukkit.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.pistonmaster.pistonqueue.bukkit.PistonQueueBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.time.Instant;

import java.util.Locale;

public class TosCommand implements CommandExecutor {
  private final PistonQueueBukkit plugin;

  public TosCommand(PistonQueueBukkit plugin) {
    this.plugin = plugin;
    // Registriere hier bitte auch den Plugin‐Message‐Channel, falls noch nicht geschehen:
    // plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "piston:queue");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // Nur Spieler dürfen den Befehl nutzen
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Nur Spieler können das ausführen.");
      return true;
    }

    Player player = (Player) sender;
    if (!label.equalsIgnoreCase("tos")) {
      return false;
    }

    if (args.length != 1) {
      player.sendMessage(ChatColor.RED + "Verwendung: /tos <accept|decline>");
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);

    // Block 2: Für Spieler, die schon auf Bukkit akzeptiert haben, nichts mehr tun
    if (player.hasPermission("piston.valeqs.tos.accepted.bukkit")) {
      player.sendMessage(ChatColor.RED + "Du hast die Nutzungsbedingungen bereits akzeptiert.");
      return true;
    }

    switch (sub) {
      case "accept":
        // 1) Bukkit-Permission setzen (persistiert nur bis Server-Neustart)
        player.addAttachment(plugin).setPermission("piston.valeqs.tos.accepted.bukkit", true);

        // 2) ToS-Buch aus Inventar entfernen
        player.getInventory().remove(Material.WRITTEN_BOOK);

        // 3) Velocity informieren, dass der Spieler akzeptiert hat
        ByteArrayDataOutput outAccept = ByteStreams.newDataOutput();
        outAccept.writeUTF("ACCEPTED");                          // Sub-Channel, den dein Proxy erwartet
        outAccept.writeUTF(player.getUniqueId().toString());     // UUID
        outAccept.writeUTF(player.getName());                     // Spielername
        outAccept.writeUTF(Instant.now().toString());            // Zeitstempel
        player.sendPluginMessage(plugin, "piston:queue", outAccept.toByteArray());

        player.sendMessage(ChatColor.GREEN + "Danke! Du hast die Nutzungsbedingungen akzeptiert.");
        break;

      case "decline":
        // Velocity informieren, dass der Spieler abgelehnt hat
        ByteArrayDataOutput outDecline = ByteStreams.newDataOutput();
        outDecline.writeUTF("TOS_DECLINE");
        player.sendPluginMessage(plugin, "piston:queue", outDecline.toByteArray());

        player.sendMessage(ChatColor.RED + "Du hast die Nutzungsbedingungen abgelehnt.");
        player.kickPlayer(ChatColor.RED + "Verbindung getrennt: Nutzungsbedingungen nicht akzeptiert.");
        break;

      default:
        player.sendMessage(ChatColor.RED + "Unbekannter Subbefehl. Nutze /tos accept oder /tos decline.");
        break;
    }

    return true;
  }
}
