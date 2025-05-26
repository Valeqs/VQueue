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
package net.pistonmaster.pistonqueue.shared.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@AllArgsConstructor
public class QueueType {
  private final Map<UUID, QueuedPlayer> queueMap = Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<Integer, Duration> durationFromPosition = Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<UUID, Map<Integer, Instant>> positionCache = new ConcurrentHashMap<>();
  private final AtomicInteger playersWithTypeInTarget = new AtomicInteger();
  private final String name;
  @Setter
  private int order;
  @Setter
  private String permission;
  @Setter
  private int reservedSlots;
  @Setter
  private List<String> header;
  @Setter
  private List<String> footer;

  public static QueueType getQueueType(PlayerWrapper player) {
    //System.out.println("DEBUG: Spieler " + player.getName() +
    //" hat veteran=" + player.hasPermission("queue.veteran") +
    //", priority=" + player.hasPermission("queue.priority") +
    //", default=" + player.hasPermission("default"));

    // Veteran
    if (player.hasPermission("queue.veteran")) {
      //System.out.println("DEBUG: Rückgabe VETERAN!");
      return Config.QUEUE_TYPES[0];
    }

    // Priority
    if (player.hasPermission("queue.priority")) {
      //System.out.println("DEBUG: Rückgabe PRIORITY!");
      return Config.QUEUE_TYPES[1];
    }

    // Default
    //System.out.println("DEBUG: Rückgabe DEFAULT!");
    return Config.QUEUE_TYPES[2];
  }

  public enum QueueReason {
    SERVER_FULL,
    SERVER_DOWN,
    RECOVERY
  }

  public record QueuedPlayer(
    String targetServer,
    QueueReason queueReason
  ) {}
  public boolean isFull() {
    return queueMap.size() >= reservedSlots;
  }
}
