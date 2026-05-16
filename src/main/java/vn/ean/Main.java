package vn.ean;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.ean.chicken_farm.ChickenFarm;
import vn.ean.delivery.Delivery;
import vn.ean.economy.Economy;

public class Main extends JavaPlugin {

    private Economy economy;
    private Delivery delivery;
    private ChickenFarm chickenFarm;

    @Override
    public void onEnable() {
        economy = new Economy();
        delivery = new Delivery(this);
        chickenFarm = new ChickenFarm(this);
        getLogger().info("RPCity plugin enabled");
        getServer().getPluginManager().registerEvents(economy, this);
        getServer().getPluginManager().registerEvents(delivery, this);
        getServer().getPluginManager().registerEvents(chickenFarm, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("RPCity plugin disabled");
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
