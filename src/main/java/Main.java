import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.CompletionException;

public class Main {

    private static final Logger logger = LogManager.getLogger("Main");

    public static void main(String[] args) {
        logger.info("Attempting to enter application...");

        String token = "token";
        try {
            DiscordApi bot = new DiscordApiBuilder().setToken(token).login().join();

            logger.info("Entered application successfully.");

            bot.addMessageCreateListener(new CommandListener());
        } catch (IllegalStateException | CompletionException e) {
            logger.error("Invalid token. Was it removed for a commit?");
        }
    }

}
