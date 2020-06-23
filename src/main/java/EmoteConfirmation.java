import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EmoteConfirmation {

    private final static Logger logger = LogManager.getLogger(EmoteConfirmation.class);

    private String sender, user, confirmationNumber;
    private Instant i;
    //    private ArrayList<ServerTextChannel> channelsToPurge;
    private List<ServerTextChannel> channelsToPurge;
    private ServerTextChannel channel;

    public EmoteConfirmation(ServerTextChannel channel) {
        this.channel = channel;
    }

    public void poseConfirmation(ServerTextChannel channel, String sender, String user, Instant i, List<ServerTextChannel> channelsToPurge) {

        logger.info("[CONFIRMATION] Initializing confirmation message in " + this.channel.getName() + ".");

        this.sender = sender;
        this.user = user;
        this.i = i;
        this.channelsToPurge = channelsToPurge;
        this.channel = channel;

        Random random = new Random();
        confirmationNumber = (new UUID(random.nextInt(2000000000), random.nextInt(2000000000)).toString());

        this.channel.addReactionAddListener(new confirmationEmoteListener());
        this.channel.addMessageCreateListener(new confirmationMessageListener());

        this.channel.sendMessage("Purge command invoked by **" + this.sender
                + "**.\n\nAre you sure that you want to **purge messages** sent from **"
                + user + "**?\n\n**Note**: this confirmation message will automatically"
                + " delete in **15** seconds.\n\n*Confirmation number: " + confirmationNumber + "*");

        logger.info("[CONFIRMATION] Sent confirmation message in " + this.channel.getName() + ".");
    }

    private static class confirmationMessageListener implements MessageCreateListener {

        @Override
        public void onMessageCreate(MessageCreateEvent event) {
            if (event.getMessageAuthor().getDiscriminatedName().equals("Purge#0337")) {
                event.getMessage().addReaction("✅");
                event.getMessage().addReaction("❎");
            }
        }
    }

    private class confirmationEmoteListener implements ReactionAddListener {

        @Override
        public void onReactionAdd(ReactionAddEvent event) {

            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    CompletableFuture<Void> messageExpiryDeletion = event.getMessage().get().delete();
                    messageExpiryDeletion.thenAccept((del) -> {
                            logger.info("[CONFIRMATION] Automatically deleted confirmation message in " + channel.getName() + ".");
                    });
                }
            };

            if (event.getMessage().get().getAuthor().getDiscriminatedName().equals("Purge#0337")) {
                // Confirmation message sent by Purge bot

                if (event.getMessage().get().getContent().endsWith(confirmationNumber + "*")) {
                    // Confirmation message contains unique confirmation number

                    if (event.getUser().getDiscriminatedName().equals("Purge#0337") && event.getReaction().get().getEmoji().equalsEmoji("❎")) {
                        logger.info("[CONFIRMATION] Allowing sender 15 seconds to confirm deletion process in " + channel.getName() + ".");
                        timer.schedule(task, 15000);
                    }

                    if (event.getUser().getDiscriminatedName().equals(sender)) {
                        // Emote sent by command sender

                        if (event.getReaction().get().getEmoji().equalsEmoji("✅")) {
                            // Confirmation is affirmative
                            logger.info("[CONFIRMATION] Verification complete. Beginning deletion process...");
                            Purger purger = new Purger();
                            purger.preliminaryPurgeSequence(user, i, channelsToPurge);

                            event.getMessage().get().delete();
                            logger.info("[CONFIRMATION] Deletion process complete.");

                        } else if (event.getReaction().get().getEmoji().equalsEmoji("❎")) {
                            // Confirmation is negative
                            event.getMessage().get().delete();
                            logger.info("[CONFIRMATION] Sender rejected confirmation. Deletion process aborted.");
                        }
                    }
                }
            }
        }
    }
}