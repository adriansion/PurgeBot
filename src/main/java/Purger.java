import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        ArrayList<Message> ym = new ArrayList<>(), om = new ArrayList<>();
        ArrayList<CompletableFuture<Void>> yf = new ArrayList<>(), of = new ArrayList<>();
        Stack<ArrayList<Message>> messageArrays = new Stack<>(), ya = new Stack<>(), oa = new Stack<>();
        messageArrays.push(new ArrayList<>());
        ya.push(new ArrayList<>());
        oa.push(new ArrayList<>());
        int userMessageCount = 0;
        final int maxArraySize = 10;
        boolean added;
        AtomicBoolean allCallableDeletionsCompleted = new AtomicBoolean(true);

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
            if (ya.peek().size() < 3000) {
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

        ArrayList<Boolean> successfulDeletions = new ArrayList<>();
        HashMap<CompletableFuture<Void>, Integer> fm = new HashMap<>();
        Runnable task = () -> {
            for (ArrayList<Message> a : ya) {
                fm.put(c.deleteMessages(a), a.size());
            }

            fm.forEach((f, i) -> {
                try {
                    logger.info("Deleting " + i + " message(s)... (bulk)");
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });

            fm.forEach((f, i) -> {
                successfulDeletions.add(f.isDone());
            });

            successfulDeletions.forEach((b) -> {
                System.out.println(b);
            });
        };
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(task);
        executor.shutdown();


//        ArrayList<Runnable> runnables = new ArrayList<>();
//        ExecutorService executor = Executors.newFixedThreadPool(15);
//        Runnable task = () -> {
//
//            for (ArrayList<Message> a : ya) {
//                runnables.add(() -> {
//                    logger.info("Deleting " + a.size() + " message(s)... (bulk)");
//                    c.deleteMessages(a);
//                });
//            }
//            for (Runnable r : runnables) {
//                executor.execute(r);
//            }
//        };
//
//        executor.execute(task);
//        executor.shutdown();


//        for (ArrayList<Message> a : ya) {
//            Runnable log = () -> {
//                logger.info("Deleting " + a.size() + " message(s)... (bulk)");
//            };
//            CompletableFuture<Void> logf = CompletableFuture.runAsync(log); // submit to a thread
//            yf.add(CompletableFuture.allOf(c.deleteMessages(a), logf));
//        }
//
//        ExecutorService executor = Executors.newFixedThreadPool(4);
//        ArrayList<Callable<Void>> callables = new ArrayList<>();
//        yf.forEach((f) -> callables.add(() -> {
//            f.get();
//            return null;
//        }));
//
//        try {
//            for (Future f : executor.invokeAll(callables)) {
//                if (!f.isDone()) {
//                    allCallableDeletionsCompleted.set(false);
//                    break;
//                }
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if (allCallableDeletionsCompleted.get()) {
//            logger.info("Deletion successful in " + c.getName() + ".");
//        } else {
//            logger.info("Deletion concluded but possibly incomplete in " + c.getName() + ".");
//        }
//
////        return CompletableFuture.allOf(yf.get(1), yf.get(2));
//
//
////        // Deletes applicable messages from channel.
////        return CompletableFuture.runAsync(() ->
////
////        {
////            // Deletes young messages
////            for (ArrayList<Message> a : ya) {
//////                c.bulkDelete(a).thenAccept((del) -> logger.info("Deletion count: " + a.size() + " (bulk)"));
////
//////                CompletableFuture<Void> future = c.bulkDelete(a);
////                CompletableFuture<Void> future = c.deleteMessages(a);
////                try {
////                    future.get();
////                    logger.info("Deletion count: " + a.size() + " (bulk)");
////                } catch (InterruptedException | ExecutionException e) {
////                    e.printStackTrace();
////                }
////            }
////
////            // Deletes old messages
//////            for (ArrayList<Message> a : oa) {
//////                c.deleteMessages(a).thenAccept((del) -> logger.info("Deletion count: " + a.size()));
//////            }
////        });
    }
}
