package com.noticore.noticore.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "send", description = "Send a new notification")
public class SendCommand implements Callable<Integer> {

    // Picocli maps these @Option fields to command-line flags automatically,
    // e.g. --user user123 --channel EMAIL --to test@example.com --message hi
    @Option(names = "--user", required = true, description = "User ID to notify")
    String userId;

    @Option(names = "--channel", required = true, description = "EMAIL, SMS, or PUSH")
    String channel;

    @Option(names = "--to", required = true, description = "Recipient address/phone/token")
    String recipient;

    @Option(names = "--message", required = true, description = "Notification message text")
    String message;

    @Override
    public Integer call() throws Exception {
        // Hand-building this small JSON string keeps the CLI dependency-free
        // (no extra JSON library needed just for 4 fields). A basic escape
        // of double quotes in the message avoids breaking the JSON if the
        // user's message happens to contain a " character.
        String json = String.format(
                "{\"userId\":\"%s\",\"channel\":\"%s\",\"recipient\":\"%s\",\"message\":\"%s\"}",
                userId, channel, recipient, escape(message));
        return HttpHelper.postJson(HttpHelper.BASE_URL, json);
    }

    private String escape(String text) {
        return text.replace("\"", "\\\"");
    }
}
