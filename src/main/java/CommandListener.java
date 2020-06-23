import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Listens for messages sent to any text channel in the server, and acts when
 * the purge command is identified.
 * Command usage: "!purge <Discriminated Name> [all]"
 * Optional: [all] - Purges messages across all server channels. Restricted to
 * working channel if omitted.
 * For example, "!purge shiggydiggy#8455 all"
 *
 * @author Adrian
 */
public class CommandListener implements MessageCreateListener {

    private static final Logger logger = LogManager.getLogger("Command");

    public CommandListener() {
    }

    public void onMessageCreate(MessageCreateEvent event) {
        if (event.getMessageContent().startsWith("!purge")
                && event.getMessage().getAuthor().getDiscriminatedName().equals("shiggydiggy#8455")) {

            String[] args = event.getMessageContent().split(" ");
            String user = args[1];
            String commandSender = event.getMessageAuthor().getDiscriminatedName();
            Purger purger = new Purger();

            // Logs purge command usage.
            logger.info("Sender: " + commandSender + " User: " + user +
                    " Channel: " + event.getServerTextChannel().get().getName());

            // The instant at which the user being purged joined the server.
            Instant i = (event.getServer().get().getMemberByDiscriminatedName(user).get()
                    .getJoinedAtTimestamp(event.getServer().get()).get());

            // Removes original "!purge" command from channel.
            event.getMessage().delete();

            List<ServerTextChannel> channelsToPurge = new ArrayList<>();

            // Includes appropriate channels for message deletion.
            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("all")) {
                    channelsToPurge = event.getServer().get().getTextChannels();
                    logger.info("Purge command invoked across all channels.");
                }
            } else {
                channelsToPurge.add(event.getServerTextChannel().get());
            }

            // Requests confirmation from command sender for message purge
            Confirmation emoteConfirmation = new Confirmation(event.getServerTextChannel().get());
            emoteConfirmation.poseConfirmation(event.getServerTextChannel().get(), commandSender, user, i, channelsToPurge);


        } else if (event.getMessageContent().startsWith("!fill")) {
            Thread thread = new Thread();
            for (int i = 0; i < 25; i++) {
//                for (int j = 0; j < 5; j++) {
                try {

                    thread.sleep(750);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                event.getChannel().sendMessage(Integer.toString(i));
//                }
            }
        }
    }
}
