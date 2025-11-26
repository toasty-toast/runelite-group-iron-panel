package com.groupironpanel;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Experience;
import net.runelite.client.RuneLite;
import net.runelite.client.hiscore.*;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.hiscore.HiscoreSkill.*;

@Slf4j
public class PlayerStatsPanel extends JPanel {
    private static final List<HiscoreSkill> SKILLS = ImmutableList.of(
            ATTACK, HITPOINTS, MINING,
            STRENGTH, AGILITY, SMITHING,
            DEFENCE, HERBLORE, FISHING,
            RANGED, THIEVING, COOKING,
            PRAYER, CRAFTING, FIREMAKING,
            MAGIC, FLETCHING, WOODCUTTING,
            RUNECRAFT, SLAYER, FARMING,
            CONSTRUCTION, HUNTER, SAILING
    );

    private Map<HiscoreSkill, JLabel> skillToLabel = new HashMap<>();

    private HiscoreClient hiscoreClient;

    private String username;

    private JLabel usernameLabel;

    public PlayerStatsPanel(HiscoreClient hiscoreClient) {
        this.hiscoreClient = hiscoreClient;
        buildPanel();
    }

    /**
     * Clears the stats on the panel.
     */
    public void clearStats() {
        for (Map.Entry<HiscoreSkill, JLabel> entry : skillToLabel.entrySet()) {
            JLabel label = entry.getValue();
            if (label != null) {
                label.setText("-");
            }
        }
    }

    /**
     * Updates the stats with a given hiscore result.
     *
     * @param playerResult     The hiscore result for the player displayed in this panel.
     */
    public void updateStats(HiscoreResult playerResult) {
        if (playerResult == null) {
            return;
        }

        for (Map.Entry<HiscoreSkill, JLabel> entry : skillToLabel.entrySet()) {
            HiscoreSkill skill = entry.getKey();
            JLabel label = entry.getValue();

            if (label == null) {
                continue;
            }

            Skill resultSkill;

            if (skill == null) {
                int combatLevel = Experience.getCombatLevel(
                        playerResult.getSkill(ATTACK).getLevel(),
                        playerResult.getSkill(STRENGTH).getLevel(),
                        playerResult.getSkill(DEFENCE).getLevel(),
                        playerResult.getSkill(HITPOINTS).getLevel(),
                        playerResult.getSkill(MAGIC).getLevel(),
                        playerResult.getSkill(RANGED).getLevel(),
                        playerResult.getSkill(PRAYER).getLevel()
                );
                label.setText(Integer.toString(combatLevel));
            } else if ((resultSkill = playerResult.getSkill(skill)) != null) {
                final long experience = resultSkill.getExperience();
                if (experience > -1 && skill.getType() == HiscoreSkillType.SKILL) {
                    label.setText(String.valueOf(Experience.getLevelForXp((int) experience)));
                } else {
                    label.setText(String.valueOf(resultSkill.getLevel()));
                }
            }

            label.setToolTipText(buildSkillPanelTooltip(playerResult, skill));
        }
    }

    /**
     * Returns the name of the player represented by this stats panel.
     */
    public String getPlayer() {
        return this.username;
    }

    /**
     * Updates the player to be show in the panel. This does not cause the panel to refresh the player stats.
     *
     * @param username The name of the player to show in this stats panel.
     */
    public void setPlayer(String username) {
        this.username = username;
        usernameLabel.setText(this.username);
        if (username == null || username.isEmpty()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
    }

    /**
     * Builds the UI controls for the player stats panel.
     */
    private void buildPanel() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 5, 0);

        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel usernamePanel = new JPanel();
        usernamePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        usernameLabel = new JLabel();
        usernamePanel.add(usernameLabel);
        add(usernamePanel, constraints);
        constraints.gridy++;

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(8, 3));
        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        for (HiscoreSkill skill : SKILLS) {
            statsPanel.add(buildSkillPanel(skill));
        }
        add(statsPanel, constraints);
        constraints.gridy++;

        JPanel summaryStatsPanel = new JPanel();
        summaryStatsPanel.setLayout(new GridLayout(1, 2));
        summaryStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        summaryStatsPanel.add(buildSkillPanel(null));
        summaryStatsPanel.add(buildSkillPanel(OVERALL));
        add(summaryStatsPanel, constraints);
        constraints.gridy++;

        setVisible(false);
    }

    /**
     * Builds the JPanel to display a single skill for the player.
     */
    private JPanel buildSkillPanel(HiscoreSkill skill) {
        JPanel panel = new JPanel();
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        String skillName = (skill == null ? "combat" : skill.name().toLowerCase());

        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(ImageUtil.loadImageResource(RuneLite.class, "/skill_icons_small/" + skillName + ".png")));
        label.setText("-");
        label.setIconTextGap(skill == OVERALL || skill == null ? 10 : 4);
        label.setToolTipText(skill == null ? "Combat" : skill.getName());
        skillToLabel.put(skill, label);
        panel.add(label);

        return panel;
    }

    /**
     * Builds an HTML tooltip for a single skill in the panel.
     * This logic is taken from the builtin Hiscore plugin.
     *
     * @param playerResult     The hiscore result for the player.
     * @param skill            The skill to build the tooltip for.
     */
    private String buildSkillPanelTooltip(HiscoreResult playerResult, HiscoreSkill skill) {
        StringBuilder builder = new StringBuilder();
        builder.append("<html><body style = 'padding: 5px;color:#989898'>");

        if (skill == null) {
            double combatLevel = Experience.getCombatLevelPrecise(
                    playerResult.getSkill(ATTACK).getLevel(),
                    playerResult.getSkill(STRENGTH).getLevel(),
                    playerResult.getSkill(DEFENCE).getLevel(),
                    playerResult.getSkill(HITPOINTS).getLevel(),
                    playerResult.getSkill(MAGIC).getLevel(),
                    playerResult.getSkill(RANGED).getLevel(),
                    playerResult.getSkill(PRAYER).getLevel()
            );
            double combatExperience = playerResult.getSkill(ATTACK).getExperience()
                    + playerResult.getSkill(STRENGTH).getExperience() + playerResult.getSkill(DEFENCE).getExperience()
                    + playerResult.getSkill(HITPOINTS).getExperience() + playerResult.getSkill(MAGIC).getExperience()
                    + playerResult.getSkill(RANGED).getExperience() + playerResult.getSkill(PRAYER).getExperience();
            builder.append("<p><span style = 'color:white'>Combat</span></p>");
            builder.append("<p><span style = 'color:white'>Exact Combat Level:</span> " + QuantityFormatter.formatNumber(combatLevel) + "</p>");
            builder.append("<p><span style = 'color:white'>Experience:</span> " + QuantityFormatter.formatNumber(combatExperience) + "</p>");
        } else if (skill == HiscoreSkill.OVERALL) {
            Skill overallSkill = playerResult.getSkill(skill);
            String rank = (overallSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(overallSkill.getRank());
            String exp = (overallSkill.getExperience() == -1L) ? "Unranked" : QuantityFormatter.formatNumber(overallSkill.getExperience());
            builder.append("<p><span style = 'color:white'>" + skill.getName() + "</span></p>");
            builder.append("<p><span style = 'color:white'>Rank:</span> " + rank + "</p>");
            builder.append("<p><span style = 'color:white'>Experience:</span> " + exp + "</p>");
        } else {
            Skill requestedSkill = playerResult.getSkill(skill);
            final long experience = requestedSkill.getExperience();

            String rank = (requestedSkill.getRank() == -1) ? "Unranked" : QuantityFormatter.formatNumber(requestedSkill.getRank());
            String exp = (experience == -1L) ? "Unranked" : QuantityFormatter.formatNumber(experience);
            String remainingXp;
            if (experience == -1L) {
                remainingXp = "Unranked";
            } else {
                int currentLevel = Experience.getLevelForXp((int) experience);
                remainingXp = (currentLevel + 1 <= Experience.MAX_VIRT_LEVEL) ? QuantityFormatter.formatNumber(Experience.getXpForLevel(currentLevel + 1) - experience) : "0";
            }

            builder.append("<p><span style = 'color:white'>Skill:</span> " + skill.getName() + "</p>");
            builder.append("<p><span style = 'color:white'>Rank:</span> " + rank + "</p>");
            builder.append("<p><span style = 'color:white'>Experience:</span> " + exp + "</p>");
            builder.append("<p><span style = 'color:white'>Remaining XP:</span> " + remainingXp + "</p>");
        }

        if (skill != null && skill.getType() == HiscoreSkillType.SKILL) {
            long experience = playerResult.getSkill(skill).getExperience();
            if (experience >= 0) {
                int currentXp = (int) experience;
                int currentLevel = Experience.getLevelForXp(currentXp);
                int xpForCurrentLevel = Experience.getXpForLevel(currentLevel);
                int xpForNextLevel = currentLevel + 1 <= Experience.MAX_VIRT_LEVEL ? Experience.getXpForLevel(currentLevel + 1) : -1;

                double xpGained = currentXp - xpForCurrentLevel;
                double xpGoal = xpForNextLevel != -1 ? xpForNextLevel - xpForCurrentLevel : 100;
                int progress = (int) ((xpGained / xpGoal) * 100f);

                builder.append("<div style = 'margin-top:3px'>");
                builder.append("<div style = 'background: #070707; border: 1px solid #070707; height: 6px; width: 100%;'>");
                builder.append("<div style = 'height: 6px; width: " + progress + "%; background: #dc8a00;'>");
                builder.append("</div>");
                builder.append("</div>");
                builder.append("</div>");
            }
        }

        builder.append("</body></html>");
        return builder.toString();
    }
}
