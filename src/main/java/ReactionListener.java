import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

/**
 * Listens for reactions added to deletion confirmation messages.
 *
 * @author Adrian
 */
public class ReactionListener implements ReactionAddListener {

    private static final Logger logger = LogManager.getLogger("ReactionListener");

    Verifier verifier = Verifier.getInstance();
    String sender = verifier.getSender(), reactor;

    @Override
    public void onReactionAdd(ReactionAddEvent event) {

        reactor = event.getUser().getDiscriminatedName();
        Message message = event.getMessage().get();
        Emoji reaction = event.getReaction().get().getEmoji();

        if (reactor.equals(sender)) {
            // Reaction added by sender

            if (message.getContent().endsWith(verifier.getConfirmationNumber() + "*")) {
                // Message contains unique confirmation number

                if (reaction.equalsEmoji("✅")) {
                    // Reaction is affirmative

                    logger.info("Verification complete. Informing confirmation of verification...");

                    verifier.confirmFromReactionListener(event, sender);
                    message.delete();

                } else if (reaction.equalsEmoji("❎")) {
                    // Reaction is negative

                    logger.info("Sender rejected confirmation. Deletion will not occur.");
                    message.delete();

                }
            }
        }
    }
}
