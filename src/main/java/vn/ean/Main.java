package vn.ean;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import vn.ean.economy.Economy;

public class Main extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {
        economy = new Economy();
        getLogger().info("RPCity plugin enabled");
        getServer().getPluginManager().registerEvents(economy, this);
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
                default:
                    break;
            }
        } catch (Exception e) {
            return false;
        }


        return true;
    }
}
