package com.groupironpanel;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "Group Iron Panel",
        description = "A panel that displays your Group Ironman group's skills and hiscores all in one place"
)
public class GroupIronPanelPlugin extends Plugin {
    private static final Pattern GIM_USERNAME_PATTERN = Pattern.compile("^<.*>(.*)<.*>$");

    private int autoDetectTickCooldown = 0;

    private GroupIronPanel panel;

    private NavigationButton toolbarButton;

    @Inject
    private Client client;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private GroupIronPanelConfig config;

    @Inject
    private HiscoreClient hiscoreClient;

    @Provides
    GroupIronPanelConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GroupIronPanelConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals("groupironpanel")) {
            executor.execute(this::updatePanel);
        }
    }

    @Override
    protected void startUp() throws Exception {
        panel = new GroupIronPanel(hiscoreClient);
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/GroupIron.png");
        toolbarButton = NavigationButton.builder()
                .tooltip("Group Iron")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(toolbarButton);
        updatePanel();
    }

    @Override
    protected void shutDown() throws Exception {
        clientToolbar.removeNavigation(toolbarButton);
    }

    @Subscribe
    private void onGameTick(GameTick gameTick) throws IOException {
        if (config.autoDetectGroup()) {
            // The group info won't actually change very often, so no need to spend time checking it every tick.
            if (autoDetectTickCooldown == 0) {
                tryAutoDetectGroupInformation();
                autoDetectTickCooldown = 5;
            } else if (autoDetectTickCooldown > 0) {
                autoDetectTickCooldown--;
            }
        }
    }

    /**
     * Tries to auto-detect the group name and members.
     * This will only succeed if the group ironman tab is open.
     */
    private void tryAutoDetectGroupInformation() {
        Widget groupNameContainerWidget = client.getWidget(WidgetID.GROUP_IRON_GROUP_ID, 1);
        if (groupNameContainerWidget == null) {
            return;
        }
        Widget[] groupNameWidgets = groupNameContainerWidget.getChildren();
        String groupName = null;
        if (groupNameWidgets != null) {
            for (Widget child : groupNameWidgets) {
                String widgetText = child.getText();
                if (widgetText != null && widgetText.length() > 0) {
                    groupName = widgetText;
                }
            }
        }

        Widget groupMembersContainerWidget = client.getWidget(WidgetID.GROUP_IRON_GROUP_ID, 6);
        if (groupMembersContainerWidget == null) {
            return;
        }
        Widget[] groupMemberWidgets = groupMembersContainerWidget.getChildren();
        List<String> groupMembers = new ArrayList<>();
        if (groupMemberWidgets != null) {
            for (Widget child : groupMemberWidgets) {
                String widgetName = child.getName();
                Matcher matcher = GIM_USERNAME_PATTERN.matcher(widgetName);
                if (matcher.find()) {
                    groupMembers.add(matcher.group(1));
                }
            }
        }

        if (groupName != null && !groupName.isEmpty() && groupMembers.size() > 0) {
            config.setGroupName(groupName);
            config.setGroupMembers(Text.toCSV(groupMembers));
        }
    }

    /**
     * Update the panel with the most up-to-date configuration.
     */
    private void updatePanel() {
        panel.setGroupName(config.groupName());
        panel.setGroupMembers(Text.fromCSV(config.groupMembers()));
        panel.refreshPanel();
    }
}
