package cn.lazymoon.command;

import cn.lazymoon.command.exceptions.CommandExecutionException;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public interface Command {

    String[] getAliases();

    void execute(String[] arguments) throws CommandExecutionException;

    String getUsage();

}
