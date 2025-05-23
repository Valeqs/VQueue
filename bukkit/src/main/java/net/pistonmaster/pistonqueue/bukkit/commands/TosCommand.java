package net.pistonmaster.pistonqueue.bukkit.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TosCommand implements CommandExecutor {
  private final JavaPlugin plugin;
  private static final String CHANNEL = "piston:queue"; // Muss mit deinem Velocity-Channel übereinstimmen

  public TosCommand(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // 1) Nur Spieler dürfen das ausführen
    if (!(sender instanceof Player)) {
      sender.sendMessage("Nur Spieler können diesen Befehl ausführen.");
      return true;
    }

    Player player = (Player) sender;

    // 2) Usage, falls kein Unterbefehl angegeben wurde
    if (args.length != 1) {
      player.sendMessage("Verwendung: /tos <accept|decline>");
      return true;
    }

    String sub = args[0].toLowerCase();
    if ("accept".equals(sub)) {
      // 3a) Temporär lokale Permission setzen (falls du das brauchst)
      player.addAttachment(plugin, "piston.valeqs.tos.accepted", true);

      // 3b) Plugin-Message an Velocity senden
      ByteArrayDataOutput out = ByteStreams.newDataOutput();
      out.writeUTF("ACCEPTED");
      out.writeUTF(player.getUniqueId().toString());
      out.writeUTF(player.getName());
      String timestamp = ZonedDateTime
        .now(ZoneId.of("Europe/Berlin"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
      out.writeUTF(timestamp);

      // Wichtig: Outgoing-Channel vorher in onEnable() registrieren!
      player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
      player.sendMessage("§aDanke! Du wirst nun weitergeleitet…");
      return true;
    }

    if ("decline".equals(sub)) {
      player.kickPlayer("Du hast die Nutzungsbedingungen abgelehnt. Verbindung getrennt.");
      return true;
    }

    // 4) Ungültiger Unterbefehl
    player.sendMessage("Unbekannter Unterbefehl. Verwendung: /tos <accept|decline>");
    return true;
  }
}
