package cn.lazymoon.command;

import cn.lazymoon.Client;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.command.exceptions.InvalidArgumentException;
import cn.lazymoon.command.exceptions.SyntaxErrorException;
import cn.lazymoon.command.impl.*;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.misc.MessageEvent;
import cn.lazymoon.utils.client.ClientUtils;


import java.util.Arrays;
import static net.minecraft.util.Formatting.*;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */

public class CommandManager extends Manager<Command> {
    private static final String PREFIX = ".";
    private static final String HELP_MESSAGE = "Try \"" + PREFIX + "help\" for help.";
    private static final String SYNTAX_ERR = "Invalid command syntax. Hint: ";
    private static final String INVALID_ARG = "Invalid arguments. ";

    public CommandManager(){
        super(Arrays.asList(
                new BindCommand(),
                new BindsCommand(),
                new ConfigCommand(),
                new HelpCommand(),
                new HideCommand(),
                new ToggleCommand()
        ));
        Client.INSTANCE.getEventManager().register(this);
    }

    @EventTarget
    public void onMessageSent(MessageEvent event) {
        String message;
        if ((message = event.getMessage()).startsWith(PREFIX)) {
            event.setCancelled(true);
            String removedPrefix = message.substring(1);
            String[] arguments = removedPrefix.split(" ");
            if (!removedPrefix.isEmpty() && arguments.length > 0) {
                for (Command command : getElements()) {
                    for (String alias : command.getAliases()) {
                        if (alias.equalsIgnoreCase(arguments[0])) {
                            try {
                                command.execute(arguments);
                            } catch (SyntaxErrorException e) {
                                ClientUtils.displayChat(RED + SYNTAX_ERR + e.getMessage());
                            } catch (InvalidArgumentException e){
                                ClientUtils.displayChat(RED + INVALID_ARG + e.getMessage());
                            } catch (CommandExecutionException e) {
                                ClientUtils.displayChat(RED + e.getMessage());
                            } catch (Exception e) {
                                ClientUtils.displayChat(RED + "Unexpected error: " + e.getMessage() + "(See console for details)");
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                }
                ClientUtils.displayChat(RED + "\"" + arguments[0] + "\" is not a valid command. " + HELP_MESSAGE);
            } else {
                ClientUtils.displayChat(RED + "No arguments were supplied. " + HELP_MESSAGE);
            }
        }
    }
}
