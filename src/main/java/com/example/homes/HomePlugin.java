package com.example.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomePlugin extends JavaPlugin implements CommandExecutor {

    private Map<UUID, Map<String, Location>> playerHomes = new HashMap<>();
    private File homesFile;
    private FileConfiguration homesConfig;
    private int maxHomes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxHomes = getConfig().getInt("max-homes", 2);

        homesFile = new File(getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        loadHomes();

        getCommand("sethome").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("deletehome").setExecutor(this);
        getCommand("listhomes").setExecutor(this);
    }

    @Override
    public void onDisable() {
        saveHomes();
    }

    private void loadHomes() {
        ConfigurationSection playersSection = homesConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Location> homes = new HashMap<>();
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                for (String homeName : playerSection.getKeys(false)) {
                    ConfigurationSection homeSection = playerSection.getConfigurationSection(homeName);
                    World world = Bukkit.getWorld(homeSection.getString("world"));
                    if (world == null) continue; // Skip if world not loaded
                    double x = homeSection.getDouble("x");
                    double y = homeSection.getDouble("y");
                    double z = homeSection.getDouble("z");
                    float yaw = (float) homeSection.getDouble("yaw");
                    float pitch = (float) homeSection.getDouble("pitch");
                    Location loc = new Location(world, x, y, z, yaw, pitch);
                    homes.put(homeName, loc);
                }
                playerHomes.put(uuid, homes);
            }
        }
    }

    private void saveHomes() {
        homesConfig.set("players", null); // Clear existing data
        for (Map.Entry<UUID, Map<String, Location>> entry : playerHomes.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Location> homes = entry.getValue();
            for (Map.Entry<String, Location> homeEntry : homes.entrySet()) {
                String name = homeEntry.getKey();
                Location loc = homeEntry.getValue();
                String path = "players." + uuid.toString() + "." + name + ".";
                homesConfig.set(path + "world", loc.getWorld().getName());
                homesConfig.set(path + "x", loc.getX());
                homesConfig.set(path + "y", loc.getY());
                homesConfig.set(path + "z", loc.getZ());
                homesConfig.set(path + "yaw", loc.getYaw());
                homesConfig.set(path + "pitch", loc.getPitch());
            }
        }
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        playerHomes.putIfAbsent(uuid, new HashMap<>());
        Map<String, Location> homes = playerHomes.get(uuid);
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("sethome")) {
            String name = (args.length > 0) ? args[0] : "home";
            if (!homes.containsKey(name) && homes.size() >= maxHomes) {
                player.sendMessage("You can only have up to " + maxHomes + " homes.");
                return true;
            }
            Location loc = player.getLocation();
            homes.put(name, loc);
            player.sendMessage("Home '" + name + "' set.");
            saveHomes();
            return true;
        } else if (cmd.equals("home")) {
            if (args.length == 0) {
                if (homes.size() == 1) {
                    String name = homes.keySet().iterator().next();
                    teleportToHome(player, name, homes);
                } else if (homes.size() > 1) {
                    player.sendMessage("You have multiple homes. Specify: /home <name>");
                    listHomes(player, homes);
                } else {
                    player.sendMessage("You have no homes set.");
                }
            } else {
                String name = args[0];
                teleportToHome(player, name, homes);
            }
            return true;
        } else if (cmd.equals("deletehome")) {
            if (args.length == 0) {
                player.sendMessage("Usage: /deletehome <name>");
                return true;
            }
            String name = args[0];
            if (homes.remove(name) != null) {
                player.sendMessage("Home '" + name + "' deleted.");
                saveHomes();
            } else {
                player.sendMessage("No home named '" + name + "'.");
            }
            return true;
        } else if (cmd.equals("listhomes")) {
            listHomes(player, homes);
            return true;
        }
        return false;
    }

    private void listHomes(Player player, Map<String, Location> homes) {
        if (homes.isEmpty()) {
            player.sendMessage("You have no homes.");
            return;
        }
        player.sendMessage("Your homes:");
        for (String name : homes.keySet()) {
            player.sendMessage("- " + name);
        }
    }

    private void teleportToHome(Player player, String name, Map<String, Location> homes) {
        Location loc = homes.get(name);
        if (loc == null) {
            player.sendMessage("No home named '" + name + "'.");
            return;
        }
        if (!isSafe(loc)) {
            player.sendMessage("Cannot teleport to '" + name + "' (unsafe: lava, suffocation, or fall risk).");
            return;
        }
        player.teleport(loc);
        player.sendMessage("Teleported to '" + name + "'.");
    }

    private boolean isSafe(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        Block floor = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        Block space = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Block above = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        if (!floor.getType().isSolid()) return false; // Risk of falling
        if (!space.getType().isAir()) return false; // Suffocation or lava/water
        if (!above.getType().isAir()) return false; // Suffocation
        return true;
    }
}