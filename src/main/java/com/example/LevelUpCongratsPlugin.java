package com.example;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Level Up Congrats",
        description = "When you level up, nearby players cheer you on (only on your screen).",
        tags = {"level", "levelup", "fun", "congrats"}
)
public class LevelUpCongratsPlugin extends Plugin
{
    // How long each message stays on screen, in milliseconds (5000 = 5 seconds).
    // Change this number if you want them to last longer or shorter.
    private static final int DISPLAY_MS = 5000;

    // The messages appear scattered across this window, in milliseconds
    // (1000 = spread out over 1 second) so they don't all pop up at once.
    private static final int STAGGER_MS = 2000;

    @Inject
    private Client client;

    @Inject
    private CongratsConfig config;

    // Lets us run something a little later -- used for the stagger and the auto-clear.
    @Inject
    private ScheduledExecutorService executor;

    // Anything that changes the game's on-screen state must run on this special
    // "client thread." We hand our message changes to it to stay safe.
    @Inject
    private ClientThread clientThread;

    // Remembers the last level we saw for each skill, so we can tell when one goes UP.
    private final Map<Skill, Integer> lastLevels = new HashMap<>();

    // A dice-roller for picking random messages and random delays.
    private final Random random = new Random();

    @Provides
    CongratsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CongratsConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        // Back at the login screen? Forget old levels so the next login starts
        // fresh and doesn't mistake the login "level flood" for real level-ups.
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            lastLevels.clear();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Skill skill = event.getSkill();
        int newLevel = event.getLevel(); // your "real" level from total XP

        Integer oldLevel = lastLevels.get(skill);
        lastLevels.put(skill, newLevel);

        // Nothing recorded yet = the login flood. Stay quiet.
        if (oldLevel == null)
        {
            return;
        }

        // Only celebrate if the level genuinely went up.
        if (newLevel > oldLevel)
        {
            cheerFromNearbyPlayers();
        }
    }

    private void cheerFromNearbyPlayers()
    {
        String[] messages = config.messages().split(",");
        Player you = client.getLocalPlayer();

        for (Player player : client.getPlayers())
        {
            if (player == null || player == you)
            {
                continue;
            }

            String message = messages[random.nextInt(messages.length)].trim();
            if (message.isEmpty())
            {
                continue;
            }

            // A random delay between 0 and STAGGER_MS, so each player's
            // message appears at a slightly different moment.
            int appearDelay = random.nextInt(STAGGER_MS);

            // Show the message after its little delay.
            executor.schedule(
                    () -> clientThread.invoke(() -> player.setOverheadText(message)),
                    appearDelay,
                    TimeUnit.MILLISECONDS
            );

            // Erase it DISPLAY_MS after it appeared -- but only if it's still the
            // same message, so we don't wipe out a newer one that replaced it.
            executor.schedule(
                    () -> clientThread.invoke(() ->
                    {
                        if (message.equals(player.getOverheadText()))
                        {
                            player.setOverheadText(null);
                        }
                    }),
                    appearDelay + DISPLAY_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }
}