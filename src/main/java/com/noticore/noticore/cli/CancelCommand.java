package com.noticore.noticore.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "cancel", description = "Cancel a PENDING notification by ID")
public class CancelCommand implements Callable<Integer> {

    // @Parameters (vs @Option) is for a positional argument, e.g.
    // `noticore cancel <id>` rather than needing a --id flag.
    @Parameters(index = "0", description = "The notification ID to cancel")
    String id;

    @Override
    public Integer call() throws Exception {
        return HttpHelper.patch(HttpHelper.BASE_URL + "/" + id + "/cancel");
    }
}
