package vn.ean.delivery;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.goals.WanderGoal;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.CommandTrait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import vn.ean.economy.Economy;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Delivery implements Listener {
    private JavaPlugin plugin;
    private Economy economy;

    public Delivery(JavaPlugin plugin) {
        this.plugin = plugin;
        economy = new Economy();
    }

    private static final Random random = new Random();
    private HashMap<String, Object> orders = new HashMap<>();

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
        String[] receivers = {"alex", "steve", "eanvn", "hoaiandev", "tuanang", "mojang", "chysun", "kingwixx", "heenei", "louismc", "wangg", "chuotmc", "huynope", "taco"};

        int serial = rand(111111, 666666);
        int range = rand(50, 300);

        Location playerLoc = player.getLocation();
        Location addressPoint = null;

        int addressGenRetry;
        for (addressGenRetry = 0; addressGenRetry <= 10; addressGenRetry++) {
            addressPoint = getRandomAroundPlayer(player, range);
            if (!addressPoint.clone().add(0, -1, 0).getBlock().getType().isAir()) {
                break;
            }
        }

        if (addressGenRetry == 10) {
            player.sendActionBar("Hiện tại chưa có đơn hàng nào sẵn sàng");
            return false;
        }

        double dx = addressPoint.getX() - playerLoc.getX();
        double dz = addressPoint.getZ() - playerLoc.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        int time = (int) Math.floor(distance * ((double) rand(1, 20) / 10));
        if (time < 10) {
            time = 10 + rand(0, 5);
        }

        int currentBalance = economy.check(player);
        if (currentBalance < 50000) {
            player.sendActionBar("Hãy kiếm thêm tiền để nhận hàng");
            return false;
        }

        int value = rand((int) percentOf(1, currentBalance), (int) percentOf(50, currentBalance));
        if (value < 10000) {
            value = rand(1, 5) * 10000;
        }
        value = value - (value % 1000);

        if (currentBalance <= value) {
            player.sendActionBar("Hiện tại chưa có đơn hàng phù hợp với bạn");
            return false;
        }

        boolean takeState = economy.take(player, value);
        if (!takeState) {
            return false;
        }

        int cost = (int) Math.floor(rand((int) percentOf(5, distance), (int) percentOf(40, distance))) * 1000;
        if (cost < 3000) {
            cost = 3000;
        }

        String receiverName = receivers[rand(0, receivers.length - 1)] + rand(111, 999);

        NPC npc = CitizensAPI.getNPCRegistry()
                .createNPC(EntityType.PLAYER, receiverName);

        npc.data().set("delivery_serial", serial);
        npc.data().set("delivery_value", value);
        npc.data().set("delivery_cost", cost);
        npc.spawn(addressPoint);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc wander --id " + npc.getId() + " --xrange 5 --yrange 2 --zrange 5");

        ItemStack pack = getPackage(serial, String.format("%.0f X, %.0f Y (%.1fm)", addressPoint.getX(), addressPoint.getZ(), distance), receiverName, value, cost, time);

        Location location = player.getEyeLocation();
        Vector direction = location.getDirection().normalize();
        Location dropLocation = location.add(direction.multiply(1));
        Item dropped = player.getWorld().dropItem(dropLocation, pack);
        dropped.setVelocity(direction.multiply(0.3));

        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            if (npc.isSpawned()) {
                npc.destroy();
                player.sendActionBar(receiverName + " đã huỷ đơn hàng do quá thời gian giao");
            }
        }, 20L * time);

        return false;
    }

    public ItemStack getPackage(int serial, String address, String receiverName, int value, int cost, int time) {
        List<Material> packageTypes = List.of(
                Material.BEEF,
                Material.COOKED_BEEF,
                Material.APPLE,
                Material.CAKE,
                Material.COOKED_SALMON,
                Material.COOKED_CHICKEN
        );

        ItemStack item = new ItemStack(packageTypes.get(rand(0, packageTypes.size() - 1)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Đơn hàng của " + receiverName).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Vị trí: " + address).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY),
                Component.text("Thời gian giao: " + time + " giây").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY),
                Component.text("Tiền món: " + economy.format(value)).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY),
                Component.text("Tiền ship: " + economy.format(cost)).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "delivery_serial"),
                PersistentDataType.INTEGER,
                serial
        );

//        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
//        modelData.setFloats(List.of((float) 10000));
//        modelData.setStrings(List.of("cargo_box"));
//        meta.setCustomModelDataComponent(modelData);
//        meta.setItemModel(new NamespacedKey("bills", "cargo_box"));

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onDelivery(NPCRightClickEvent event) {
        Player player = event.getClicker();
        NPC npc = event.getNPC();
        if (npc.data() == null) {
            return;
        }

        int packId = npc.data().get("delivery_serial");
        int value = npc.data().get("delivery_value");
        int cost = npc.data().get("delivery_cost");
        ItemStack inHand = player.getInventory().getItemInMainHand();
        ItemMeta meta = inHand.getItemMeta();

        int serial = 0;
        try {
            serial = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "delivery_serial"), PersistentDataType.INTEGER);
        } catch (Exception e) {
            return;
        }
        if (packId != serial) {
            return;
        }

        player.getInventory().setItemInMainHand(null);
        economy.give(player, cost + value);
        npc.setSneaking(true);

        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            npc.destroy();
        }, 10L);
    }

    public int rand(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static double percentOf(double percent, double number) {
        return (percent / 100.0) * number;
    }

    public Location getRandomAroundPlayer(Player player, double radius) {
        Random random = new Random();

        double angle = random.nextDouble() * Math.PI * 2; // 0 -> 2π
        double distance = random.nextDouble() * radius;    // 0 -> radius

        double offsetX = Math.cos(angle) * distance;
        double offsetZ = Math.sin(angle) * distance;

        Location base = player.getLocation();

        return new Location(
                base.getWorld(),
                base.getX() + offsetX,
                base.getY(),
                base.getZ() + offsetZ,
                base.getYaw(),
                base.getPitch()
        );
    }
}
