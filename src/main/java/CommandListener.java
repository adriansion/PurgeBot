import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private static final Logger logger = LogManager.getLogger("CommandListener");

    @Override
    public void onMessageCreate(MessageCreateEvent event) {

//        if (event.getMessage().getAuthor().canManageServer()) {
        if (event.getMessageAuthor().getDiscriminatedName().equalsIgnoreCase("shiggydiggy#8455")){
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

                    logger.info("Purge command received. [Sender: " + sender + "] [Channel: " + channel.getName() + "] [Server: " + server.getName() + "]");

                    String user = args[1];

                    // Removes command from channel.
                    event.getMessage().delete();

                    List<ServerTextChannel> channels = new ArrayList<>();

                    // Includes appropriate channels for user deletion.
                    if (args.length == 3) {
                        if (args[2].equalsIgnoreCase("all")) {
                            channels = server.getTextChannels();
                            logger.info("Purge command invoked across all channels.");
                        }
                    } else {
                        channels.add(channel);
                    }

                    // Requests confirmation from sender
//                    Verifier verifier = Verifier.getInstance();
//                    verifier.setChannel(channel);
//                    verifier.poseConfirmation(channel, sender, user, channels);
                    Purger purger = new Purger();
                    purger.verifiedDeletion(user, channels, event.getApi());

                } else if (command.startsWith("!fill")) {
                    // Fill Command
                    Runnable filler = () -> {
                        for (int i = 0; i < 5000; i++) {
                            try {
                                Thread.sleep(1200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            channel.sendMessage(Integer.toString(i));
                        }

                    };
                    ExecutorService executor = Executors.newFixedThreadPool(4);
                    executor.execute(filler);
                    executor.shutdown();
                }

            }
        }
    }
}
