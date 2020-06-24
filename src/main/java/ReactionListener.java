import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
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

    String confirmationNumber, sender;

    private ReactionListener() {
    }

    private static ReactionListener instance = new ReactionListener();

    public static ReactionListener getInstance() {
        return instance;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {

        ServerTextChannel channel = event.getServerTextChannel().get();
        Message message = event.getMessage().get();
        Confirmation confirmation = Confirmation.getInstance();
        confirmationNumber = confirmation.getConfirmationNumber();
        String reactor = event.getUser().getDiscriminatedName();
        Emoji reaction = event.getReaction().get().getEmoji();

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

        if (message.getAuthor().getDiscriminatedName().equals("Purge#0337")) {
            // Confirmation message sent by Purge bot

            if (message.getContent().endsWith(confirmationNumber + "*")) {
                // Confirmation message contains unique confirmation number

                if (reactor.equals("Purge#0337") && reaction.equalsEmoji("❎")) {
                    logger.info("Allowing sender 15 seconds to confirm deletion process in " + channel.getName() + ".");
                    timer.schedule(expiry, 15000);
                }

                if (reactor.equals(sender)) {
                    // Emote sent by command sender

                    if (reaction.equalsEmoji("✅")) {
                        // Confirmation is affirmative
                        logger.info("Verification complete. Beginning deletion process...");

                        confirmation.confirmFromReactionListener(event, sender);

                        message.delete();
                        logger.info("Informed confirmation of verification.");

                    } else if (reaction.equalsEmoji("❎")) {
                        // Confirmation is negative
                        message.delete();
                        logger.info("Sender rejected confirmation. Deletion process aborted.");
                    }
                }
            }
        }
    }
}
