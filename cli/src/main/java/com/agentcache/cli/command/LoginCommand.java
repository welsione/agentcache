package com.agentcache.cli.command;

import com.agentcache.cli.Config;
import com.agentcache.cli.output.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * {@code login} 子命令：将 API Key 与服务端 URL 写入本地配置。
 *
 * <p>不与后端交互，纯粹是本地凭证落地。</p>
 */
@Command(name = "login", mixinStandardHelpOptions = true,
        description = "Configure API Key and server URL")
public class LoginCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "API Key (ak-...)")
    String apiKey;

    @Option(names = {"--server"},
            description = "Server URL (default: ${DEFAULT-VALUE})",
            defaultValue = "http://localhost:8080")
    String server;

    @Option(names = {"--space"}, description = "Default space ID")
    Long spaceId;

    @Override
    public Integer call() throws Exception {
        Printer printer = new Printer(Printer.isJson());
        Config config = new Config();
        config.setServerUrl(server);
        config.setApiKey(apiKey);
        config.setDefaultSpace(spaceId);
        try {
            config.validate();
        } catch (IllegalArgumentException ex) {
            printer.printError(ex.getMessage());
            return 2;
        }
        config.save();
        System.out.println("Saved config to " + Config.getConfigPath());
        System.out.println("Server: " + config.getServerUrl());
        if (config.getDefaultSpace() != null) {
            System.out.println("Default space: " + config.getDefaultSpace());
        }
        System.out.println("API key: configured");
        return 0;
    }
}