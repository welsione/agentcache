package com.agentcache.cli.command;

import com.agentcache.cli.Client;
import com.agentcache.cli.Config;
import com.agentcache.cli.dto.SpaceResponse;
import com.agentcache.cli.output.Printer;
import com.fasterxml.jackson.core.type.TypeReference;
import picocli.CommandLine.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * {@code spaces} 子命令组。
 */
@Command(name = "spaces", mixinStandardHelpOptions = true,
        description = "List spaces visible to the current API Key",
        subcommands = { SpacesCommand.ListCommand.class })
public class SpacesCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Usage: agentcache spaces list");
        return 0;
    }

    /**
     * 列出当前 API Key 可见的空间。
     */
    @Command(name = "list", mixinStandardHelpOptions = true, description = "List spaces")
    public static class ListCommand implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Printer printer = new Printer(Printer.isJson());
            Config config = requireAuth(printer);
            if (config == null) {
                return 2;
            }
            try (Client client = new Client(config)) {
                List<SpaceResponse> spaces = client.get(
                        "/api/spaces", new TypeReference<List<SpaceResponse>>() {}, null);
                if (spaces == null) {
                    spaces = List.of();
                }
                if (Printer.isJson()) {
                    printer.printObject(spaces);
                } else if (spaces.isEmpty()) {
                    System.out.println("(no spaces)");
                } else {
                    List<String[]> rows = new ArrayList<>();
                    rows.add(new String[]{"ID", "Name", "Owner", "Created"});
                    for (SpaceResponse s : spaces) {
                        rows.add(new String[]{
                                String.valueOf(s.getId()),
                                nullSafe(s.getName()),
                                String.valueOf(s.getOwnerId()),
                                s.getCreatedAt() == null ? "" : s.getCreatedAt().toString()
                        });
                    }
                    printer.printTable(rows);
                }
                return 0;
            } catch (com.agentcache.cli.CliException ex) {
                printer.printError(ex.getMessage());
                return 1;
            }
        }
    }

    static Config requireAuth(Printer printer) {
        try {
            Config config = Config.load();
            config.validate();
            if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
                printer.printError("Not logged in. Run 'agentcache login <apiKey>' first.");
                return null;
            }
            return config;
        } catch (Exception ex) {
            printer.printError(ex.getMessage());
            return null;
        }
    }

    static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}