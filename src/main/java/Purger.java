import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.javacord.api.exception.UnknownMessageException;
import org.javacord.api.util.ratelimit.Ratelimiter;
import org.javacord.core.util.ratelimit.RatelimitBucket;
import org.javacord.core.util.ratelimit.RatelimitManager;
import org.javacord.core.util.rest.RestEndpoint;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Purger {

    private static final Logger logger = LogManager.getLogger("Purger");
    private DiscordApi api;

    /**
     * Called to begin deletion from Verifier.java.
     */
    protected void verifiedDeletion(String user, List<ServerTextChannel> channels, DiscordApi api) {
        this.api = api;
        // Commits deletion on specified text channel(s).
        for (ServerTextChannel c : channels) {
            this.channelPurge(c, user);
        }
    }

    /**
     * Assorts user messages into ArrayLists and deletes from each individual list sequentially.
     *
     * @param c    ServerTextChannel
     * @param user Discriminated username of user being purged
     */
    public void channelPurge(ServerTextChannel c, String user) {

        // The instant at which user joined server.
        Instant instant = c.getServer().getMemberByDiscriminatedName(user).get().getJoinedAtTimestamp(c.getServer()).get();

        MessageSet allMessages;
        ArrayList<Message> userMessages = new ArrayList<>();
        Stack<ArrayList<Message>> deletionBatches = new Stack<>();

        AtomicInteger userMessageCount = new AtomicInteger(0);
        AtomicInteger deletedMessageCount = new AtomicInteger(-1);
        AtomicInteger deletionPercentFinished = new AtomicInteger(0);
        AtomicInteger deletionProgressLogsSent = new AtomicInteger(0);
        AtomicIntegerArray loggerProgressNotifyThresholdsAtomic;
        AtomicBoolean allDeletionsSuccessful;
        boolean logCompletionPercentagePerBatch;
        RatelimitBucket ratelimitBucket = new RatelimitBucket(this.api, RestEndpoint.MESSAGE_DELETE);


        try {

            // Collects all messages sent to channel since user joined server.
            logger.info("Collecting channel messages... [Channel: " + c.getName() + "]");
            allMessages = c.getMessagesWhile(m -> m.getCreationTimestamp().compareTo(instant) > 0).get();
            logger.info("Found " + allMessages.size() + " channel messages since the user joined. [Channel: " + c.getName() + "]");


            // Examines each message and keeps those whose author is the user.
            for (Message m : allMessages) {
                if (m.getAuthor().getDiscriminatedName().equals(user)) {
                    userMessages.add(m);
                    userMessageCount.incrementAndGet();
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        logger.info(userMessageCount.toString() + " messages will be deleted. [Channel: " + c.getName() + "]");


        // Distributes user messages to deletion batch arrays.
        int maxArraySize = 100; // 100 for recent message deletion batches
        deletionBatches.push(new ArrayList<>());
        for (Message m : userMessages) {
            if (deletionBatches.peek().size() >= maxArraySize) {
                deletionBatches.push(new ArrayList<>());
            }
            deletionBatches.peek().add(m);
        }

        int maxProgressLogCount = 14;
        logCompletionPercentagePerBatch = (userMessageCount.intValue() <= maxArraySize * maxProgressLogCount);
        int[] loggerProgressNotifyThresholds = new int[maxProgressLogCount];

        if (!logCompletionPercentagePerBatch) {
            int thresholdMultiplier = (int) ((float) userMessageCount.intValue() / (float) maxProgressLogCount);
            for (int i = 0; i < maxProgressLogCount; i++) {
                loggerProgressNotifyThresholds[i] = thresholdMultiplier * (i);
            }
        }

        loggerProgressNotifyThresholdsAtomic = new AtomicIntegerArray(loggerProgressNotifyThresholds);
        allDeletionsSuccessful = new AtomicBoolean(true);
        HashMap<CompletableFuture<Void>, Integer> fm = new HashMap<>();
        Runnable task = () -> {
            for (ArrayList<Message> a : deletionBatches) {
                fm.put(c.deleteMessages(a), a.size());
            }
            fm.forEach((f, i) -> {
                try {
                    deletedMessageCount.set(deletedMessageCount.intValue() + i);

                    // Periodically log percentage completion.


                    if (logCompletionPercentagePerBatch) {
                        deletionPercentFinished.set((int) ((((float) deletedMessageCount.intValue()) / ((float) userMessageCount.intValue())) * 100));
                        logger.info("Deletion " + deletionPercentFinished.toString() + "% complete. [Channel: " + c.getName() + "]");
                    } else if (deletionProgressLogsSent.intValue() < maxProgressLogCount) {

                        if (deletedMessageCount.intValue() >= (loggerProgressNotifyThresholdsAtomic
                                .get(deletionProgressLogsSent.intValue()))) {
                            deletionPercentFinished.set((int) ((((float) deletedMessageCount.intValue()) / ((float) userMessageCount.intValue())) * 100));
                            deletionProgressLogsSent.incrementAndGet();
//                            deletionPercentFinished.set((int) ((((float) deletedMessageCount.intValue()) / ((float) userMessageCount.intValue())) * 100));
                            logger.info("Deletion " + deletionPercentFinished.toString() + "% complete. [Channel: " + c.getName() + "]");
                        }

                    }

                    f.get();
                    int timeleft = ratelimitBucket.getTimeTillSpaceGetsAvailable();
                    System.out.println(timeleft);
                } catch (InterruptedException | ExecutionException e) {
                    logger.warn("Deletion batch interrupted or completed exceptionally.");

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
