package vn.ean;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.ean.chicken_farm.ChickenFarm;
import vn.ean.delivery.Delivery;
import vn.ean.economy.Economy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {

    private Economy economy;
    private Delivery delivery;
    private ChickenFarm chickenFarm;
    private String ip = "", licenseCode = "";

    @Override
    public void onEnable() {
        initClientLicense();

        economy = new Economy();
        delivery = new Delivery(this);
        chickenFarm = new ChickenFarm(this);

        if (!hasLicense()) {
            getLogger().severe("Can't verify license " + licenseCode + ", please contact via nghoaian161@gmail.com");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("RPCity plugin enabled for license " + licenseCode);
        getServer().getPluginManager().registerEvents(economy, this);
        getServer().getPluginManager().registerEvents(delivery, this);
        getServer().getPluginManager().registerEvents(chickenFarm, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("RPCity plugin disabled");
    }

    public void initClientLicense()  {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            this.ip = br.readLine();
        } catch (Exception e) {
            this.ip = "";
        }
        licenseCode = "RPCity/" + ip;
    }

    public boolean hasLicense()  {
        String status;
        try {
            String encoded = URLEncoder.encode(licenseCode, StandardCharsets.UTF_8);
            URL url = new URL("https://mcp-license.ean.vn/?code=" + encoded);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

            status = br.readLine();
            return ("APPROVED").equals(status.trim());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().toLowerCase().equals("rpcity")) {
            return false;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "economy":
                    return economy.hookCommand((Player) sender, args);
                case "delivery":
                    return delivery.hookCommand(sender, args);
                case "chicken_farm":
                    return chickenFarm.hookCommand(sender, args);
                default:
                    break;
            }
        } catch (Exception e) {
            getLogger().info(e.getLocalizedMessage());
            return false;
        }

        return true;
    }
}
