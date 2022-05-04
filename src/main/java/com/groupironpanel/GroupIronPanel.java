package com.groupironpanel;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.hiscore.HiscoreClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class GroupIronPanel extends PluginPanel {
    private static final int GROUP_IRON_MAX_GROUP_SIZE = 5;

    private HiscoreClient hiscoreClient;

    private JLabel groupNameLabel;

    private List<PlayerStatsPanel> playerStatsPanels;

    public GroupIronPanel(HiscoreClient hiscoreClient) {
        this.hiscoreClient = hiscoreClient;
        rebuild();
    }

    /**
     * Builds the UI controls for the panel.
     */
    private void rebuild() {
        removeAll();

        JPanel groupNamePanel = new JPanel();
        groupNamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        groupNamePanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        groupNameLabel = new JLabel();
        groupNameLabel.setFont(new Font(groupNameLabel.getFont().getName(), Font.BOLD, 18));
        groupNamePanel.add(groupNameLabel);

        final BufferedImage refreshIcon = ImageUtil.loadImageResource(getClass(), "/Refresh.png");
        JLabel refreshButton = new JLabel();
        refreshButton.setIcon(new ImageIcon(refreshIcon));
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                refreshPanel();
            }
        });
        groupNamePanel.add(refreshButton);

        add(groupNamePanel);

        playerStatsPanels = new ArrayList<>();
        for (int i = 0; i < GROUP_IRON_MAX_GROUP_SIZE; i++) {
            PlayerStatsPanel statsPanel = new PlayerStatsPanel(hiscoreClient);
            statsPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
            playerStatsPanels.add(statsPanel);
            add(statsPanel);
        }
    }

    /**
     * Sets the group name to be displayed in the panel.
     *
     * @param groupName The Group Ironman group name.
     */
    public void setGroupName(String groupName) {
        groupNameLabel.setText(groupName);
    }

    /**
     * Sets the list of group members to be displayed in the panel.
     *
     * @param groupMembers The usernames of the group members.
     */
    public void setGroupMembers(List<String> groupMembers) {
        int i = 0;
        while (i < groupMembers.size()) {
            playerStatsPanels.get(i).setPlayer(groupMembers.get(i));
            i++;
        }
        while (i < GROUP_IRON_MAX_GROUP_SIZE) {
            playerStatsPanels.get(i).setPlayer(null);
            i++;
        }
    }

    /**
     * Refreshes the hiscores for the group and displays them in the panel.
     */
    public void refreshPanel() {
        for (PlayerStatsPanel panel : playerStatsPanels) {
            panel.clearStats();
        }

        List<CompletableFuture<HiscoreResult>> hiscoreFutures = playerStatsPanels
                .stream()
                .map(panel -> hiscoreClient.lookupAsync(panel.getPlayer(), HiscoreEndpoint.NORMAL))
                .collect(Collectors.toList());

        CompletableFuture.allOf(hiscoreFutures.toArray(new CompletableFuture<?>[0])).whenCompleteAsync((result, ex) -> {
            List<HiscoreResult> results = hiscoreFutures.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    return null;
                } catch (ExecutionException e) {
                    return null;
                }
            }).collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < playerStatsPanels.size(); i++) {
                    HiscoreResult hiscoreResult = results.get(i);
                    if (hiscoreResult == null) {
                        continue;
                    }
                    playerStatsPanels.get(i).updateStats(hiscoreResult);
                }
            });
        });
    }
}
