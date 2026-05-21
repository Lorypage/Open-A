package cn.lazymoon.command.completion;


import cn.lazymoon.Client;
import cn.lazymoon.command.Command;

import java.util.ArrayList;
import java.util.List;

public class CommandCompleter {

    public static List<String> getSuggestions(String[] args) {
        List<String> result = new ArrayList<>();

        if (args.length <= 1) {
            String partial = args[0].toLowerCase();
            for (Command cmd : Client.INSTANCE.getCommandManager().getElements()) {
                for (String alias : cmd.getAliases()) {
                    if (alias.startsWith(partial)) result.add(alias);
                }
            }
            return result;
        }

        Command matched = null;
        for (Command cmd : Client.INSTANCE.getCommandManager().getElements()) {
            for (String alias : cmd.getAliases()) {
                if (alias.equalsIgnoreCase(args[0])) {
                    matched = cmd;
                    break;
                }
            }
        }

        if (matched == null) return result;

        if (matched instanceof ArgsCompleter completer) {
            result.addAll(completer.suggestArgs(args));
        }

        return result;
    }
}
