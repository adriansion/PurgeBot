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

    private static final Logger logger = LogManager.getLogger("Purger");

    /**
     * Called to begin deletion from Verifier.java.
     */
    protected void verifiedDeletion(String user, List<ServerTextChannel> channels) {

        // Commits deletion on specified text channel(s).
        for (ServerTextChannel c : channels) {
            CompletableFuture<Void> purge = this.channelPurge(c, user);

            // Logs deletion completion.
            purge.thenAccept((del) -> logger.info("Process initialization successful in " + c.getName() + "."));
        }
    }

    /**
     * Assorts user messages into ArrayLists and deletes from each individual list sequentially.
     *
     * @param c    ServerTextChannel
     * @param user Discriminated username of user being purged
     * @return CompletableFuture upon completed deletion
     */
    public CompletableFuture<Void> channelPurge(ServerTextChannel c, String user) {

        // The instant at which user joined server.
        Instant instant = c.getServer().getMemberByDiscriminatedName(user).get().getJoinedAtTimestamp(c.getServer()).get();
        MessageSet allMessages;
        ArrayList<Message> ym = new ArrayList<>(), om = new ArrayList<>();
        Stack<ArrayList<Message>> messageArrays = new Stack<>(), ya = new Stack<>(), oa = new Stack<>();
        messageArrays.push(new ArrayList<>());
        ya.push(new ArrayList<>());
        oa.push(new ArrayList<>());
        int userMessageCount = 0;
        final int maxArraySize = 10;
        boolean added;

        try {

            // Collects all messages sent to channel since user joined server.
            logger.info("Collecting channel messages. [Channel: " + c.getName() + "].");
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(instant) > 0).get();
            logger.info("Found " + allMessages.size() + " channel messages. [Channel: " + c.getName() + "]");

            Instant twoWeeksPrior = Instant.now().minus(27, ChronoUnit.HALF_DAYS);

            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {

                    added = m.getCreationTimestamp().isAfter(twoWeeksPrior) ? ym.add(m) : om.add(m);
                    userMessageCount++;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
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
            if (oa.peek().size() < maxArraySize) {
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
