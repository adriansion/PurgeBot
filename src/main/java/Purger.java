import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Purger {

    private final static Logger logger = LogManager.getLogger("Purger");

    public Purger() {
    }

    public void preliminaryPurgeSequence(String user, Instant i, List<ServerTextChannel> channelsToPurge) {

        // Commits message purge on specified text channel(s).
        for (ServerTextChannel c : channelsToPurge) {
            CompletableFuture<Void> purge = this.channelPurgeB(c, user, i);

            // Logs deletion completion.
            purge.thenAccept((del) -> logger.info("Process initialization successful in " + c.getName() + "."));
        }
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
            logger.info("Analyzing " + allMessages.size() + " messages in " + c.getName() + ".");

            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {
                    userMessages.add(m);
                    userMessageCount++;
                }
            }

            logger.info("Analyzing " + userMessageCount + " messages in " + c.getName() + ".");

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
        ArrayList<Message> ym = new ArrayList<>(), om = new ArrayList<>();
        Stack<ArrayList<Message>> messageArrays = new Stack<>(), ya = new Stack<>(), oa = new Stack<>();
        messageArrays.push(new ArrayList<>());
        ya.push(new ArrayList<>());
        oa.push(new ArrayList<>());
        int userMessageCount = 0, MAXARRAYSIZE = 10;
        boolean added;

        try {

            // Collects all messages sent to the channel since user joined server.
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(i) > 0).get();
            logger.info("Analyzing " + allMessages.size() + " messages in " + c.getName() + ".");

            Instant twoWeeksPrior = Instant.now().minus(27, ChronoUnit.HALF_DAYS);

            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {

                    added = m.getCreationTimestamp().isAfter(twoWeeksPrior) ? ym.add(m) : om.add(m);
                    userMessageCount++;
                }
            }
        } catch (InterruptedException |
                ExecutionException e) {
            e.printStackTrace();
        }

        logger.info("Analyzing " + userMessageCount + " messages in " + c.getName() + ".");

        // Distributes young messages to young message arrays.
        for (Message m : ym) {
            if (ya.peek().size() < 100) {
                ya.peek().add(m);
            } else {
                ya.push(new ArrayList<>());
                ya.peek().add(m);
            }
        }

        // Distributes old messages to old message arrays.
        for (Message m : om) {
            if (oa.peek().size() < MAXARRAYSIZE) {
                oa.peek().add(m);
            } else {
                oa.push(new ArrayList<>());
                oa.peek().add(m);
            }
        }


        // Deletes applicable messages from channel.
        return CompletableFuture.runAsync(() ->

        {
            // Deletes young messages
            for (ArrayList<Message> a : ya) {
                c.bulkDelete(a).thenAccept((del) -> logger.info("Deletion count: " + a.size() + " (bulk)"));
            }

            // Deletes old messages
            for (ArrayList<Message> a : oa) {
                c.deleteMessages(a).thenAccept((del) -> logger.info("Deletion count: " + a.size()));
            }
        });
    }
}
