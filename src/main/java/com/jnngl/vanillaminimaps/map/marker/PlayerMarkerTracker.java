/*
 *  Copyright (C) 2024  JNNGL
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jnngl.vanillaminimaps.map.marker;

import com.jnngl.vanillaminimaps.VanillaMinimaps;
import com.jnngl.vanillaminimaps.config.Config;
import com.jnngl.vanillaminimaps.map.Minimap;
import com.jnngl.vanillaminimaps.map.MinimapLayer;
import com.jnngl.vanillaminimaps.map.SecondaryMinimapLayer;
import com.jnngl.vanillaminimaps.map.icon.MinimapIcon;
import com.jnngl.vanillaminimaps.map.renderer.MinimapIconRenderer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shows other online players on the minimap and keeps their positions up to date.
 * <p>
 * Uses the {@value #ICON_KEY} icon when it is available (icons/other_player.png in the plugin folder, or
 * /minimap/other_player.png inside the jar). Until then a pink dot is drawn instead. The icon is looked up
 * periodically, so it shows up without restarting the server once the file is added.
 */
public class PlayerMarkerTracker implements Runnable, Listener {

  public static final String ICON_KEY = "other_player";

  private static final MinimapIcon PLACEHOLDER_ICON =
      MinimapIcon.dot(ICON_KEY, 5, new Color(242, 127, 165), new Color(170, 89, 116));

  private static final float DEPTH = 0.3F;
  // Already tracked players are kept a bit further away, so they don't flicker on the radius boundary.
  private static final int REMOVE_MARGIN = 16;
  private static final int ICON_LOOKUP_INTERVAL = 100;

  private final VanillaMinimaps plugin;
  private final Map<UUID, Deque<MinimapLayer>> freeLayers = new HashMap<>();
  private MinimapIconRenderer renderer = new MinimapIconRenderer(PLACEHOLDER_ICON);
  private boolean customIcon;
  private int interval = 1;
  private int ticksUntilIconLookup;

  public PlayerMarkerTracker(VanillaMinimaps plugin) {
    this.plugin = plugin;
  }

  public void start() {
    Config.Markers.PlayerMarkers config = Config.instance().markers.playerMarkers;
    if (!config.enabled) {
      return;
    }

    lookupIcon();
    if (!customIcon) {
      plugin.getLogger().info("Icon '" + ICON_KEY + "' not found, other players will be shown as a pink dot "
          + "until icons/" + ICON_KEY + ".png is added.");
    }

    interval = Math.max(1, config.updateInterval);
    Bukkit.getPluginManager().registerEvents(this, plugin);
    Bukkit.getScheduler().runTaskTimer(plugin, this, interval, interval);
  }

  private void lookupIcon() {
    MinimapIcon icon = plugin.iconProvider().getIcon(ICON_KEY);
    if (icon != null) {
      renderer = new MinimapIconRenderer(icon);
      customIcon = true;
    }
  }

  private static String layerKey(UUID target) {
    return Minimap.PLAYER_MARKER_PREFIX + target;
  }

  @Override
  public void run() {
    if (!customIcon && (ticksUntilIconLookup -= interval) <= 0) {
      ticksUntilIconLookup = ICON_LOOKUP_INTERVAL;
      lookupIcon();
      if (customIcon) {
        plugin.getLogger().info("Icon '" + ICON_KEY + "' found, using it for other players.");
        applyRenderer();
      }
    }

    for (Minimap minimap : List.copyOf(plugin.minimapListener().getPlayerMinimaps().values())) {
      update(minimap);
    }
  }

  private void applyRenderer() {
    for (Minimap minimap : plugin.minimapListener().getPlayerMinimaps().values()) {
      for (SecondaryMinimapLayer layer : minimap.secondaryLayers().values()) {
        if (layer instanceof PlayerMarkerMinimapLayer) {
          layer.setRenderer(renderer);
          minimap.updateSecondaryLayer(plugin, layer);
        }
      }
    }
  }

  private static double horizontalDistanceSquared(Location a, Location b) {
    double dx = a.getX() - b.getX();
    double dz = a.getZ() - b.getZ();
    return dx * dx + dz * dz;
  }

  private static boolean isVisible(Player viewer, Player target, Config.Markers.PlayerMarkers config) {
    if (target.isDead() || !viewer.canSee(target)) {
      return false;
    }

    if (config.hideSpectators && target.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }

    if (config.hideInvisible && (target.isInvisible() || target.hasPotionEffect(PotionEffectType.INVISIBILITY))) {
      return false;
    }

    return !config.hideSneaking || !target.isSneaking();
  }

  private void update(Minimap minimap) {
    Config.Markers.PlayerMarkers config = Config.instance().markers.playerMarkers;
    Player viewer = minimap.holder();
    if (!viewer.isOnline()) {
      return;
    }

    Location viewerLocation = viewer.getLocation();
    double radius = Math.max(1, config.radius);
    double keepRadius = radius + REMOVE_MARGIN;

    List<Player> targets = new ArrayList<>();
    for (Player target : viewer.getWorld().getPlayers()) {
      if (target.equals(viewer) || !isVisible(viewer, target, config)) {
        continue;
      }

      double limit = minimap.secondaryLayers().containsKey(layerKey(target.getUniqueId())) ? keepRadius : radius;
      if (horizontalDistanceSquared(viewerLocation, target.getLocation()) <= limit * limit) {
        targets.add(target);
      }
    }

    targets.sort(Comparator.comparingDouble(target -> horizontalDistanceSquared(viewerLocation, target.getLocation())));
    if (targets.size() > config.maxPlayers) {
      targets = targets.subList(0, Math.max(0, config.maxPlayers));
    }

    Set<String> shown = new HashSet<>();
    for (Player target : targets) {
      String key = layerKey(target.getUniqueId());
      shown.add(key);

      Location location = target.getLocation();
      if (minimap.secondaryLayers().get(key) instanceof PlayerMarkerMinimapLayer marker) {
        if (marker.getPositionX() != location.getBlockX() || marker.getPositionZ() != location.getBlockZ()
            || !location.getWorld().equals(marker.getWorld())) {
          marker.setWorld(location.getWorld());
          marker.setPositionX(location.getBlockX());
          marker.setPositionZ(location.getBlockZ());
          minimap.updateSecondaryLayer(plugin, marker);
        }
        continue;
      }

      MinimapLayer baseLayer = obtainLayer(viewer);
      PlayerMarkerMinimapLayer created = new PlayerMarkerMinimapLayer(baseLayer, renderer, config.stickToBorder,
          location.getWorld(), location.getBlockX(), location.getBlockZ(), DEPTH, target.getUniqueId());
      minimap.secondaryLayers().put(key, created);
      plugin.packetSender().spawnLayer(viewer, baseLayer);
      minimap.updateSecondaryLayer(plugin, created);
    }

    Iterator<Map.Entry<String, SecondaryMinimapLayer>> iterator = minimap.secondaryLayers().entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, SecondaryMinimapLayer> entry = iterator.next();
      if (entry.getValue() instanceof PlayerMarkerMinimapLayer marker && !shown.contains(entry.getKey())) {
        iterator.remove();
        releaseLayer(viewer, marker);
      }
    }
  }

  // Layers are reused, so players walking in and out of range don't allocate new map ids on the client.
  private MinimapLayer obtainLayer(Player viewer) {
    Deque<MinimapLayer> free = freeLayers.get(viewer.getUniqueId());
    if (free != null && !free.isEmpty()) {
      return free.pop();
    }

    return plugin.clientsideMinimapFactory().createMinimapLayer(viewer.getWorld(), null);
  }

  private void releaseLayer(Player viewer, PlayerMarkerMinimapLayer marker) {
    plugin.packetSender().despawnLayer(viewer, marker.getBaseLayer());
    freeLayers.computeIfAbsent(viewer.getUniqueId(), k -> new ArrayDeque<>()).push(marker.getBaseLayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID quitting = event.getPlayer().getUniqueId();
    freeLayers.remove(quitting);

    String key = layerKey(quitting);
    for (Minimap minimap : plugin.minimapListener().getPlayerMinimaps().values()) {
      if (minimap.holder().getUniqueId().equals(quitting)) {
        continue;
      }

      if (minimap.secondaryLayers().get(key) instanceof PlayerMarkerMinimapLayer marker) {
        minimap.secondaryLayers().remove(key);
        releaseLayer(minimap.holder(), marker);
      }
    }
  }
}
