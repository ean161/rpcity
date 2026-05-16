package vn.ean.utils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    public static int rand(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    public static double percentOf(double percent, double number) {
        return (percent / 100.0) * number;
    }

    public static boolean isChance(double percent) {

        if (percent <= 0) {
            return false;
        }

        if (percent >= 100) {
            return true;
        }

        return ThreadLocalRandom.current().nextDouble(100.0) < percent;
    }
}
