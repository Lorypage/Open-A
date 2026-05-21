package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.completion.ArgsCompleter;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.command.exceptions.InvalidArgumentException;
import cn.lazymoon.command.exceptions.SyntaxErrorException;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;
import cn.lazymoon.features.module.Module;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class ToggleCommand implements Command, ArgsCompleter {
    @Override
    public String[] getAliases() {
        return new String[]{"toggle", "t"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        if (arguments.length != 2) throw new SyntaxErrorException(getUsage());

        Module module = Client.INSTANCE.getModuleManager().getModule(arguments[1]);
        if (module == null) throw new InvalidArgumentException(arguments[1], "module not found");

        module.setState(!module.isState());
        ClientUtils.displayChat(String.format("%s%s %s%s",
                module.isState() ? Formatting.AQUA : Formatting.RED,
                module.isState() ? "Enabled" : "Disabled",
                Formatting.WHITE,
                module.getName()));
    }

    @Override
    public String getUsage() {
        return ".toggle [module]";
    }

    @Override
    public List<String> suggestArgs(String[] args) {
        if (args.length != 2) return List.of();
        String partial = args[1].toLowerCase();
        return Client.INSTANCE.getModuleManager().getAllModules().stream()
                .map(Module::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
