import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for deletion confirmation message.
 *
 * @author Adrian
 */
public class ConfirmationListener implements MessageCreateListener {

    private static final Logger logger = LogManager.getLogger("CfML");

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageContent().endsWith(Verifier.getInstance().getConfirmationNumber() + "*")) {
            event.getMessage().addReaction("✅");
            event.getMessage().addReaction("❎");
        }

        Timer timer = new Timer();
        TimerTask expiry = new TimerTask() {
            @Override
            public void run() {
                CompletableFuture<Void> messageExpiryDeletion = event.getMessage().delete();
                messageExpiryDeletion.thenAccept((del) -> logger.info("Automatically deleted confirmation message."));
            }
        };

        logger.info("Allowing sender 15 seconds to confirm deletion process.");
        timer.schedule(expiry, 15000);
    }
}
