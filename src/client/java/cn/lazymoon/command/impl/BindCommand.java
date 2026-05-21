package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.completion.ArgsCompleter;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.command.exceptions.SyntaxErrorException;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.key.KeyCodeConverter;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.stream.Collectors;

public class BindCommand implements Command, ArgsCompleter {
    @Override
    public String[] getAliases() {
        return new String[]{"bind", "b"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        if (arguments.length != 3) throw new SyntaxErrorException(getUsage());

        Module module = Client.INSTANCE.getModuleManager().getModule(arguments[1]);
        if (module == null) {
            throw new CommandExecutionException(Formatting.RED + arguments[1] + Formatting.RESET + " is an invalid module.");
        }

        int key = KeyCodeConverter.stringToKeycode(arguments[2].toUpperCase());
        String keyName = KeyCodeConverter.keycodeToString(key);

        module.setKey(KeyCodeConverter.getKeyCodeFromName(keyName.toUpperCase()));

        ClientUtils.displayChat(String.format("%sBound %s%s %sto %s%s",
                Formatting.WHITE,
                Formatting.GREEN, module.getName(),
                Formatting.WHITE,
                Formatting.GREEN, key == 0 ? "NONE" : arguments[2].toUpperCase()));
    }

    @Override
    public String getUsage() {
        return ".bind [module] [key]";
    }

    @Override
    public List<String> suggestArgs(String[] args) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            return Client.INSTANCE.getModuleManager().getAllModules().stream()
                    .map(Module::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length == 3) {
            String partial = args[2].toLowerCase();
            return KeyCodeConverter.getKeyNames().stream()
                    .filter(k -> k.startsWith(partial))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}