package net.pistonmaster.pistonqueue.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class QueuePluginMessageListener implements PluginMessageListener {
  private final PistonQueueBukkit plugin;

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void onPluginMessageReceived(@NotNull String channel, @NotNull Player messagePlayer, byte[] message) {
    if (!channel.equals("piston:queue")) {
      return;
    }

    // plugin.getLogger().info("[Bukkit] PluginMessage erhalten: channel=" + channel);

    ByteArrayDataInput in = ByteStreams.newDataInput(message);
    String subChannel = in.readUTF();

    // plugin.getLogger().info("[Bukkit] SubChannel=" + subChannel);

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
    }
    // ————————————— TOS ACCEPTED —————————————
    else if ("ACCEPTED".equals(subChannel)) {
      //plugin.getLogger().info("Bukkit-Listener: TOS ACCEPTED-Block erreicht für Kanal „piston:queue“");
      // 1) UUID/Name/Timestamp auslesen
      UUID uuid = UUID.fromString(in.readUTF());
      plugin.getLogger().info("TOS ACCEPTED für " + uuid);
      String name = in.readUTF();
      String timestamp = in.readUTF();

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

      // 3) Bukkit-Permission sofort und dauerhaft setzen
      Player p = plugin.getServer().getPlayer(uuid);
      if (p == null) {
        return;
      }
      // Sofortige Wirkung
      p.addAttachment(plugin).setPermission("piston.valeqs.tos.accepted.bukkit", true);
      // Dauerhaft via LuckPerms
// 3b) Persistente Speicherung via LuckPerms API über den Player-Adapter
      LuckPerms api = LuckPermsProvider.get();
// Da unsere Variable `p` ja schon das Bukkit-Player-Objekt ist:
      User userLP = api.getPlayerAdapter(org.bukkit.entity.Player.class).getUser(p);

      Node node = Node.builder("piston.valeqs.tos.accepted.bukkit")
        .value(true)
        .build();
// Node hinzufügen und speichern
      userLP.data().add(node);
      api.getUserManager().saveUser(userLP);

      // 4) kurze Chat-Bestätigung
      p.sendMessage("§aDu hast die Nutzungsbedingungen akzeptiert.");
    }
    // ————————————— TOS DECLINE —————————————
    else if ("DECLINE".equals(subChannel)) {
      // TODO: Decline-Logik (Kicken etc.)
    }
  }
}
