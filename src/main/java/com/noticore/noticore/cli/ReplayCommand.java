package com.noticore.noticore.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "replay", description = "Replay a FAILED notification by ID")
public class ReplayCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "The notification ID to replay")
    String id;

    @Override
    public Integer call() throws Exception {
        return HttpHelper.postNoBody(HttpHelper.BASE_URL + "/" + id + "/replay");
    }
}
