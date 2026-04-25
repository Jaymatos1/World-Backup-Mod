package com.worldbackup;

import java.io.*;
import java.nio.file.*;

public class ConfigManager {

    // Config file goes in the user's Downloads folder for easy access
    private static final Path CONFIG_FILE = Paths.get(
        System.getProperty("user.home"), "Downloads", "worldbackup.config"
    );

    // Defaults
    public boolean enableCompression = true;
    public boolean enableLogging     = true;
    public boolean enableRotation    = true;
    public int     maxBackups        = 5;
    public String  savesPath         = "";
    public String  backupPath        = "";

    public ConfigManager() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
        } catch (IOException e) {
            System.out.println("[WorldBackup] Could not load config, using defaults. Error: " + e.getMessage());
            applyDefaults();
        }
    }

    private void applyDefaults() {
        String appdata = System.getenv("APPDATA");
        this.savesPath  = appdata + "\\ModrinthApp\\profiles\\DEFAULT_PATH\\saves";
        this.backupPath = appdata + "\\ModrinthApp\\profiles\\DEFAULT_PATH\\backups";
    }

    private void writeDefaults() throws IOException {
        String appdata  = System.getenv("APPDATA");
        String defaultSaves  = appdata + "\\ModrinthApp\\profiles\\DEFAULT_PATH\\saves";
        String defaultBackup = appdata + "\\ModrinthApp\\profiles\\DEFAULT_PATH\\backups";

        String content =
            "# ============================================================\n" +
            "#   WorldBackup Mod — Configuration File\n" +
            "#   by Jaymatos\n" +
            "#\n" +
            "#   Edit this file to customize your backup settings.\n" +
            "#   Lines starting with # are comments and are ignored.\n" +
            "#   Save the file after editing, then restart Minecraft.\n" +
            "# ============================================================\n" +
            "\n" +
            "# ------------------------------------------------------------\n" +
            "#   PATHS\n" +
            "# ------------------------------------------------------------\n" +
            "\n" +
            "# Where your Minecraft worlds are saved.\n" +
            "# Replace DEFAULT_PATH with your actual profile folder name.\n" +
            "#\n" +
            "# Examples by launcher:\n" +
            "#   Modrinth App:\n" +
            "#     " + appdata + "\\ModrinthApp\\profiles\\YOUR_PROFILE_NAME\\saves\n" +
            "#   Vanilla Minecraft Launcher:\n" +
            "#     " + appdata + "\\.minecraft\\saves\n" +
            "#   CurseForge:\n" +
            "#     " + appdata + "\\CurseForge\\minecraft\\Instances\\YOUR_PROFILE_NAME\\saves\n" +
            "#   ATLauncher:\n" +
            "#     " + appdata + "\\ATLauncher\\instances\\YOUR_PROFILE_NAME\\saves\n" +
            "saves_path=" + defaultSaves + "\n" +
            "\n" +
            "# Where your backups will be saved.\n" +
            "# You can change this to any folder you want, e.g. an external drive.\n" +
            "# Example: D:\\MinecraftBackups\n" +
            "backup_path=" + defaultBackup + "\n" +
            "\n" +
            "# ------------------------------------------------------------\n" +
            "#   FEATURES\n" +
            "# ------------------------------------------------------------\n" +
            "\n" +
            "# Compress backups into a .zip file to save disk space.\n" +
            "# true = compress (recommended) | false = copy files as-is\n" +
            "enable_compression=true\n" +
            "\n" +
            "# Save a log of every backup to backup_log.txt in your backup folder.\n" +
            "# true = save log | false = no log\n" +
            "enable_logging=true\n" +
            "\n" +
            "# Automatically delete old backups to save disk space.\n" +
            "# true = auto-delete old backups | false = keep all backups forever\n" +
            "enable_rotation=true\n" +
            "\n" +
            "# How many backups to keep when rotation is enabled.\n" +
            "# The oldest backup is deleted when this number is exceeded.\n" +
            "# Example: 5 = always keep your 5 most recent backups\n" +
            "max_backups=5\n";

        Files.writeString(CONFIG_FILE, content);
        System.out.println("[WorldBackup] Config file created at: " + CONFIG_FILE);
        System.out.println("[WorldBackup] Please open worldbackup.config in your Downloads folder and set your saves_path.");
    }

    private void load() throws IOException {
        for (String line : Files.readAllLines(CONFIG_FILE)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("=", 2);
            if (parts.length != 2) continue;

            String key   = parts[0].trim();
            String value = parts[1].trim();

            switch (key) {
                case "saves_path"         -> savesPath         = value;
                case "backup_path"        -> backupPath        = value;
                case "enable_compression" -> enableCompression = value.equalsIgnoreCase("true");
                case "enable_logging"     -> enableLogging     = value.equalsIgnoreCase("true");
                case "enable_rotation"    -> enableRotation    = value.equalsIgnoreCase("true");
                case "max_backups"        -> {
                    try { maxBackups = Integer.parseInt(value); }
                    catch (NumberFormatException e) { maxBackups = 5; }
                }
            }
        }
        System.out.println("[WorldBackup] Config loaded from: " + CONFIG_FILE);
    }

    public Path getConfigPath() {
        return CONFIG_FILE;
    }
}
