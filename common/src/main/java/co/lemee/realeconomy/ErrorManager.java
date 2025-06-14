/**
 * This file is part of RealEconomy.
 * <p>
 * RealEconomy is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * <p>
 * RealEconomy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package co.lemee.realeconomy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/**
 * Class that manages errors and sends them to console or player.
 */
public abstract class ErrorManager {
    // List to store all errors in.
    private static final ArrayList<String> errors = new ArrayList<>();

    /**
     * Method to add a new error.
     * @param newError String that represents a new error.
     */
    public static void addError(String newError) {
        errors.add(newError);
    }

    /**
     * Method to get all errors.
     * @return ArrayList of error messages.
     */
    public static ArrayList<String> getErrors() {
        return errors;
    }

    /**
     * Method to print all errors to a given player.
     * @param player The player to send the errors to.
     */
    public static void printErrorsToPlayer(ServerPlayer player) {
        // If no errors exist, return.
        if (errors.toArray().length == 0) {
            return;
        }

        // Create the output string.
        String outputString = "§6RealEconomy Errors: \n§c";

        // Add each error to the string.
        for (String error : errors) {
            outputString = outputString + error.trim() + "\n";
        }

        // Send the string to the player.
        player.sendSystemMessage(Component.literal(outputString.trim()));
    }

    /**
     * Method to print all errors to a given player.
     * @param player The player to send the errors to.
     */
    public static void printErrorsToPlayer(Player player) {
        // If no errors exist, return.
        if (errors.toArray().length == 0) {
            return;
        }

        // Create the output string.
        String outputString = "§6RealEconomy Errors: \n§c";

        // Add each error to the string.
        for (String error : errors) {
            outputString = outputString + error.trim() + "\n";
        }

        // Send the string to the player.
        player.displayClientMessage(Component.literal(outputString.trim()), false);
    }

    /**
     * Method to print all errors to the console.
     */
    public static void printErrorsToConsole() {
        // If no error exists, return.
        if (errors.toArray().length == 0) {
            return;
        }

        // Create the output string.
        String outputString = "RealEconomy Errors: \n";

        // Add all errors to the string.
        for (String error : errors) {
            outputString = outputString + error.trim() + "\n";
        }

        // Send the errors to the console.
        RealEconomy.LOGGER.error(outputString.trim());
    }
}
