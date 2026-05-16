package vn.ean.chicken_farm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import vn.ean.economy.Economy;
import vn.ean.utils.Utils;

import java.util.List;

public class ChickenFarm implements Listener {

    private final JavaPlugin plugin;
    private final Economy economy;

    public ChickenFarm(JavaPlugin plugin) {
        this.plugin = plugin;
        this.economy = new Economy();
    }

    public boolean hookCommand(CommandSender sender, String[] args) {
        String command = args[1].toLowerCase();
        switch (command) {
            case "create":
                return createCommand(args);
            default:
                return false;
        }
    }

    public boolean createCommand(String[] args) {
        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            return false;
        }

        return create(player);
    }

    public boolean create(Player player) {

        return false;
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Chicken baby)
            || !(event.getMother() instanceof Chicken mother)
            || !(event.getFather() instanceof Chicken father)) {
            return;
        }

        int motherRare, fatherRare;
        try {
            motherRare = (int) mother.getPersistentDataContainer().get(new NamespacedKey(plugin, "chicken_farm_rare"), PersistentDataType.INTEGER);
        } catch (Exception e) {
            motherRare = 0;
        }

        try {
            fatherRare = (int) father.getPersistentDataContainer().get(new NamespacedKey(plugin, "chicken_farm_rare"), PersistentDataType.INTEGER);
        } catch (Exception e) {
            fatherRare = 0;
        }

        int babyRare = generateRare(motherRare, fatherRare);
        double weight = generateWeight(babyRare);
        if (babyRare > 0) {
            baby.getPersistentDataContainer().set(new NamespacedKey(plugin, "chicken_farm_rare"), PersistentDataType.INTEGER, babyRare);
            baby.getPersistentDataContainer().set(new NamespacedKey(plugin, "chicken_farm_weight"), PersistentDataType.DOUBLE, weight);

            String title = String.format("§r%s§r §f%.1fkg", getRareTitle(babyRare), weight);
            baby.customName(Component.text(title).decoration(TextDecoration.ITALIC, false));
            baby.setCustomNameVisible(true);

            AttributeInstance scale = baby.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(1 * weight);
            }
        }
    }

    public String getRareTitle(int rare) {
        List<String> titles = List.of(
            "",
            "§7\uD83C\uDF1F",
            "§a\uD83C\uDF1F\uD83C\uDF1F",
            "§e\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F",
            "§6\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F",
            "§c\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F",
            "§4\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F\uD83C\uDF1F"
        );

        return titles.get(rare);
    }

    public double generateWeight(int rare) {
        int preWeight = ++rare;
        double weight = ((double) Utils.rand((int) Utils.percentOf(50, preWeight * 10), (int) Utils.percentOf(150, preWeight * 10))) / 10;
        if (weight < 0.1) {
            return 0.1;
        }

        return weight;
    }

    public int generateRare(int motherRare, int fatherRare) {
        int min = Math.min(motherRare, fatherRare);
        int max = Math.max(motherRare, fatherRare);

        if (Utils.isChance(0.1)) {
            return max + 1;
        } else if (Utils.isChance(10)) {
            return max;
        } else if (Utils.isChance(60)) {
            if (min == 0) {
                return min;
            }
            return min - 1;
        }

        return min;
    }
}
