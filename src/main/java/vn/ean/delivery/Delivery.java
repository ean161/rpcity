package vn.ean.delivery;

import net.citizensnpcs.api.CitizensAPI;
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
        String[] receivers = {"alex", "steve", "hoaiandev", "tuanang"};

        int serial = rand(111111, 666666);
        int range = rand(50, 300);
        Location addressPoint = getRandomAroundPlayer(player, range);
        int currentBalance = economy.check(player);
        if (currentBalance < 50000) {
            player.sendActionBar("Hãy kiếm thêm tiền để nhận hàng");
            return false;
        }

        int value = rand(30, currentBalance / 1000 * 3) * 1000;
        if (currentBalance <= value) {
            player.sendActionBar("Hiện tại chưa có đơn hàng phù hợp với bạn");
            return false;
        }
        boolean takeState = economy.take(player, value);
        if (!takeState) {
            return false;
        }

        int cost = Math.floorDiv(range, rand(2, 10)) * 1000;
        String receiverName = receivers[rand(0, receivers.length - 1)] + rand(111, 999);

        NPC npc = CitizensAPI.getNPCRegistry()
                .createNPC(EntityType.PLAYER, receiverName);
        CommandTrait trait = npc.getOrAddTrait(CommandTrait.class);
        npc.data().set("delivery_serial", serial);
        npc.data().set("delivery_value", value);
        npc.data().set("delivery_cost", cost);
        npc.spawn(addressPoint);

        ItemStack pack = getPackage(serial, String.format("%s (X: %.0f, Z: %.0f, %dm)", receiverName, addressPoint.getX(), addressPoint.getZ(), range), player, value, cost);

        Location location = player.getEyeLocation();
        Vector direction = location.getDirection().normalize();
        Location dropLocation = location.add(direction.multiply(1));
        Item dropped = player.getWorld().dropItem(dropLocation, pack);
        dropped.setVelocity(direction.multiply(0.3));

        return false;
    }

    public ItemStack getPackage(int serial, String address, Player player, int value, int cost) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Đơn hàng #" + serial).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Người nhận: " + address).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY),
                Component.text("Giá trị đơn: " + economy.format(value)).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY),
                Component.text("Tiền kiếm được: " + economy.format(cost)).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY)
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

        npc.destroy();
    }

    public int rand(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
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
