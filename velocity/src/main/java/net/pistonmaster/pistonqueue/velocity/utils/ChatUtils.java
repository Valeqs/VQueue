package net.pistonmaster.pistonqueue.velocity.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.pistonmaster.pistonqueue.shared.utils.SharedChatUtils;
import com.velocitypowered.api.proxy.Player;

import java.util.List;
import java.util.stream.Collectors;

public final class ChatUtils {
  private ChatUtils() {}

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

  /**
   * Parst zuerst SharedChatUtils (Platzhalter),
   * dann alle &-Legacy-Codes,
   * und zuletzt alle MiniMessage-Tags (<yellow>, <click:...>, etc.).
   */
  public static Component parseToComponent(String str) {
    return parseToComponent(str, null);
  }

  public static Component parseToComponent(String str, Player player) {
    // Shared-Placeholder auflösen
    String afterShared = SharedChatUtils.parseText(str);

    // (Kein direkter %wait%-Ersatz – wird extern erledigt)

    // 1) &-Codes nach Component
    Component legacyComp = LEGACY.deserialize(afterShared);
    // 2) Component zurück zu String mit §-Codes
    String withSections = LEGACY.serialize(legacyComp);
    // 3) MiniMessage-Tags parsen
    return MINI.deserialize(withSections);
  }

  /**
   * Für Tab-Listen: verbindet Lines und parst sie wie oben.
   */
  public static Component parseTab(List<String> lines) {
    return parseTab(lines, null);
  }

  public static Component parseTab(List<String> lines, Player player) {
    String joined = lines.stream().collect(Collectors.joining("\n"));
    return parseToComponent(joined, player);
  }
}
