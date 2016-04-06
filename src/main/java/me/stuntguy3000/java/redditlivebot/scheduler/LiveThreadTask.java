package me.stuntguy3000.java.redditlivebot.scheduler;

import lombok.Getter;
import me.stuntguy3000.java.redditlivebot.RedditLiveBot;
import me.stuntguy3000.java.redditlivebot.handler.RedditHandler;
import me.stuntguy3000.java.redditlivebot.hook.TelegramHook;
import me.stuntguy3000.java.redditlivebot.object.Lang;
import me.stuntguy3000.java.redditlivebot.object.reddit.LiveThread;
import me.stuntguy3000.java.redditlivebot.object.reddit.livethread.LiveThreadChildren;
import me.stuntguy3000.java.redditlivebot.object.reddit.livethread.LiveThreadChildrenData;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.Chat;

import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

// @author Luke Anderson | stuntguy3000
public class LiveThreadTask extends TimerTask {
    @Getter
    private RedditLiveBot plugin;
    @Getter
    private LiveThreadChildrenData lastPost = null;
    @Getter
    private String threadID;

    public LiveThreadTask(String threadID, LiveThreadChildrenData lastPost) {
        this.lastPost = lastPost;
        this.plugin = RedditLiveBot.getInstance();
        this.threadID = threadID;

        new Timer().schedule(this, 0, 10 * 1000);
    }

    private void postUpdate(LiveThreadChildrenData data) {
        if (data != null) {
            lastPost = data;
            Lang.send(TelegramHook.getRedditLiveChat(),
                    Lang.LIVE_THREAD_UPDATE, getThreadID(), data.getAuthor(), data.getBody());

            for (String chatID : plugin.getSubscriptionHandler().getSubscriptions()) {
                Chat chat = TelegramBot.getChat(chatID);

                if (chat != null) {
                    Lang.send(chat,
                            Lang.LIVE_THREAD_UPDATE, getThreadID(), data.getAuthor(), data.getBody());
                } else {
                    plugin.getSubscriptionHandler().unsubscribeChat(chatID);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            LiveThread liveThread = RedditHandler.getLiveThread(threadID);

            LinkedList<LiveThreadChildrenData> updates = new LinkedList<>();

            for (LiveThreadChildren liveThreadChild : liveThread.getData().getChildren()) {
                LiveThreadChildrenData data = liveThreadChild.getData();

                Lang.sendDebug("LP: %s Data: %s Compare: %s", lastPost.getId(), data.getId(), data.getId() == lastPost.getId());
                if (lastPost == null || data.getId() != lastPost.getId()) {
                    Lang.sendDebug("1");
                    updates.add(data);
                } else {
                    Lang.sendDebug("2");
                    break;
                }
            }


            if (lastPost == null) {
                postUpdate(updates.get(0));
            } else {
                updates.forEach(this::postUpdate);

                if (updates.isEmpty()) {
                    long secs = (new Date().getTime()) / 1000;

                    // Older than 6 hours?
                    if ((secs - lastPost.getCreated_utc()) > 21600) {
                        plugin.getRedditHandler().stopLiveThread();
                    }
                }
            }
        } catch (Exception e) {
            Lang.sendDebug("Exception Caught: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
    