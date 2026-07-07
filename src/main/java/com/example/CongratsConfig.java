package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

// This interface creates the little settings panel for your plugin.
// RuneLite reads it automatically -- you don't call it yourself.
@ConfigGroup("levelupcongrats")
public interface CongratsConfig extends Config
{
    @ConfigItem(
            keyName = "messages",
            name = "Messages",
            description = "Congratulation messages, separated by commas. One is picked at random for each nearby player."
    )
    default String messages()
    {
        // Edit these in-game in the plugin's settings -- no need to touch code.
        return "Gzz,Gzzzzz,Gzz@@@@@@,Gz,@@@@@@@@@@";
    }
}