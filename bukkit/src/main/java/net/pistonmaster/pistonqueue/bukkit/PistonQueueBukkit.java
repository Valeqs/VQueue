/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.bukkit;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.util.Collections;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.util.UUID;


@Getter
public final class PistonQueueBukkit extends JavaPlugin {
  private boolean forceLocation;

  private String forcedWorldName;
  private int forcedX;
  private int forcedY;
  private int forcedZ;

  private boolean hidePlayers;
  private boolean disableChat;
  private boolean disableCmd;
  private boolean restrictMovement;
  private boolean forceGamemode;
  private String forcedGamemode;

  private boolean team;
  private String teamName;

  private boolean preventExperience;
  private boolean preventDamage;
  private boolean preventHunger;

  private boolean protocolLib;
  private boolean disableDebug;

  private boolean noChunkPackets;
  private boolean noTimePackets;
  private boolean noHealthPackets;
  private boolean noAdvancementPackets;
  private boolean noExperiencePackets;
  private boolean showHeadPacket;

  private boolean playXP;

  @Getter
  private final Map<UUID, String> acceptedMap = new HashMap<>();

  @Getter
  private ConfigurationNode dataRoot;

  @Getter
  private YamlConfigurationLoader dataLoader;

  @SuppressWarnings("deprecation")
  @Override
  public void onEnable() {
    saveDefaultConfig();

    Path dataFile = getDataFolder().toPath().resolve("data.yml");
    dataLoader = YamlConfigurationLoader.builder().path(dataFile).build();

    try {
      dataRoot = dataLoader.load();
    } catch (IOException e) {
      getLogger().severe("Konnte data.yml nicht laden");
      e.printStackTrace();
    }

    this.getCommand("tos").setExecutor(new net.pistonmaster.pistonqueue.bukkit.commands.TosCommand(this));
    loadTosPages();

    getServer().getMessenger().registerIncomingPluginChannel(this, "piston:queue", new QueuePluginMessageListener(this));
    getServer().getMessenger().registerOutgoingPluginChannel(this, "piston:queue");

    getServer().getPluginManager().registerEvents(new TosListener(this), this);
    getServer().getPluginManager().registerEvents(new ServerListener(this), this);

    Logger log = getLogger();
    log.info(ChatColor.BLUE + "PistonQueue V" + getDescription().getVersion());

    log.info(ChatColor.BLUE + "Loading config");

    forceLocation = getConfig().getBoolean("forceLocation");
    forcedWorldName = getConfig().getString("forcedWorldName");
    forcedX = getConfig().getInt("forcedX");
    forcedY = getConfig().getInt("forcedY");
    forcedZ = getConfig().getInt("forcedZ");
    hidePlayers = getConfig().getBoolean("hidePlayers");
    restrictMovement = getConfig().getBoolean("restrictMovement");
    forceGamemode = getConfig().getBoolean("forceGamemode");
    disableChat = getConfig().getBoolean("disableChat");
    disableCmd = getConfig().getBoolean("disableCmd");
    forcedGamemode = getConfig().getString("forcedGamemode");
    team = getConfig().getBoolean("team");
    teamName = getConfig().getString("teamName");

    preventExperience = getConfig().getBoolean("preventExperience");
    preventDamage = getConfig().getBoolean("preventDamage");
    preventHunger = getConfig().getBoolean("preventHunger");

    disableDebug = getConfig().getBoolean("disableDebug");

    noChunkPackets = getConfig().getBoolean("noChunkPackets");
    noTimePackets = getConfig().getBoolean("noTimePackets");
    noHealthPackets = getConfig().getBoolean("noHealthPackets");
    noAdvancementPackets = getConfig().getBoolean("noAdvancementPackets");
    noExperiencePackets = getConfig().getBoolean("noExperiencePackets");
    showHeadPacket = getConfig().getBoolean("showHeadPacket");

    playXP = getConfig().getBoolean("playXP");

    log.info(ChatColor.BLUE + "Preparing server");
    if (hidePlayers) {
      for (World world : getServer().getWorlds()) {
        world.setGameRuleValue("announceAdvancements", "false");
      }

      log.info(ChatColor.BLUE + "Game-rule announceAdvancements was set to false because hidePlayers was true.");
    }

    log.info(ChatColor.BLUE + "Looking for hooks");
    if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      log.info(ChatColor.BLUE + "Hooked into ProtocolLib");
      protocolLib = true;

      ProtocolLibWrapper.setupProtocolLib(this);
    } else {
      log.info(ChatColor.YELLOW + "It is recommended to install ProtocolLib");
    }

    log.info(ChatColor.BLUE + "Registering listeners");

    log.info(ChatColor.BLUE + "Checking for a newer version");
    try {
      String currentVersionString = this.getDescription().getVersion();
      SemanticVersion gitHubVersion = new GitHubUpdateChecker()
        .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
      SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

      if (gitHubVersion.isNewerThan(currentVersion)) {
        log.info(ChatColor.RED + "There is an update available!");
        log.info(ChatColor.RED + "Current version: " + currentVersionString + " New version: " + gitHubVersion);
        log.info(ChatColor.RED + "Download it at: https://modrinth.com/plugin/pistonqueue");
      } else {
        log.info(ChatColor.BLUE + "You're up to date!");
      }
    } catch (IOException e) {
      log.severe("Could not check for updates!");
      e.printStackTrace();
    }
  }

  private final Map<String, List<String>> tosPages = new HashMap<>();

  private void loadTosPages() {
    ConfigurationSection root = getConfig().getConfigurationSection("tos-pages");
    if (root == null) return;
    for (String localeKey : root.getKeys(false)) {
      ConfigurationSection loc = root.getConfigurationSection(localeKey);
      // sortiere nach page_1, page_2, …
      Map<Integer, String> sorted = new TreeMap<>();
      for (String pageKey : loc.getKeys(false)) {
        if (pageKey.startsWith("page_")) {
          int idx = Integer.parseInt(pageKey.substring(5));
          sorted.put(idx, loc.getString(pageKey));
        }
      }
      tosPages.put(localeKey, new ArrayList<>(sorted.values()));
    }
  }

  public ItemStack getTosBook(Player player) {
    String loc = player.getLocale().substring(0, 2).toLowerCase();

    // versuche erst loc, dann en, dann eine leere Liste
    List<String> pages = tosPages.get(loc);
    if (pages == null) {
      pages = tosPages.get("en");
    }
    if (pages == null) {
      pages = Collections.emptyList();
    }

    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) book.getItemMeta();
    meta.setTitle("TOS");
    meta.setAuthor("Server");

    // wenn pages leer, kommt ein leeres Buch
    List<Component> comps = pages.stream()
      .map(text -> MiniMessage.miniMessage().deserialize(text))
      .collect(Collectors.toList());
    meta.pages(comps);
    book.setItemMeta(meta);
    return book;
  }

  @Override
  public void onDisable() {
    this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
  }
}
