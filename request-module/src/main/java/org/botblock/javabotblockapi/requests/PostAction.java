/*
 * Copyright 2019 - 2020 Andre601
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.botblock.javabotblockapi.requests;

import org.botblock.javabotblockapi.core.BotBlockAPI;
import org.botblock.javabotblockapi.core.exceptions.RatelimitedException;
import org.botblock.javabotblockapi.core.requests.CheckUtil;
import org.botblock.javabotblockapi.requests.handler.RequestHandler;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PostAction{
    
    private final RequestHandler REQUEST_HANDLER;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Constructor to get an instance of PostAction.
     *
     * <p>Using this constructor will set the following default values:
     * <br><ul>
     *     <li>User-Agent: {@code "JavaBotBlockAPI-0000/API_VERSION (Unknown; +https://jbba.dev) DBots/{id}"}</li>
     * </ul>
     *
     * @param id
     *        The id of the bot. This is required for the internal User-Agent.
     *
     * @throws NullPointerException
     *         When the provided id is empty.
     */
    public PostAction(@Nonnull String id){
        this("JavaBotBlockAPI-0000/API_VERSION (Unknown; +https://jbba.dev) DBots/{id}", id);
    }
    
    /**
     * Constructor to get an instance of PostAction.
     * <br>This constructor allows you to set a own User-Agent by providing any String as the first argument.
     *
     * <p>Note that you can provide {@code {id}} inside the userAgent to get it replaced with the provided id.
     *
     * @param userAgent
     *        The Name to use as User-Agent.
     * @param id
     *        The id of the bot. This is required for the internal User-Agent.
     *
     * @throws NullPointerException
     *         When the provided userAgent or id is empty.
     */
    public PostAction(@Nonnull String userAgent, @Nonnull String id){
        CheckUtil.notEmpty(userAgent, "UserAgent");
        CheckUtil.notEmpty(id, "ID");
        
        this.REQUEST_HANDLER = new RequestHandler(userAgent.replace("{id}", id));
    }
    
    /**
     * Disables the automatic posting of Stats.
     * <br>This essentially just performs a {@link java.util.concurrent.ScheduledExecutorService#shutdown() ScheduledExecutorService.shutdown()}
     * by calling the {@link #disableAutoPost(BotBlockAPI) disableAutoPost(null)} method.
     *
     * <p>Note that using this method will NOT make the scheduler wait for previously scheduled tasks to complete.
     * <br>If you want to wait for the tasks to complete use {@link #disableAutoPost(BotBlockAPI) disableAutoPost(BotBlockAPI)} or
     * {@link #disableAutoPost(long, TimeUnit) disableAutoPost(long, TimeUnit)} instead.
     * 
     * @see java.util.concurrent.ScheduledExecutorService#shutdown()
     */
    public void disableAutoPost(){
        disableAutoPost(null);
    }
    
    /**
     * Disables the automatic posting of Stats.
     * <br>Unlike {@link #disableAutoPost() disableAutoPost()} can you make the scheduler wait for all scheduled tasks to 
     * finish, or to time out after n minutes by providing the {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlock instance}.
     * 
     * <p>Passing null as argument will just perform a {@link java.util.concurrent.ScheduledExecutorService#shutdown() ScheduledExecutorService.shutdown()}
     * similar to what the disableAutoPost() method does.
     * 
     * <p>If you want to use a different delay than what you've set in the BotBlockAPI instance, can you use 
     * {@link #disableAutoPost(long, TimeUnit) disableAutoPost(long, TimeUnit)} instead.
     * 
     * <p>This method may throw a {@link java.lang.InterruptedException InterruptedException} in the terminal.
     * 
     * @param botBlockAPI
     *        The {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlockAPI instance} or null to just perform a shutdown.
     * 
     * @since 6.0.0
     */
    public void disableAutoPost(@Nullable BotBlockAPI botBlockAPI){
        if(botBlockAPI != null){
            disableAutoPost(botBlockAPI.getUpdateDelay(), TimeUnit.MINUTES);
            return;
        }
        
        scheduler.shutdown();
    }
    
    /**
     * Disables the automatic posting of Stats.
     * <br>Unlike {@link #disableAutoPost() disableAutoPost()} can you make the scheduler wait for all scheduled tasks to 
     * finish, or to time out after a specified time frame.
     *
     * <p>This method may throw a {@link java.lang.InterruptedException InterruptedException} in the terminal.
     *
     * @param time
     *        The amount of time to wait for scheduled executions to finish before the Scheduler would time out.
     * @param timeUnit
     *        The {@link java.util.concurrent.TimeUnit TimeUnit} to use.
     * 
     * @since 6.0.0
     */
    public void disableAutoPost(long time, @Nonnull TimeUnit timeUnit){
        try{
            scheduler.awaitTermination(time, timeUnit);
        }catch(InterruptedException ex){
            ex.printStackTrace();
        }
    }
    
    /**
     * Starts a {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit) scheduleAtFixedRate}
     * task, which will post the provided guild count to the provided bot lists every n minutes.
     *
     * <p>If the post can't be performed - either by getting a {@link org.botblock.javabotblockapi.core.exceptions.RatelimitedException RatelimitedException}
     * or by getting an {@link java.io.IOException IOException} - will the exception be catched and the stacktrace printed.
     * <br>The scheduler may be canceled by this.
     *
     * <p>The scheduler will wait an initial delay of 1 minute and then performs a task every n minutes, where n is the
     * time set in {@link org.botblock.javabotblockapi.core.BotBlockAPI.Builder#setUpdateDelay(Integer) BotBlockAPI.Builder.setUpdateDelay(Integer)}
     * (default is 30 minutes).
     *
     * @param botId
     *        The ID of the bot as Long.
     * @param guilds
     *        The guild count.
     * @param botBlockAPI
     *        The {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlockAPI instance} to use.
     */
    public void enableAutoPost(@Nonnull Long botId, int guilds, @Nonnull BotBlockAPI botBlockAPI){
        scheduler.scheduleAtFixedRate(() -> {
            try{
                postGuilds(botId, guilds, botBlockAPI);
            }catch(IOException | RatelimitedException ex){
                ex.printStackTrace();
            }
        }, botBlockAPI.getUpdateDelay(), botBlockAPI.getUpdateDelay(), TimeUnit.MINUTES);
    }
    
    /**
     * Starts a {@link java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit) scheduleAtFixedRate}
     * task, which will post the provided guild count to the provided bot lists every n minutes.
     *
     * <p>If the post can't be performed - either by getting a {@link org.botblock.javabotblockapi.core.exceptions.RatelimitedException RatelimitedException}
     * or by getting an {@link java.io.IOException IOException} - will the exception be catched and the stacktrace printed.
     * <br>The scheduler may be canceled by this.
     *
     * <p>The scheduler will wait an initial delay of 1 minute and then performs a task every n minutes, where n is the
     * time set in {@link org.botblock.javabotblockapi.core.BotBlockAPI.Builder#setUpdateDelay(Integer) BotBlockAPI.Builder.setUpdateDelay(Integer)}
     * (default is 30 minutes).
     *
     * @param botId
     *        The ID of the bot as String.
     * @param guilds
     *        The guild count.
     * @param botBlockAPI
     *        The {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlockAPI instance} to use.
     */
    public void enableAutoPost(@Nonnull String botId, int guilds, @Nonnull BotBlockAPI botBlockAPI){
        scheduler.scheduleAtFixedRate(() -> {
            try{
                postGuilds(botId, guilds, botBlockAPI);
            }catch(IOException | RatelimitedException ex){
                ex.printStackTrace();
            }
        }, botBlockAPI.getUpdateDelay(), botBlockAPI.getUpdateDelay(), TimeUnit.MINUTES);
    }
    
    /**
     * Posts the guild count with the provided bot id.
     *
     * @param  botId
     *         The ID of the bot.
     * @param  guilds
     *         The guild count.
     * @param  botBlockAPI
     *         The {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlockAPI instance}.
     *
     * @throws java.io.IOException
     *         When the post request couldn't be performed.
     * @throws org.botblock.javabotblockapi.core.exceptions.RatelimitedException
     *         When we exceed the rate-limit of the BotBlock API.
     */
    public void postGuilds(@Nonnull Long botId, @Nonnull Integer guilds, @Nonnull BotBlockAPI botBlockAPI) throws IOException, RatelimitedException{
        postGuilds(Long.toString(botId), guilds, botBlockAPI);
    }
    
    /**
     * Posts the guild count with the provided bot id.
     *
     * @param  botId
     *         The ID of the bot.
     * @param  guilds
     *         The guild count.
     * @param  botBlockAPI
     *         The {@link org.botblock.javabotblockapi.core.BotBlockAPI BotBlockAPI instance}.
     *
     * @throws java.io.IOException
     *         When the post request couldn't be performed.
     * @throws org.botblock.javabotblockapi.core.exceptions.RatelimitedException
     *         When we exceed the rate-limit of the BotBlock API.
     */
    public void postGuilds(@Nonnull String botId, @Nonnull Integer guilds, @Nonnull BotBlockAPI botBlockAPI) throws IOException, RatelimitedException{
        JSONObject json = new JSONObject()
                .put("server_count", guilds)
                .put("bot_id", botId);
        
        botBlockAPI.getTokens().forEach(json::put);
        
        REQUEST_HANDLER.performPOST(json, botBlockAPI.getTokens().size());
    }
}
