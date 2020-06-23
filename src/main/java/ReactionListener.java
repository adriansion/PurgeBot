import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for emotes sent to a specified message.
 * <p>
 * Particularly useful for Confirmation.java
 *
 * @author Adrian
 */
public class ReactionListener implements ReactionAddListener {

    private static final Logger logger = LogManager.getLogger("Confirmation");


    @Override
    public void onReactionAdd(ReactionAddEvent event) {

        ServerTextChannel channel = event.getServerTextChannel().get();
        Confirmation confirmation = Confirmation.getInstance();

        Timer timer = new Timer();
        TimerTask expiry = new TimerTask() {
            @Override
            public void run() {
                CompletableFuture<Void> messageExpiryDeletion = event.getMessage().get().delete();
                messageExpiryDeletion.thenAccept((del) -> {
                    logger.info("Automatically deleted confirmation message in " + channel.getName() + ".");
                });
            }
        };
    }
}
