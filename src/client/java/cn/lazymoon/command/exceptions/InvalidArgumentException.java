package cn.lazymoon.command.exceptions;

import lombok.Getter;

@Getter
public class InvalidArgumentException extends CommandExecutionException {
    private final String argument;

    public InvalidArgumentException(String argument, String reason) {
        super("\"" + argument + "\" " + reason);
        this.argument = argument;
    }
}
