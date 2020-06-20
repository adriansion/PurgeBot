import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Main {
    public static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");

        ConsoleAppender ConsoleAppender = new ConsoleAppender(new PatternLayout());
        ConsoleAppender.setName("ConsoleAppender");

        logger.addAppender(ConsoleAppender);
        logger.setAdditivity(false);
        logger.info("Attempting to enter application...");

        String token = "NzIxMTU3MzgyMzM5NjkwNTg2.Xu1uTg.eYwEyXs2LdxJGH2YXJlfV4gUlUw";
        DiscordApi bot = new DiscordApiBuilder().setToken(token).login().join();

        logger.info("Entered application successfully.");

        bot.addMessageCreateListener(new CommandListener());
    }

}
