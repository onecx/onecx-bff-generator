package org.tkit.onecx.onecxbffgen;

import org.junit.jupiter.api.Test;
import org.tkit.onecx.onecxbffgen.commands.CreateBffCommand;
import org.tkit.onecx.onecxbffgen.service.GeneratorService;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainCommandTest {

    @Test
    void shouldRegisterCreateBffSubcommand() {
        CommandLine cli = buildCli();

        assertTrue(cli.getSubcommands().containsKey("create-bff"));
    }

    @Test
    void shouldExecuteRootAndCreateBffHelpFlows() {
        CommandLine cli = buildCli();

        assertEquals(0, cli.execute("--help"));
        assertEquals(0, cli.execute("create-bff", "--help"));
        assertEquals(0, cli.execute());
    }

    private CommandLine buildCli() {
        CommandLine cli = new CommandLine(new Main.RootCommand());
        cli.addSubcommand("create-bff", new CreateBffCommand(new GeneratorService()));
        return cli;
    }
}
