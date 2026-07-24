package com.agentcache.cli.command;

import com.agentcache.cli.Config;
import com.agentcache.cli.output.Printer;
import picocli.CommandLine.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * {@code logout} 子命令：删除本地配置文件。
 */
@Command(name = "logout", mixinStandardHelpOptions = true,
        description = "Remove local CLI configuration")
public class LogoutCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Path path = Config.getConfigPath();
        if (!Files.exists(path)) {
            System.out.println("No config file at " + path);
            return 0;
        }
        Files.delete(path);
        System.out.println("Deleted " + path);
        return 0;
    }
}