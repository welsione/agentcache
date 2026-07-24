package com.agentcache.cli.command;

import com.agentcache.cli.Config;
import com.agentcache.cli.output.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * {@code config} 子命令组：显示 / 修改本地配置。
 */
@Command(name = "config", mixinStandardHelpOptions = true,
        description = "Manage local CLI configuration",
        subcommands = { ConfigCommand.ShowCommand.class, ConfigCommand.SetServerCommand.class })
public class ConfigCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: agentcache config <show|set-server>");
        return 0;
    }

    /**
     * 显示当前配置（API Key 脱敏）。
     */
    @Command(name = "show", mixinStandardHelpOptions = true, description = "Show current configuration")
    public static class ShowCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Config config = Config.load();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("serverUrl", config.getServerUrl());
            snapshot.put("apiKey", mask(config.getApiKey()));
            snapshot.put("defaultSpace", config.getDefaultSpace());
            snapshot.put("configPath", Config.getConfigPath().toString());

            Printer printer = new Printer(Printer.isJson());
            if (Printer.isJson()) {
                printer.printObject(snapshot);
            } else {
                snapshot.forEach((k, v) -> System.out.println(k + ": " + v));
            }
            return 0;
        }

        private static String mask(String apiKey) {
            if (apiKey == null || apiKey.isEmpty()) {
                return "(not set)";
            }
            int dash = apiKey.indexOf('-');
            String prefix = dash > 0 ? apiKey.substring(0, dash + 1) : apiKey.substring(0, Math.min(3, apiKey.length()));
            return prefix + "***";
        }
    }

    /**
     * 写入新的服务端 URL。
     */
    @Command(name = "set-server", mixinStandardHelpOptions = true,
            description = "Update server URL in config")
    public static class SetServerCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Server URL (http:// or https://)")
        String url;

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = Config.load();
            config.setServerUrl(url);
            try {
                config.validate();
            } catch (IllegalArgumentException ex) {
                printer.printError(ex.getMessage());
                return 2;
            }
            config.save();
            System.out.println("Server URL updated to " + config.getServerUrl());
            return 0;
        }
    }
}