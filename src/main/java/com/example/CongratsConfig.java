package com.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;


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
        return "Gzz,Gzzzzz,Gzz@@@@@@,Gz,@@@@@@@@@@";
    }
}