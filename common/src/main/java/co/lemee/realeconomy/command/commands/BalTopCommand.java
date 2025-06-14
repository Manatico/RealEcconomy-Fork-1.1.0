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
package co.lemee.realeconomy.command.commands;

import co.lemee.realeconomy.account.Account;
import co.lemee.realeconomy.account.AccountManager;
import co.lemee.realeconomy.config.ConfigManager;
import co.lemee.realeconomy.currency.Currency;
import co.lemee.realeconomy.permission.PermissionManager;
import co.lemee.realeconomy.util.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates the command "/baltop [page|currency] [page]" in game.
 */
public abstract class BalTopCommand {
    private static final int PAGE_SIZE = 5; // Amount of players per page

    /**
     * Method to register and build the command.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        createCommand(dispatcher);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        createCommand(dispatcher);
    }

    private static void createCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = Commands
                .literal("balancetop")
                .executes(BalTopCommand::run)
                .then(Commands.argument("currency", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            for (String name : ConfigManager.getConfig().getCurrenciesAsString()) {
                                builder.suggest(name);
                            }
                            return builder.buildFuture();
                        })
                        .executes(BalTopCommand::run)
                        .then(Commands.argument("page", IntegerArgumentType.integer())
                                .suggests((ctx, builder) -> {
                                    for (int i = 1; i <= 3; i++) {
                                        builder.suggest(i);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(BalTopCommand::run)))
                .build();

        dispatcher.getRoot().addChild(root);

        // Adds alias baltop to balancetop.
        dispatcher.register(Commands.literal("baltop").redirect(root).executes(BalTopCommand::run));
    }

    /**
     * Method that's used to execute the functionality for the command.
     * @param context the source of the command.
     * @return integer to complete the command.
     */
    public static int run(CommandContext<CommandSourceStack> context) {
        boolean isPlayer = context.getSource().isPlayer();
        ServerPlayer playerSource = context.getSource().getPlayer();

        // If the source is a player, check for permission.
        if (isPlayer) {
            if (!PermissionManager.hasPermission(playerSource.getUUID(), PermissionManager.BALTOP_PERMISSION)) {
                context.getSource().sendSystemMessage(Component.literal("§cYou need the permission §b" +
                        PermissionManager.BALTOP_PERMISSION +
                        "§c to run this command."));
                return -1;
            }
        }

        // Counts the amount of arguments given.
        int argLength = context.getInput().split(" ").length;

        Currency currency = null;
        int page = 1; // Sets the page to 1 by default.

        // If there is only one argument (baltop) get the default currency.
        if (argLength == 1) {
            currency = ConfigManager.getConfig().getCurrencyByName(ConfigManager.getConfig().getDefaultCurrency());
        }

        // If there are two arguments, check for a page number or currency. Set the currency to the one given
        // or use the default, if a page number is given.
        if (argLength == 2) {
            String currencyArg = StringArgumentType.getString(context, "currency");
            if (Utils.isStringInt(currencyArg)) {
                page = Integer.parseInt(currencyArg);

                currency = ConfigManager.getConfig().getCurrencyByName(ConfigManager.getConfig().getDefaultCurrency());
            } else {
                currency = ConfigManager.getConfig().getCurrencyByName(currencyArg);
            }
        }

        // If there are three arguments, get the currency given and the page number.
        if (argLength == 3) {
            String currencyArg = StringArgumentType.getString(context, "currency");
            currency = ConfigManager.getConfig().getCurrencyByName(currencyArg);

            page = IntegerArgumentType.getInteger(context, "page");
        }

        // If no currency was found, tell the sender.
        if (currency == null) {
            context.getSource().sendSystemMessage(Component.literal(Utils.formatMessage("§cCurrency could not be found.",
                    isPlayer)));
            return -1;
        }

        // If the page number is less than 1, tell the user this is invalid.
        if (page < 1) {
            context.getSource().sendSystemMessage(Component.literal(Utils.formatMessage("§cPage number must be at least 1.",
                    isPlayer)));
            return -1;
        }

        // Set the index based on the page requested.
        int index = (page - 1) * PAGE_SIZE;

        // Get the sorted balances for the queried currency.
        List<Account> balances = AccountManager.sortAccountsByBalance(currency).stream().distinct().collect(Collectors.toList());

        // If the index is higher than the length of balances, tell the player that page doesn't exist.
        if (balances.size() <= index) {
            context.getSource().sendSystemMessage(Component.literal(Utils.formatMessage("§cPage " + page + " does not exist.",
                    isPlayer)));
            return -1;
        }

        List<Account> pageBalances;

        // Get the data for the specified page.
        if (balances.size() - 1 < PAGE_SIZE + index) {
            pageBalances = balances.subList(index, balances.size());
        } else {
            pageBalances = balances.subList(index, PAGE_SIZE);
        }

        // Calculate the total pages based on the balances received.
        int pages = (int) Math.ceil((double) balances.size() / (double) PAGE_SIZE);

        // Create the output string.
        String output = "§7=== §eBalance Top §7(§b" + currency.getPlural() + "§7) ===\n\n";

        // Add the sorted balances to the string.
        for (int i = 0; i < pageBalances.size(); i++) {
            Account account = pageBalances.get(i);
            output += "§7" + (index + i + 1) + " - §b" + account.getUsername() + "§7: §f" + account.getBalance(currency) +
                    "§a\n";
        }
        // Add the page number to the string.
        output += "§7Page " + page + "/" + pages;

        // Send the output string to the sender.
        context.getSource().sendSystemMessage(Component.literal(Utils.formatMessage(output,
                isPlayer)));
        return 1;
    }
}
