package org.tkit.onecx.onecxbffgen;

import org.tkit.onecx.onecxbffgen.commands.CreateBffCommand;
import org.tkit.onecx.onecxbffgen.service.GeneratorService;
import picocli.CommandLine;

import java.util.concurrent.Callable;

public class Main {

    @CommandLine.Command(
            name = "onecx-bff-generator",
            mixinStandardHelpOptions = true,
            description = "OneCX BFF generator CLI",
            subcommands = { CommandLine.HelpCommand.class }
    )
    static class RootCommand implements Callable<Integer> {
        @Override
        public Integer call() {
            CommandLine.usage(this, System.out);
            return 0;
        }
    }

    public static void main(String[] args) {
        CommandLine cli = new CommandLine(new RootCommand());
        cli.addSubcommand("create-bff", new CreateBffCommand(new GeneratorService()));
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}