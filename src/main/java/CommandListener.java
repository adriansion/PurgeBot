import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Listens for messages sent to any text channel in the server, and acts when
 * the purge command is identified.
 * <p>
 * Command usage: "!purge <Discriminated Name> [all]"
 * <p>
 * Optional: [all] - Purges messages across all server channels. Restricted to
 * working channel if omitted.
 * <p>
 * For example, "!purge shiggydiggy#8455 all"
 *
 * @author Adrian
 */
public class CommandListener implements MessageCreateListener {

    private static final Logger logger = LogManager.getLogger(CommandListener.class);

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
            logger.info(commandSender + " invoked \"!purge\" on " + user + " in " + event.getServer().get().getName());

            // The instant at which the user being purged joined the server.
            Instant i = (event.getServer().get().getMemberByDiscriminatedName(user).get()
                    .getJoinedAtTimestamp(event.getServer().get()).get());

            // Removes original "!purge" command from channel.
            event.getMessage().delete();

            List<ServerTextChannel> channelsToPurge = new ArrayList<ServerTextChannel>();

            // Includes appropriate channels for message deletion.
            if (args.length == 3) {
                if (args[2].equalsIgnoreCase("all")) {
                    channelsToPurge = event.getServer().get().getTextChannels();
                    logger.info("Purge command to be invoked across all channels.");
                }
            } else {
                channelsToPurge.add(event.getServerTextChannel().get());
            }

            // Commits message purge on specified text channel(s).
            for (ServerTextChannel c : channelsToPurge) {
//				CompletableFuture<Void> purge = this.channelPurgeB(c, user, i);
                CompletableFuture<Void> purge = purger.channelPurgeB(c, user, i);

                // Logs deletion completion.
                purge.thenAccept((del) -> logger.info(user + " successfully purged in " + c.getName() + "."));
            }
        }
    }
}
