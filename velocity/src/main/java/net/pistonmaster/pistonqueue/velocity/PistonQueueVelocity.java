package net.pistonmaster.pistonqueue.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.pistonmaster.pistonqueue.data.PluginData;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;
import net.pistonmaster.pistonqueue.velocity.commands.MainCommand;
import net.pistonmaster.pistonqueue.velocity.listeners.QueueListenerVelocity;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;
import net.pistonmaster.pistonqueue.velocity.utils.AcceptedData;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import net.pistonmaster.pistonqueue.shared.config.Config;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.io.ByteArrayDataOutput;

import net.pistonmaster.pistonqueue.shared.queue.QueueType;

@Plugin(
  id          = "pistonqueue",
  name        = PluginData.NAME,
  version     = PluginData.VERSION,
  url         = PluginData.URL,
  description = PluginData.DESCRIPTION,
  authors     = {"AlexProgrammerDE"}
)
public final class PistonQueueVelocity implements PistonQueuePlugin {
  @Getter private final Path dataDirectory;
  @Getter private final ProxyServer proxyServer;
  @Getter private final Logger logger;
  @Getter private final PluginContainer pluginContainer;
  @Getter private final QueueListenerVelocity queueListenerVelocity = new QueueListenerVelocity(this);
  private final Metrics.Factory metricsFactory;

  // Für TOS-Persistenz
  private File dataFile;
  private YamlConfigurationLoader loader;
  private ConfigurationNode dataConfigNode;
  private final Map<UUID, AcceptedData> acceptedMap = new ConcurrentHashMap<>();

  // Für Rate-Limiting der WARN-Meldungen
  private final Map<String, Long> lastWarning = new ConcurrentHashMap<>();

  @Inject
  public PistonQueueVelocity(
    ProxyServer proxyServer,
    Logger logger,
    @DataDirectory Path dataDirectory,
    PluginContainer pluginContainer,
    Metrics.Factory metricsFactory
  ) {
    this.proxyServer      = proxyServer;
    this.logger           = logger;
    this.dataDirectory    = dataDirectory;
    this.pluginContainer  = pluginContainer;
    this.metricsFactory   = metricsFactory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event) {
    info("Loading config");
    processConfig(dataDirectory);

    // ─── ToS: data.yml laden ───
    try {
      // data.yml anlegen oder kopieren, falls noch nicht vorhanden
      dataFile = dataDirectory.resolve("data.yml").toFile();
      if (!dataFile.exists()) {
        try (InputStream in = getClass().getResourceAsStream("/data.yml")) {
          Files.copy(in, dataFile.toPath());
        }
      }
      // Configurate-Loader initialisieren und Node laden
      loader = YamlConfigurationLoader.builder()
        .path(dataFile.toPath())
        .build();
      dataConfigNode = loader.load();
      // acceptedMap füllen
      Map<Object, ? extends ConfigurationNode> children = dataConfigNode.node("accepted").childrenMap();
      for (Map.Entry<Object, ? extends ConfigurationNode> e : children.entrySet()) {
        UUID uuid = UUID.fromString(e.getKey().toString());
        ConfigurationNode node = e.getValue();
        acceptedMap.put(uuid, new AcceptedData(
          node.node("name").getString(""),
          node.node("time").getString("")
        ));
      }
    } catch (IOException ex) {
      logger.error("Fehler beim Laden der ToS-Daten!", ex);
    }

    initializeReservationSlots();

    info("Looking for hooks");
    if (proxyServer.getPluginManager().getPlugin("pistonmotd").isPresent()) {
      info("Hooking into PistonMOTD");
      new PistonMOTDPlaceholder();
    }

    info("Registering plugin messaging channel");
    proxyServer.getChannelRegistrar()
      .register(MinecraftChannelIdentifier.create("piston", "queue"));

    info("Registering commands");
    proxyServer.getCommandManager().register("pistonqueue", new MainCommand(this), "pq");

    info("Registering listeners");
    proxyServer.getEventManager().register(this, queueListenerVelocity);

    info("Loading Metrics");
    metricsFactory.make(this, 12389);

    info("Checking for update");
    try {
      String currentVersionString = pluginContainer.getDescription().getVersion().orElse("unknown");
      SemanticVersion gitHubVersion = new GitHubUpdateChecker()
        .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
      SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

      if (gitHubVersion.isNewerThan(currentVersion)) {
        info("There is an update available!");
        info("Current: " + currentVersionString + " New: " + gitHubVersion);
        info("Download at: https://modrinth.com/plugin/pistonqueue");
      } else {
        info("You're up to date!");
      }
    } catch (IOException e) {
      error("Could not check for updates!");
      e.printStackTrace();
    }

    info("Scheduling tasks");
    scheduleTasks(queueListenerVelocity);

    // Lade bereits gespeicherte Akzeptanzen
    loadAcceptedData();
  }

  @Override
  public Optional<PlayerWrapper> getPlayer(UUID uuid) {
    return proxyServer.getPlayer(uuid).map(this::wrapPlayer);
  }

  @Override
  public List<PlayerWrapper> getPlayers() {
    return proxyServer.getAllPlayers().stream()
      .map(this::wrapPlayer)
      .collect(Collectors.toList());
  }

  @Override
  public Optional<ServerInfoWrapper> getServer(String name) {
    return proxyServer.getServer(name).map(this::wrapServer);
  }

  @Override
  public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
    proxyServer.getScheduler()
      .buildTask(this, runnable)
      .delay(delay, unit)
      .repeat(period, unit)
      .schedule();
  }

  @Override
  public void info(String message) {
    logger.info(message);
  }

  @Override
  public void warning(String message) {
    long now = System.currentTimeMillis();
    Long last = lastWarning.get(message);
    if (last == null || now - last >= 5_000) {   // 5 Sekunden
      logger.warn(message);
      lastWarning.put(message, now);
    }
  }

  @Override
  public void error(String message) {
    logger.error(message);
  }

  @Override
  public List<String> getAuthors() {
    return pluginContainer.getDescription().getAuthors();
  }

  @Override
  public String getVersion() {
    return pluginContainer.getDescription().getVersion().orElse("unknown");
  }

  private ServerInfoWrapper wrapServer(RegisteredServer server) {
    return new ServerInfoWrapper() {
      @Override
      public List<PlayerWrapper> getConnectedPlayers() {
        return server.getPlayersConnected().stream()
          .map(PistonQueueVelocity.this::wrapPlayer)
          .collect(Collectors.toList());
      }

      @Override
      public boolean isOnline() {
        try {
          server.ping().join();
          return true;
        } catch (CancellationException | CompletionException e) {
          return false;
        }
      }

      @Override
      public void sendPluginMessage(String channel, byte[] data) {
        server.sendPluginMessage(() -> "piston:queue", data);
      }
    };
  }

  public PlayerWrapper wrapPlayer(Player player) {
    return new PlayerWrapper() {
      @Override
      public boolean hasPermission(String node) {
        return player.hasPermission(node);
      }

      @Override
      public void connect(String server) {
        // echten Proxy-Connect auslösen
        RegisteredServer target = proxyServer
          .getServer(server)
          .orElseThrow(() ->
            new IllegalStateException("Server nicht gefunden: " + server)
          );
        player.createConnectionRequest(target).connect();
      }

      @Override
      public Optional<String> getCurrentServer() {
        return player.getCurrentServer()
          .map(c -> c.getServerInfo().getName());
      }

      @Override
      public void sendMessage(MessageType type, String message) {
        if (message == null || message.isBlank()) return;
        switch (type) {
          case CHAT       -> player.sendMessage(ChatUtils.parseToComponent(message, player));
          case ACTION_BAR -> player.sendActionBar(ChatUtils.parseToComponent(message, player));
        }
      }

      @Override
      public void sendPlayerList(List<String> header, List<String> footer) {
        player.sendPlayerListHeaderAndFooter(
          ChatUtils.parseTab(header),
          ChatUtils.parseTab(footer)
        );
      }

      @Override
      public void resetPlayerList() {
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
      }

      @Override
      public String getName() {
        return player.getUsername();
      }

      @Override
      public UUID getUniqueId() {
        return player.getUniqueId();
      }

      @Override
      public void disconnect(String message) {
        player.disconnect(ChatUtils.parseToComponent(message));
      }
    };
  }

  /** Lädt gespeicherte TOS-Akzeptanzen via Configurate */
  /** Lädt gespeicherte TOS-Akzeptanzen via Configurate */
  private void loadAcceptedData() {
    // 1) data.yml im Proxy-Data-Ordner anlegen, falls noch nicht vorhanden
    dataFile = dataDirectory.resolve("data.yml").toFile();
    if (!dataFile.exists()) {
      try (InputStream in = getClass().getResourceAsStream("/data.yml")) {
        if (in == null) {
          throw new IllegalStateException("Default data.yml nicht im JAR gefunden");
        }
        Files.copy(in, dataFile.toPath());
      } catch (IOException e) {
        throw new IllegalStateException("Konnte default data.yml nicht kopieren", e);
      }
    }

    // 2) Configurate-Loader aufsetzen und Datei einlesen
    loader = YamlConfigurationLoader.builder()
      .path(dataFile.toPath())
      .build();
    try {
      dataConfigNode = loader.load();
    } catch (IOException e) {
      throw new IllegalStateException("Kann data.yml nicht laden", e);
    }

    // 3) Inhalte in die Map acceptedMap packen
    //    node("accepted") gibt uns die Section mit allen UUID-Unterschlüsseln
    Map<Object, ? extends ConfigurationNode> children =
      dataConfigNode.node("accepted").childrenMap();
    for (Map.Entry<Object, ? extends ConfigurationNode> entry : children.entrySet()) {
      String key = entry.getKey().toString();
      ConfigurationNode node = entry.getValue();
      UUID uuid = UUID.fromString(key);
      String name = node.node("name").getString("");
      String time = node.node("time").getString("");
      acceptedMap.put(uuid, new AcceptedData(name, time));
    }

    // 4) Shared-StorageTool konfigurieren, damit es in der Shared-Logik genutzt werden kann
    StorageTool.setDataConfigNode(dataConfigNode);
  }

  /** Speichert {@code acceptedMap} via Configurate */
  private void saveAcceptedData() {
    try {
      // 1) Grab the "accepted" section
      ConfigurationNode acceptedNode = dataConfigNode.node("accepted");

      // 2) Remove every existing child under "accepted"
      //    (we make a copy of the key set to avoid concurrent‐modification)
      for (Object childKey : new ArrayList<>(acceptedNode.childrenMap().keySet())) {
        acceptedNode.removeChild(childKey);
      }

      // 3) Put back our up-to-date entries
      for (Map.Entry<UUID, AcceptedData> e : acceptedMap.entrySet()) {
        String key = e.getKey().toString();
        acceptedNode.node(key, "name").set(e.getValue().getName());
        acceptedNode.node(key, "time").set(e.getValue().getTime());
      }

      // 4) Finally write the file
      loader.save(dataConfigNode);
    } catch (IOException ex) {
      logger.error("Failed to save data.yml", ex);
    }
  }

  @Subscribe
  public void onPluginMessage(PluginMessageEvent event) {
    MinecraftChannelIdentifier id = MinecraftChannelIdentifier.create("piston", "queue");
    if (!event.getIdentifier().equals(id)) {
      return;
    }

    // logger.info("<<< Velocity hat PluginMessage erhalten: channel=\"" + id + "\"");

    ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
    String subChannel = in.readUTF();
    // logger.info("<<< Velocity SubChannel: " + subChannel);

    if ("ACCEPTED".equals(subChannel)) {
      // ─── ACCEPTED ───
      UUID uuid      = UUID.fromString(in.readUTF());
      String name    = in.readUTF();
      String timestamp = in.readUTF();

      acceptedMap.put(uuid, new AcceptedData(name, timestamp));
      saveAcceptedData();

      LuckPerms api = LuckPermsProvider.get();
      Node tosNode = Node.builder("piston.valeqs.tos.accepted").value(true).build();
      api.getUserManager()
        .modifyUser(uuid, u -> u.data().add(tosNode))
        .thenRun(() -> logger.info("Proxy-LP: Node gesetzt für " + uuid));

      proxyServer.getPlayer(uuid).ifPresent(proxyPlayer -> {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ACCEPTED");
        out.writeUTF(uuid.toString());
        out.writeUTF(name);
        out.writeUTF(timestamp);
        proxyPlayer.getCurrentServer().ifPresent(serverConnection ->
          serverConnection.sendPluginMessage(id, out.toByteArray())
        );
        // logger.info("Velocity → Queue: ACCEPTED für " + uuid);
      });

    } else if ("DECLINE".equals(subChannel) || "TOS_DECLINE".equals(subChannel)) {
      // ─── DECLINE ───
      UUID uuid = UUID.fromString(in.readUTF());
      logger.info("<<< Velocity: TOS DECLINE für " + uuid);
      proxyServer.getPlayer(uuid).ifPresent(p ->
        p.disconnect(Component.text(
          "Du musst die Nutzungsbedingungen akzeptieren, um auf diesem Server spielen zu können.",
          NamedTextColor.GOLD
        ))
      );
    }
  }

  @Subscribe
  public void onServerPreConnect(ServerPreConnectEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();

    if (acceptedMap.containsKey(uuid)) {
      // Bereits TOS akzeptiert? Dann *immer* direkt auf MAIN – egal ob voll oder nicht
      RegisteredServer main = proxyServer.getServer("main")
        .orElseThrow(() -> new IllegalStateException("Main-Server nicht gefunden"));
      event.setResult(ServerPreConnectEvent.ServerResult.allowed(main));
    } else {
      // Noch nicht akzeptiert? In die Queue umleiten
      RegisteredServer queue = proxyServer.getServer("queue")
        .orElseThrow(() -> new IllegalStateException("Queue-Server nicht gefunden"));
      event.setResult(ServerPreConnectEvent.ServerResult.allowed(queue));
    }
  }

  @Override
  public Map<UUID, String> getAcceptedMap() {
    // Konvertiere unser interne Map<UUID,AcceptedData> in Map<UUID,String> (nur Timestamp)
    Map<UUID, String> map = new HashMap<>();
    for (Map.Entry<UUID, AcceptedData> e : acceptedMap.entrySet()) {
      map.put(e.getKey(), e.getValue().getTime());
    }
    return map;
  }

  @Override
  public ConfigurationNode getDataRoot() {
    return dataConfigNode;
  }

  @Override
  public void saveData() {
    saveAcceptedData();
  }

  @Override
  public YamlConfigurationLoader getDataLoader() {
    return loader;
  }

  @Override
  public void updateTab(QueueType queue, Set<String> onlineServers) {
    // Beispielcode – hier muss deine Tablist-Logik rein!
    for (UUID uuid : queue.getQueueMap().keySet()) {
      getPlayer(uuid).ifPresent(player -> {
        if (!onlineServers.contains(Config.TARGET_SERVER)) {
          player.sendPlayerList(Config.RESTART_HEADER, Config.RESTART_FOOTER);
        } else {
          // Hier kommt deine normale Tablist-Logik hin (falls nötig)
        }
      });
    }
  }
}
