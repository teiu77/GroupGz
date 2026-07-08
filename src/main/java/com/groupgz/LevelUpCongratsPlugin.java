package com.groupgz;

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
        name = "Group Gz",
        description = "When you level up, nearby players will celebrate with you.",
        tags = {"level", "levelup", "fun", "gz"}
)
public class LevelUpCongratsPlugin extends Plugin
{
    // How long each message stays on screen, in milliseconds (5000 = 5 seconds).
    private static final int DISPLAY_MS = 5000;

    // The messages appear scattered across this window, in milliseconds
    private static final int STAGGER_MS = 2000;

    @Inject
    private Client client;

    @Inject
    private CongratsConfig config;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private ClientThread clientThread;

    private final Map<Skill, Integer> lastLevels = new HashMap<>();

    private final Random random = new Random();

    @Provides
    CongratsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CongratsConfig.class);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {

        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            lastLevels.clear();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Skill skill = event.getSkill();
        int newLevel = event.getLevel();

        Integer oldLevel = lastLevels.get(skill);
        lastLevels.put(skill, newLevel);

        if (oldLevel == null)
        {
            return;
        }

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

            executor.schedule(
                    () -> clientThread.invoke(() -> player.setOverheadText(message)),
                    appearDelay,
                    TimeUnit.MILLISECONDS
            );

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