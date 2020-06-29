import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

import java.util.*;

public class Verifier {

    private static final Verifier instance = new Verifier();

    private static final Logger logger = LogManager.getLogger("Verifier");

    private String sender, user, confirmationNumber;
    private List<ServerTextChannel> channels;
    private ServerTextChannel channel;

    private Verifier() {
    }

    public static Verifier getInstance() {
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

    public void poseConfirmation(ServerTextChannel channel, String sender, String user, List<ServerTextChannel> channelsToPurge) {

        logger.info("Initializing confirmation message in " + this.channel.getName() + ".");

        this.sender = sender;
        this.user = user;
        this.channels = channelsToPurge;
        this.channel = channel;

        confirmationNumber = this.createConfirmationNumber();

        this.channel.addReactionAddListener(new ReactionListener());
        this.channel.addMessageCreateListener(new ConfirmationListener());

        this.channel.sendMessage("Purge command invoked by **" + this.sender
                + "**.\n\nAre you sure that you want to **purge messages** sent from **"
                + user + "**?\n\n**Note**: this confirmation message will automatically"
                + " delete in **15** seconds.\n\n*Confirmation number: " + confirmationNumber + "*");

        logger.info("Sent confirmation message in " + this.channel.getName() + ".");
    }

    public void confirmFromReactionListener(ReactionAddEvent event, String sender) {
        if (sender.equals(this.sender)) {
            logger.info("Verification received from reaction listener. Starting deletion...");

            Purger purger = new Purger();
//            purger.verifiedDeletion(user, channels);
        }

    }

    public String getSender() {
        return this.sender;
    }
}