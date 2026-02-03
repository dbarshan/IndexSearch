package com.indexsearch.client.cli;

import com.indexsearch.client.IndexSearchClientApplication;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CliRunner implements CommandLineRunner {
    private final CommandFunction commandFunction;

    @Override
    public void run(String... args) throws Exception {
        printBanner();
        if (args.length == 0) {
            runCommandLoop();
            return;
        }
        runCommand(args[0]);
    }

    private void runCommandLoop() throws Exception {
        Terminal terminal = buildTerminal();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        while (true) {
            String input;
            try {
                input = reader.readLine("indexsearch> ");
            } catch (UserInterruptException e) {
                continue;
            } catch (EndOfFileException e) {
                break;
            }
            if (input == null) {
                continue;
            }
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            runCommand(input);
        }
    }

    private void runCommand(String input) {
        String[] args = input.split(" ");
        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "create-collection" -> commandFunction.createCollection(args);
                case "list-collections" -> commandFunction.listCollections(args);
                case "delete-collection" -> commandFunction.deleteCollection(args);
                case "add-doc" -> commandFunction.addDocument(args);
                case "update-doc" -> commandFunction.updateDocument(args);
                case "get-doc" -> commandFunction.getDocument(args);
                case "delete-doc" -> commandFunction.deleteDocument(args);
                case "search" -> commandFunction.search(args);
                case "sql" -> commandFunction.searchSql(args);
                case "rebuild-index" -> commandFunction.rebuildIndex(args);
                case "help" -> commandFunction.printUsage();
                default -> {
                    System.out.println("Unknown command: " + command);
                    commandFunction.printUsage();
                }
            }
        } catch (Exception e) {
            System.out.println("Error running command: " + e.getMessage());
        }
    }

    private void printBanner() {
        String version = getVersion();
        System.out.println("      IndexSearch v" + version);
        System.out.println("-------------------------------------------------");
    }

    private Terminal buildTerminal() throws IOException {
        return TerminalBuilder.builder()
                .system(true)
                .exec(false)
                .build();
    }

    private String getVersion() {
        String version = IndexSearchClientApplication.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "dev" : version;
    }
}
