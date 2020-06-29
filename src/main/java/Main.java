import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.javacord.core.util.ratelimit.RatelimitBucket;
import org.javacord.core.util.rest.RestEndpoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.CompletionException;

public class Main {

    private static final Logger logger = LogManager.getLogger("Main");

    public static void main(String[] args) {
        logger.info("Attempting to enter application...");


        try {
            // Retrieve token from resources
            File tokenFile = new File(Main.class.getClassLoader().getResource("token.txt").getFile());
            String token = (new Scanner(tokenFile).nextLine());
            DiscordApi bot = new DiscordApiBuilder().setToken(token).login().join();

            logger.info("Entered application successfully.");

            bot.addMessageCreateListener(new CommandListener());
            
        } catch (IllegalStateException | CompletionException e) {
            logger.error("Invalid token.");
        } catch (FileNotFoundException e) {
            logger.error("Token file not found.");
        }
    }

}
