# World Backup Mod

A lightweight Fabric mod that automatically backs up all your Minecraft worlds every time you close the game.

---

## What it does

Every time you quit Minecraft, World Backup silently saves a compressed copy of all your worlds to a backup folder of your choice. No commands. No buttons. Just install it and your worlds are always protected.

---

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2 or higher
- Fabric API
- Windows 10 or 11

---

## Installation

1. Download and install [Fabric Loader](https://fabricmc.net) for Minecraft 26.1.2
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your mods folder
3. Download `worldbackup.jar` and place it in your mods folder
4. Launch Minecraft
5. A config file called `worldbackup.config` will be created automatically in your **Downloads folder**
6. Open `worldbackup.config` and update the `saves_path` line to match your launcher

---

## Configuration

Open `worldbackup.config` from your Downloads folder in any text editor (Notepad works fine).

### Finding your saves path

**Modrinth App:**
```
C:\Users\YOUR_NAME\AppData\Roaming\ModrinthApp\profiles\YOUR_PROFILE_NAME\saves
```

**Vanilla Minecraft Launcher:**
```
C:\Users\YOUR_NAME\AppData\Roaming\.minecraft\saves
```

**CurseForge:**
```
C:\Users\YOUR_NAME\AppData\Roaming\CurseForge\minecraft\Instances\YOUR_PROFILE_NAME\saves
```

**ATLauncher:**
```
C:\Users\YOUR_NAME\AppData\Roaming\ATLauncher\instances\YOUR_PROFILE_NAME\saves
```

Replace `YOUR_NAME` with your Windows username and `YOUR_PROFILE_NAME` with your modpack or profile folder name.

---

### All config options

| Option | Default | Description |
|--------|---------|-------------|
| `saves_path` | (set this) | Path to your Minecraft worlds folder |
| `backup_path` | (set this) | Path where backups will be saved |
| `enable_compression` | true | Compress backups into .zip files |
| `enable_logging` | true | Save a log of every backup |
| `enable_rotation` | true | Auto-delete old backups |
| `max_backups` | 5 | How many backups to keep |

After editing the config, save the file and restart Minecraft for changes to take effect.

---

## How backups work

- Backups are created the moment you close Minecraft
- Each backup is named with the date and time: `Backup_2026-04-25_1430.zip`
- Backups are saved to whichever folder you set as `backup_path`
- When rotation is enabled, the oldest backup is deleted whenever your total exceeds `max_backups`
- A log of all backups is saved to `backup_log.txt` inside your backup folder

---

## Restoring a backup

1. Close Minecraft completely
2. Go to your backup folder
3. Open the `.zip` file you want to restore
4. Copy the world folder(s) inside it to your saves folder
5. Launch Minecraft — your world will be there

---

## Troubleshooting

**No backup appeared after closing Minecraft**
- Open `worldbackup.config` and check that `saves_path` points to the correct folder
- Make sure the folder path exists and contains your world folders
- Check `backup_log.txt` in your backup folder for error messages

**The zip file says it is invalid**
- Try setting `enable_compression=false` in the config
- This will create a regular folder backup instead of a zip

**Config file was not created**
- Make sure Minecraft launched with the mod installed
- Check your Downloads folder at `C:\Users\YOUR_NAME\Downloads\worldbackup.config`

---

## License

MIT — free to use, modify, and distribute.

---

*Made by [Jaymatos](https://modrinth.com/user/Jaymatos)*
