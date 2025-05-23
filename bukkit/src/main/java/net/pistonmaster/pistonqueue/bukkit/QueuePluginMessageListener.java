package net.pistonmaster.pistonqueue.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class QueuePluginMessageListener implements PluginMessageListener {
  private final PistonQueueBukkit plugin;

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void onPluginMessageReceived(@NotNull String channel, @NotNull Player messagePlayer, byte[] message) {
    if (!channel.equals("piston:queue")) return;

    ByteArrayDataInput in = ByteStreams.newDataInput(message);
    String subChannel = in.readUTF();

    // ————————————— XP V2 —————————————
    if (plugin.isPlayXP() && "xpV2".equals(subChannel)) {
      List<UUID> uuids = new ArrayList<>();
      int count = in.readInt();
      for (int i = 0; i < count; i++) {
        uuids.add(UUID.fromString(in.readUTF()));
      }
      for (UUID uuid : uuids) {
        Player target = plugin.getServer().getPlayer(uuid);
        if (target != null) {
          target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100.0F, 1.0F);
        }
      }

      // ————————————— TOS ACCEPTED —————————————
    } else if ("ACCEPTED".equals(subChannel)) {
      // 1) UUID/Name/Timestamp auslesen
      UUID uuid        = UUID.fromString(in.readUTF());
      String name      = in.readUTF();    // optional
      String timestamp = in.readUTF();    // optional für Logging

      // 2) Map + data.yml persistieren
      plugin.getAcceptedMap().put(uuid, timestamp);
      ConfigurationNode root = plugin.getDataRoot();

      try {
        root.node("accepted", uuid.toString()).set(timestamp);
        YamlConfigurationLoader loader = plugin.getDataLoader();
        loader.save(root);
      } catch (Exception ex) {
        plugin.getLogger().severe("Failed to persist accepted TOS data!");
        ex.printStackTrace();
      }

      // 3) PermissionAttachment setzen
      Player p = plugin.getServer().getPlayer(uuid);
      if (p == null) return;
      PermissionAttachment attach = p.addAttachment(plugin);
      attach.setPermission("piston.valeqs.tos.accepted", true);

      // 4) kurze Chat-Bestätigung
      p.sendMessage("§aDanke! Du hast die Nutzungsbedingungen akzeptiert.");
    }
  }
}
