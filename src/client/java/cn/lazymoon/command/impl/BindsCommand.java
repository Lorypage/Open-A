package cn.lazymoon.command.impl;

import cn.lazymoon.Client;
import cn.lazymoon.command.Command;
import cn.lazymoon.command.exceptions.CommandExecutionException;
import cn.lazymoon.utils.client.ClientUtils;
import net.minecraft.util.Formatting;
import cn.lazymoon.features.module.Module;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class BindsCommand implements Command {
    @Override
    public String[] getAliases() {
        return new String[]{"binds", "bs"};
    }

    @Override
    public void execute(String[] arguments) throws CommandExecutionException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (Module module : Client.INSTANCE.getModuleManager().getAllModules()) {
            if (module.key != 0) {
                sb.append(Formatting.AQUA).append(module.name).append(" ").append(Formatting.WHITE).append(module.getKey()).append(Formatting.RESET).append("\n");
            }
        }

        if (sb.length() == 1) {
            throw new CommandExecutionException(Formatting.RED + "No module was bound.");
        }

        ClientUtils.displayChat(Formatting.WHITE + "List of bound modules:" + Formatting.RESET);
        ClientUtils.displayChat(sb.toString());
    }

    @Override
    public String getUsage() {
        return "";
    }
}
