package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.utils.client.ClientUtils;

import java.util.Arrays;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class HelpCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"help", "h"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        ClientUtils.addMessage("§fAvailable Commands:");
        for (Command command : Client.INSTANCE.getCommandManager().getElements()) {
            String[] aliases = command.getAliases();
            String main = aliases[0];
            String aliasPart = aliases.length > 1
                    ? "(" + String.join(", ", Arrays.copyOfRange(aliases, 1, aliases.length)) + ")"
                    : "";
            ClientUtils.displayChat(String.format("§b%s§7%s §f: %s", main, aliasPart, command.getUsage()));
        }
    }

    @Override
    public String getUsage() {
        return "help/h";
    }
}
