package cn.lazymoon.command.exceptions;

public class SyntaxErrorException extends CommandExecutionException{
    public SyntaxErrorException(String message) {
        super(message);
    }
}
