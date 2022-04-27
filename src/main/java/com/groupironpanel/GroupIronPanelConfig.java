package com.groupironpanel;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("groupironpanel")
public interface GroupIronPanelConfig extends Config {
    @ConfigItem(
            keyName = "autoDetectGroup",
            name = "Auto-Detect Group",
            description = "Whether the plugin should auto-detect the group information from the group ironman tab in the game",
            position = 0
    )
    default boolean autoDetectGroup() {
        return true;
    }

    @ConfigItem(
            keyName = "groupName",
            name = "Group Name",
            description = "The name of the Group Ironman group",
            position = 1
    )
    default String groupName() {
        return "";
    }

    @ConfigItem(
            keyName = "groupName",
            name = "",
            description = ""
    )
    void setGroupName(String key);

    @ConfigItem(
            keyName = "groupMembers",
            name = "Group Members",
            description = "A comma-separated list of the group members",
            position = 2
    )
    default String groupMembers() {
        return "";
    }

    @ConfigItem(
            keyName = "groupMembers",
            name = "",
            description = ""
    )
    void setGroupMembers(String key);
}
