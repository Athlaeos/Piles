package me.athlaeos.piles.resourcepacks;

import me.athlaeos.piles.Piles;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Host {
    private static byte[] data;
    private static ServerSocket socket = null;
    private static boolean running = true;
    private static int port = Piles.getPluginConfig().getInt("resourcepack_port");
    private static String ip = Piles.getPluginConfig().getString("server_ip");
    private static BukkitTask task = null;

    public static boolean start(){
        try {
            if (port < 0) throw new Exception();
            if (task != null) task.cancel();
            if (socket == null) {
                socket = new ServerSocket(port);
                socket.setSoTimeout(0);
            }
            Piles.logFine("Resource pack hosting established!");
        } catch (Exception ex){
            Piles.logInfo("Resource pack hosting not established! Use /piles resourcepack setup if you want Piles' resource pack hosted for you");
            return false;
        }

        initializeTCP();
        return true;
    }

    public static void stop(){
        if (socket == null) return;
        try {
            running = false;
            socket.close();
            task.cancel();
        } catch (IOException ignored) {
            Piles.logWarning("Could not properly close resource pack hosting");
        }
    }

    private static void initializeTCP(){
        task = Piles.getInstance().getServer().getScheduler().runTaskAsynchronously(Piles.getInstance(), () -> {
            if (data != null) {
                try {
                    Socket s = socket.accept();
                    String response = "HTTP/1.1 200 OK\r\nContent-Type: application/zip\r\nContent-Disposition: attachment; filename=\"Piles_" +  ResourcePack.getVersion() + ".zip\"\r\nContent-Length: " + data.length + "\r\n\r\n";
                    s.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                    s.getOutputStream().write(data);
                    s.getOutputStream().flush();
                } catch (Exception ignored) {}
            }
            if (running) initializeTCP();
        });
    }

    public static byte[] getData() {
        return data;
    }

    public static String getIp() {
        return ip;
    }

    public static void setIp(String ip) {
        Host.ip = ip;
    }

    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        Host.port = port;
    }

    public static void setData(byte[] data) {
        Host.data = data;
    }

    public static void setRunning(boolean running) {
        Host.running = running;
    }
}
