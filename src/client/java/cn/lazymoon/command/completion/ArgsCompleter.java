package cn.lazymoon.command.completion;

import java.util.List;

public interface ArgsCompleter {
    List<String> suggestArgs(String[] args);
}
