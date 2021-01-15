package net.runelite.client.plugins.vroedoe;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("vroedoe")
public interface VroedoeConfig extends Config {
    @ConfigItem(
            keyName = "gibsMagic",
            name = "Gibs magic xp",
            description = "Gibs magic xp",
            position = 1
    )
    default boolean gibsMagic() { return false; }
}
