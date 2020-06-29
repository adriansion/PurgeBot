import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Purger {

    private static final Logger logger = LogManager.getLogger("Purger");

    /**
     * Called to begin deletion from Verifier.java.
     */
    protected void verifiedDeletion(String user, List<ServerTextChannel> channels) {

        // Commits deletion on specified text channel(s).
        for (ServerTextChannel c : channels) {
//            CompletableFuture<Void> purge = this.channelPurge(c, user);
            this.channelPurge(c, user);

            // Logs deletion completion.
//            purge.thenAccept((del) -> logger.info("Deletion successful in " + c.getName() + "."));
        }
    }

    /**
     * Assorts user messages into ArrayLists and deletes from each individual list sequentially.
     *
     * @param c    ServerTextChannel
     * @param user Discriminated username of user being purged
     * @return CompletableFuture upon completed deletion
     */
    public void channelPurge(ServerTextChannel c, String user) {

        // The instant at which user joined server.
        Instant instant = c.getServer().getMemberByDiscriminatedName(user).get().getJoinedAtTimestamp(c.getServer()).get();

        MessageSet allMessages;
        ArrayList<Message> userMessages = new ArrayList<>();
        Stack<ArrayList<Message>> deletionBatches = new Stack<>();

        try {

            // Collects all messages sent to channel since user joined server.
            logger.info("Collecting channel messages... [Channel: " + c.getName() + "]");
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(instant) > 0).get();
            logger.info("Found " + allMessages.size() + " channel messages since the user joined. [Channel: " + c.getName() + "]");


            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {
                    userMessages.add(m);
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        logger.info(userMessages.size() + " messages will be deleted. [Channel: " + c.getName() + "]");


        // Distributes user messages to deletion batch arrays.
        deletionBatches.push(new ArrayList<>());
        for (Message m : userMessages) {
            if (deletionBatches.peek().size() >= 100) {
                deletionBatches.push(new ArrayList<>());
            }
            deletionBatches.peek().add(m);
        }

        AtomicBoolean allDeletionsSuccessful = new AtomicBoolean(true);
        HashMap<CompletableFuture<Void>, Integer> fm = new HashMap<>();
        Runnable task = () -> {
            for (ArrayList<Message> a : deletionBatches) {
                fm.put(c.deleteMessages(a), a.size());
            }

            fm.forEach((f, i) -> {
                try {
                    logger.info("Deleting batch of " + i + " message(s)...");
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (!f.isDone()) {
                    allDeletionsSuccessful.set(false);
                }
            });
            if (allDeletionsSuccessful.get()) {
                logger.info("All deletion batches completely successful in " + c.getName() + ".");

            } else {
                logger.info("Deletion concluded in " + c.getName() + ", but not all deletion batches were successful.");
            }
        };

        // Only one thread is necessary, since the process' speed will ultimately be limited by Discord rate limits.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(task);
        executor.shutdown();

    }
}
