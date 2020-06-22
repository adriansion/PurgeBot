import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Attempting to enter application...");

        String token = "token";
        DiscordApi bot = new DiscordApiBuilder().setToken(token).login().join();

        logger.info("Entered application successfully.");

        bot.addMessageCreateListener(new CommandListener());
    }

}
