package com.agentcache.cli;

import com.agentcache.cli.command.ConfigCommand;
import com.agentcache.cli.command.FilesCommand;
import com.agentcache.cli.command.LoginCommand;
import com.agentcache.cli.command.LogoutCommand;
import com.agentcache.cli.command.SpacesCommand;
import com.agentcache.cli.output.Printer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

import java.util.concurrent.Callable;

/**
 * CLI 入口。声明子命令组并启用标准 help/version 选项。
 *
 * <p>全局选项 {@code --json} 仅在此处声明，通过 {@link IExecutionStrategy}
 * 在派发到任意子命令前将 {@code Printer} 切换到 JSON 模式。</p>
 */
@Command(
        name = "agentcache",
        mixinStandardHelpOptions = true,
        version = "agentcache 0.1.0",
        description = "AgentCache CLI",
        subcommands = {
                LoginCommand.class,
                LogoutCommand.class,
                ConfigCommand.class,
                SpacesCommand.class,
                FilesCommand.class
        })
public class AgentcacheCli implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Output JSON")
    boolean json;

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * 启动入口。
     */
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new AgentcacheCli());
        cmd.setExecutionStrategy(new JsonAwareExecutionStrategy());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            commandLine.getErr().println("Error: " + ex.getMessage());
            return 1;
        });
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    /**
     * 派发前把 {@code --json} 同步到 {@link Printer}。
     */
    static final class JsonAwareExecutionStrategy implements IExecutionStrategy {
        @Override
        public int execute(ParseResult parseResult) throws CommandLine.ExecutionException {
            ParseResult current = parseResult;
            while (current != null) {
                Object command = current.commandSpec().userObject();
                if (command instanceof AgentcacheCli cli) {
                    Printer.setJson(cli.json);
                    break;
                }
                current = current.subcommand();
            }
            return new CommandLine.RunLast().execute(parseResult);
        }
    }
}