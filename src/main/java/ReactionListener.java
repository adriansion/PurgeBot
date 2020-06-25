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

    private static final Logger logger = LogManager.getLogger("RctL");

    String sender, reactor;
    Confirmation confirmation = Confirmation.getInstance();

    private static ReactionListener instance = new ReactionListener();

    private ReactionListener() {
    }

    public static ReactionListener getInstance() {
        return instance;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public void onReactionAdd(ReactionAddEvent event) {

        Emoji reaction = event.getReaction().get().getEmoji();
        Message message = event.getMessage().get();
        reactor = event.getUser().getDiscriminatedName();

        if (message.getAuthor().getDiscriminatedName().equals("Purge#0337")) {
            // Confirmation message sent by Purge bot

            if (message.getContent().endsWith(confirmation.getConfirmationNumber() + "*")) {
                // Confirmation message contains unique confirmation number

                if (reaction.equalsEmoji("✅")) { // Reaction is affirmative

                    if (reactor.equals(sender)) {
                        // Reaction added by sender

                        logger.info("Verification complete. Beginning deletion process...");

                        confirmation.confirmFromReactionListener(event, sender);
                        message.delete();

                        logger.info("Informed confirmation of verification.");
                    }
                } else if (reaction.equalsEmoji("❎")) { // Reaction is negative

                    if (reactor.equals(sender)) {
                        // Reaction added by sender

                        message.delete();
                        logger.info("Sender rejected confirmation. Deletion process aborted.");

                    } else if (reactor.equals("Purge0337")) {
                        // Reaction added by bot

                        Timer timer = new Timer();
                        TimerTask expiry = new TimerTask() {
                            @Override
                            public void run() {
                                CompletableFuture<Void> messageExpiryDeletion = event.getMessage().get().delete();
                                messageExpiryDeletion.thenAccept((del) -> {
                                    logger.info("Automatically deleted confirmation message.");
                                });
                            }
                        };

                        logger.info("Allowing sender 15 seconds to confirm deletion process.");
                        timer.schedule(expiry, 15000);
                    }
                }
            }
        }
    }
}
