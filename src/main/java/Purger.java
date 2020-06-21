import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Purger {

    private final static Logger logger = LogManager.getLogger(Purger.class);

    public Purger() {
    }

    /**
     * Version A of channelPurge method: uses standard deleteMessages method with
     * populated ArrayList of messages.
     *
     * @param c    ServerTextChannel
     * @param user Discriminated username of user being purged
     * @param i    Instant user being purged joined server
     * @return CompletableFuture upon completed deletion
     */
    public CompletableFuture<Void> channelPurgeA(ServerTextChannel c, String user, Instant i) {

        MessageSet allMessages;
        List<Message> userMessages = new ArrayList<Message>();
        int userMessageCount = 0;

        try {

            // Collects all messages sent to the channel since user joined server.
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(i) > 0).get();
            logger.info(allMessages.size() + " messages found since user join instant in " + c.getName() + ".");

            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {
                    userMessages.add(m);
                    userMessageCount++;
                }
            }

            logger.info(userMessageCount + " messages found from user since user join instant in " + c.getName() + ".");

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Deletes applicable messages from channel.
        // Use thenRunASync for numerous deletion processes
        CompletableFuture<Void> completedDeletion = c.deleteMessages(userMessages);

        return completedDeletion;

    }

    /**
     * Version B of channelPurge method: assorts user messages into specified
     * ArrayLists and deletes from each individual list sequentially.
     *
     * @param c    ServerTextChannel
     * @param user Discriminated username of user being purged
     * @param i    Instant user being purged joined server
     * @return CompletableFuture upon completed deletion
     */
    public CompletableFuture<Void> channelPurgeB(ServerTextChannel c, String user, Instant i) {

        MessageSet allMessages;
        Stack<ArrayList<Message>> messageArrays = new Stack<ArrayList<Message>>();
        messageArrays.push(new ArrayList<Message>());
        int userMessageCount = 0, counter = 0, MAXARRAYSIZE = 10;

        try {

            // Collects all messages sent to the channel since user joined server.
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(i) > 0).get();
            logger.info(allMessages.size() + " messages found since user join instant in " + c.getName() + ".");

            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {
                    if (counter < MAXARRAYSIZE) {
                        messageArrays.peek().add(m);
                        counter++;
                    } else {

                        messageArrays.push(new ArrayList<Message>());
                        messageArrays.peek().add(m);

                        counter = 1;
                    }
                    userMessageCount++;
                }
            }

            logger.info(
                    userMessageCount + " messages found from user since user join instant in " + c.getName() + ".");

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();

        }

        // Deletes applicable messages from channel.
        return CompletableFuture.runAsync(() -> {
            for (ArrayList<Message> a : messageArrays) {
                c.deleteMessages(a).thenAccept((del) -> logger.info(a.size() + " Messages deleted"));
            }
        });
    }
}
