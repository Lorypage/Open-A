package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.completion.ArgsCompleter;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.command.exceptions.SyntaxErrorException;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class ConfigCommand implements Command, ArgsCompleter {
    @Override
    public String[] getAliases() {
        return new String[]{"config", "cfg"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        if (arguments.length < 2) throw new SyntaxErrorException(getUsage());

        switch (arguments[1].toLowerCase()) {
            case "save":
            case "load":
                if (arguments.length < 3) throw new SyntaxErrorException(getUsage());
                String fileName = arguments[2] + ".json";
                if (arguments[1].equalsIgnoreCase("save")) {
                    Client.INSTANCE.getConfigManager().saveConfig(fileName);
                } else {
                    Client.INSTANCE.getConfigManager().loadConfig(fileName, true);
                }
                break;

            case "folder":
                try {
                    Desktop.getDesktop().open(Client.INSTANCE.getConfigManager().getConfigDir());
                    ClientUtils.displayChat(Formatting.WHITE + "Opening Config Folder...");
                } catch (Exception e) {
                    throw new CommandExecutionException("Failed to open Config Folder.");
                }
                break;

            default:
                throw new SyntaxErrorException(getUsage());
        }
    }

    @Override
    public java.util.List<String> suggestArgs(String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return Stream.of("save", "load", "folder")
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }


        if (args.length == 3) {
            String sub = args[1].toLowerCase();
            if (!sub.equals("save") && !sub.equals("load")) return java.util.List.of();

            File[] files = Client.INSTANCE.getConfigManager().getConfigDir().listFiles(
                    f -> f.isFile() && f.getName().endsWith(".json")
            );
            if (files == null) return java.util.List.of();

            String partial = args[2].toLowerCase();
            return Arrays.stream(files)
                    .map(f -> f.getName().replace(".json", ""))
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    public String getUsage() {
        return ".config <save|load> [name] | .config folder";
    }
}
