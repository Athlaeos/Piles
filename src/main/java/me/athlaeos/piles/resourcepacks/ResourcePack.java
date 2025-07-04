package me.athlaeos.piles.resourcepacks;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.domain.MinecraftVersion;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ResourcePack {
    /*
     * I copied most of this from thesheepdev's Simple Resourcepack, so code credit for resource pack hosting goes to them
     */
    private static File pack;
    private static final Map<MinecraftVersion, ResourcePackDetails> resourcePacks = new HashMap<>();
    private static final String defaultPackLink = "https://github.com/user-attachments/files/17712809/Piles.zip";
    private static final String defaultSha1 = "2bdeb7005e3bad3f9b801da983da7e2ef1fcdae2";
    static {
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_26, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_25, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_24, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_23, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_22_3, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_22_2, null, null);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_22_1, "https://github.com/user-attachments/files/18075765/Piles.zip", "fb88e63894c2d2e67109727d2278dbfb1a8655c8");
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_22, "https://github.com/user-attachments/files/18075765/Piles.zip", "fb88e63894c2d2e67109727d2278dbfb1a8655c8");
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21_5, "https://github.com/user-attachments/files/18075765/Piles.zip", "fb88e63894c2d2e67109727d2278dbfb1a8655c8");
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21_4, "https://github.com/user-attachments/files/18075765/Piles.zip", "fb88e63894c2d2e67109727d2278dbfb1a8655c8");
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21_3, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21_2, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21_1, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_21, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_6, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_5, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_4, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_3, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_2, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20_1, defaultPackLink, defaultSha1);
        mapResourcePackDetails(MinecraftVersion.MINECRAFT_1_20, defaultPackLink, defaultSha1);
    }

    private static void mapResourcePackDetails(MinecraftVersion version, String packLink, String packSha1){
        resourcePacks.put(version, new ResourcePackDetails(packLink, packSha1));
    }

    public static File getPack() {
        return pack;
    }

    public static String getVersion(){
        return Piles.getPluginConfig().getString("resourcepack_version", "1");
    }

    public static void tryStart(){
        if (Host.start()){
            if (ResourcePack.generate()){
                Piles.getInstance().getServer().getPluginManager().registerEvents(new ResourcePackListener(), Piles.getInstance());
            }
        }
    }

    public static void sendUpdate(Player player) {
        if (Host.getData() != null) {
            try {
                player.setResourcePack("http://" + Host.getIp() + ":" + Host.getPort() + "/Piles_" + getVersion());
            } catch (Exception ignored) { }
        }
    }

    public static String getResourcePackURL(){
        ResourcePackDetails details = resourcePacks.get(MinecraftVersion.getServerVersion());
        if (details == null) {
            Piles.logWarning("Warning! An up-to-date resource pack for this version has not yet been made! Using default version. This may not work well or at all");
            return defaultPackLink;
        }
        return details.link;
    }

    public static String getResourcePackSha1(){
        ResourcePackDetails details = resourcePacks.get(MinecraftVersion.getServerVersion());
        if (details == null) return defaultSha1;
        return details.sha1;
    }

    public static boolean downloadDefault(){
        try (BufferedInputStream in = new BufferedInputStream(new URL(getResourcePackURL()).openStream());
             FileOutputStream out = new FileOutputStream("Piles_default.zip")) {
            byte[] data = new byte[1024];
            int read;
            while ((read = in.read(data, 0, 1024)) != -1){
                out.write(data, 0, read);
            }
        } catch (IOException ex){
            Piles.logWarning("Could not download default resource pack");
            ex.printStackTrace();
            return false;
        }
        Zipper.unzipFolder("Piles_default.zip", new File(Piles.getInstance().getDataFolder(), "/resourcepack"));
        return true;
    }

    @SuppressWarnings("all")
    public static boolean generate() {
        File dataFolder = Piles.getInstance().getDataFolder();

        File resourcePackDirectory = new File(dataFolder.getPath() + "/resourcepack");
        if (!resourcePackDirectory.exists()) {
            resourcePackDirectory.mkdirs();
        }

        File cacheDirectory = new File(dataFolder.getPath() + "/cache");
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }

        pack = new File(cacheDirectory.getPath() + "/Piles_" + getVersion() + ".zip");
        if (pack.exists()) {
            boolean ds = pack.delete();
            if (!ds) {
                Piles.logWarning("Could not delete old resource pack");
                return false;
            }
        }

        Zipper.zipFolder(resourcePackDirectory.getPath(), pack.getPath());

        try {
            Host.setData(Files.readAllBytes(pack.toPath()));
        } catch (IOException ex) {
            Piles.logWarning("Could not cache resource pack");
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getDefaultPackLink() {
        return defaultPackLink;
    }

    public static String getDefaultSha1() {
        return defaultSha1;
    }

    private static record ResourcePackDetails(String link, String sha1){}
}
