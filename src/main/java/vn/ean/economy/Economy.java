package vn.ean.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class Economy implements Listener {
    private static final String BILL_NAME_SUFFIX = "vnd";
    private static final Component BILL_LORE_COMPONENT = Component.text("Tiền dùng để giao dịch").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY);
    private static final int[] BILL_LEVELS = {
            500000,
            200000,
            100000,
            50000,
            20000,
            10000,
            5000,
            2000,
            1000
    };

    public boolean hookCommand(Player player, String[] args) {
        String command = args[1].toLowerCase();
        switch (command) {
            case "give":
                return giveCommand(args);
            case "take":
                return takeCommand(args);
            case "check":
                player.sendMessage(Component.text(checkCommand(args)));
                return true;
            default:
                return false;
        }
    }

    public boolean giveCommand(String[] args) {
        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            return false;
        }

        return give(player, Integer.parseInt(args[3]));
    }

    public boolean takeCommand(String[] args) {
        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            return false;
        }

        return take(player, Integer.parseInt(args[3]));
    }

    public int checkCommand(String[] args) {
        Player player = Bukkit.getPlayer(args[2]);
        if (player == null) {
            return 0;
        }

        return check(player);
    }

    public boolean give(Player player, int amount) {
        amount = toValidAmount(amount);

        ArrayList<ItemStack> bills = getBills(amount);
        Location location = player.getEyeLocation();
        Vector direction = location.getDirection().normalize();
        Location dropLocation = location.add(direction.multiply(1));

        for(ItemStack item : bills) {
            Item dropped = player.getWorld().dropItem(dropLocation, item);
            dropped.setVelocity(direction.multiply(0.3));
        }

        return true;
    }

    public int check(Player player) {
        int total = 0;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        for (ItemStack item : contents) {
            if (item == null) {
                continue;
            }

            for (int value : BILL_LEVELS) {
                if (item.isSimilar(getBill(value))) {
                    total += value * item.getAmount();
                }
            }
        }

        return total;
    }

    public boolean take(Player player, int amount) {
        amount = toValidAmount(amount);

        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        Map<Integer, List<Integer>> slotsByValue = new LinkedHashMap<>();
        for (int value : BILL_LEVELS) {
            slotsByValue.put(value, new ArrayList<>());
        }

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }

            for (int value : BILL_LEVELS) {
                if (item.isSimilar(getBill(value))) {
                    for (int i = 0; i < item.getAmount(); i++) {
                        slotsByValue.get(value).add(slot);
                    }
                }
            }
        }

        int collected = 0;
        Map<Integer, Integer> usedBills = new LinkedHashMap<>();
        for (int value : BILL_LEVELS) {
            List<Integer> slots = slotsByValue.get(value);
            for (int slot : slots) {
                if (collected >= amount) {
                    break;
                }
                if (collected + value > amount) {
                    continue;
                }

                collected += value;
                usedBills.put(
                        value,
                        usedBills.getOrDefault(value, 0) + 1
                );
            }
        }

        if (collected < amount) {
            boolean found = false;
            for (int value : BILL_LEVELS) {
                List<Integer> slots = slotsByValue.get(value);
                if (slots.isEmpty()) {
                    continue;
                }

                collected += value;
                usedBills.put(
                        value,
                        usedBills.getOrDefault(value, 0) + 1
                );

                found = true;
                break;
            }

            if (!found || collected < amount) {
                return false;
            }
        }

        for (Map.Entry<Integer, Integer> entry : usedBills.entrySet()) {
            int value = entry.getKey();
            int needRemove = entry.getValue();

            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item == null) {
                    continue;
                }

                if (!item.isSimilar(getBill(value))) {
                    continue;
                }
                int remove = Math.min(
                        needRemove,
                        item.getAmount()
                );

                item.setAmount(item.getAmount() - remove);
                if (item.getAmount() <= 0) {
                    inv.setItem(slot, null);
                }
                needRemove -= remove;
                if (needRemove <= 0) {
                    break;
                }
            }
        }

        int change = collected - amount;
        if (change > 0) {
            ArrayList<ItemStack> bills = getBills(change);
            for (ItemStack bill : bills) {
                inv.addItem(bill);
            }
        }

        return true;
    }

    public Map<Integer, Integer> calcBills(int amount) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int value : BILL_LEVELS) {
            int count = amount/value;
            if (count > 0) {
                result.put(value, count);
                amount %= value;
            }
        }
        return result;
    }

    public ArrayList<ItemStack> getBills(int amount) {
        ArrayList<ItemStack> result = new ArrayList<>();

        Map<Integer, Integer> bills = calcBills(amount);
        for (Map.Entry<Integer, Integer> entry : bills.entrySet()) {
            ItemStack item = getBill(entry.getKey());
            item.setAmount(entry.getValue());
            result.add(item);
        }

        return result;
    }

    public ItemStack getBill(int amount) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(format(amount)).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(BILL_LORE_COMPONENT));

        CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
        modelData.setFloats(List.of((float) amount));
        modelData.setStrings(List.of("vnd_" + amount));
        meta.setCustomModelDataComponent(modelData);
        meta.setItemModel(new NamespacedKey("bills", "vietnam_" + amount + "_banknote"));

        item.setItemMeta(meta);
        return item;
    }

    public String format(int amount) {
        return String.format("%,d %s", amount, BILL_NAME_SUFFIX);
    }

    public int toValidAmount(int amount) {
        int mod = amount % 1000;
        return amount - mod;
    }
}
