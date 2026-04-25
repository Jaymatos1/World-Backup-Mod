package com.worldbackup;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class WorldBackupMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // CLIENT_STOPPING fires before Minecraft locks/closes world files
        // Runs synchronously — backup completes before shutdown continues
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            System.out.println("[WorldBackup] Minecraft stopping — running backup...");
            BackupManager manager = new BackupManager();
            String result = manager.runBackup();
            System.out.println("[WorldBackup] " + result);
        });

        System.out.println("[WorldBackup] Loaded! Worlds will back up automatically when Minecraft closes.");
        System.out.println("[WorldBackup] Config: " + System.getProperty("user.home") + "\\Downloads\\worldbackup.config");
    }
}
