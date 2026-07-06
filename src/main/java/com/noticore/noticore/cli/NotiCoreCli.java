package com.noticore.noticore.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

// The top-level command. subcommands = {...} is what makes
// `noticore send ...`, `noticore cancel ...`, `noticore replay ...` all
// work as distinct sub-tools under one entry point -- similar to how `git`
// has `git commit`, `git push`, etc. all under one `git` executable.
@Command(
        name = "noticore",
        description = "NotiCore CLI -- send, cancel, and replay notifications",
        subcommands = { SendCommand.class, CancelCommand.class, ReplayCommand.class }
)
public class NotiCoreCli implements Runnable {

    public static void main(String[] args) {
        // CommandLine.execute() parses args, finds the matching subcommand,
        // runs it, and returns an exit code (0 = success, non-zero = error)
        // -- System.exit() propagates that so shell scripts calling this CLI
        // can check success/failure the normal way.
        int exitCode = new CommandLine(new NotiCoreCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        // Runs only if the CLI is called with no subcommand at all --
        // just show usage help instead of doing nothing silently.
        new CommandLine(this).usage(System.out);
    }
}
