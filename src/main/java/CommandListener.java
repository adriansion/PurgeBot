import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Listens for purge command sent to any text channel in the server.
 * <p>
 * Command usage: "!purge <Discriminated Name> [all]"
 * Optional: [all] - Purges messages across all server channels. Restricted to
 * working channel if omitted.
 * <p>
 * For example, "!purge shiggydiggy#8455 all"
 *
 * @author Adrian
 */
public class CommandListener implements MessageCreateListener {

    private static final Logger logger = LogManager.getLogger("CmdL");

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

        if (event.getMessage().getAuthor().canManageServer()) {
            // Authorized command sender

            if (event.getMessageContent().startsWith("!")) {
                // Command being sent

                String sender = event.getMessageAuthor().getDiscriminatedName();
                String command = event.getMessageContent();
                String[] args = command.split(" ");
                ServerTextChannel channel = event.getServerTextChannel().get();
                Server server = event.getServer().get();

                if (command.startsWith("!purge")) {
                    // Purge command

                    logger.info("[Sender: " + sender + "] [Channel: " + channel.getName() + "] [Server: " + server.getName() + "]");

                    String user = args[1];

                    // The instant at which user joined server.
                    Instant instant = (server.getMemberByDiscriminatedName(user).get().getJoinedAtTimestamp(server).get());

                    // Removes command from channel.
                    event.getMessage().delete();

                    List<ServerTextChannel> channelsToPurge = new ArrayList<>();

                    // Includes appropriate channels for user deletion.
                    if (args.length == 3) {
                        if (args[2].equalsIgnoreCase("all")) {
                            channelsToPurge = server.getTextChannels();
                            logger.info("Purge command invoked across all channels.");
                        }
                    } else {
                        channelsToPurge.add(channel);
                    }

                    // Requests confirmation from sender
                    Confirmation confirmation = Confirmation.getInstance();
                    confirmation.setChannel(channel);
                    confirmation.poseConfirmation(channel, sender, user, instant, channelsToPurge);

                }
            }
        }
    }
}
