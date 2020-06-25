import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Confirmation {

    private static Confirmation instance = new Confirmation();

    private static final Logger logger = LogManager.getLogger("Conf");

    private String sender, user, confirmationNumber;
    private Instant i;
    private List<ServerTextChannel> channelsToPurge;
    private ServerTextChannel channel;

    private Confirmation() {
    }

    public static Confirmation getInstance() {
        return instance;
    }

    public void setChannel(ServerTextChannel channel) {
        this.channel = channel;
    }

    private String createConfirmationNumber() {
        Random random = new Random();
        String number = (new UUID(random.nextInt(2000000000), random.nextInt(2000000000)).toString());
        return number;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void poseConfirmation(ServerTextChannel channel, String sender, String user, Instant i, List<ServerTextChannel> channelsToPurge) {

        logger.info("Initializing confirmation message in " + this.channel.getName() + ".");

        this.sender = sender;
        this.user = user;
        this.i = i;
        this.channelsToPurge = channelsToPurge;
        this.channel = channel;

        confirmationNumber = this.createConfirmationNumber();

        this.channel.addReactionAddListener(new ReactionListener());
        this.channel.addMessageCreateListener(new confirmationMessageListener());

        this.channel.sendMessage("Purge command invoked by **" + this.sender
                + "**.\n\nAre you sure that you want to **purge messages** sent from **"
                + user + "**?\n\n**Note**: this confirmation message will automatically"
                + " delete in **15** seconds.\n\n*Confirmation number: " + confirmationNumber + "*");

        logger.info("Sent confirmation message in " + this.channel.getName() + ".");
    }

    private static class confirmationMessageListener implements MessageCreateListener {

        @Override
        public void onMessageCreate(MessageCreateEvent event) {
            if (event.getMessageAuthor().getDiscriminatedName().equals("Purge#0337")) {
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

    public void confirmFromReactionListener(ReactionAddEvent event, String sender) {
        if (sender.equals(this.sender)) {
            logger.info("Verification received from reaction listener. Starting deletion...");

            Purger purger = new Purger();
            purger.preliminaryPurgeSequence(user, i, channelsToPurge);
        }

    }

    public String getSender() {
        return this.sender;
    }
}