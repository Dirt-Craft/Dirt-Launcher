package net.dirtcraft.dirtlauncher.lib.data.tasks.renderers;

import net.dirtcraft.dirtlauncher.lib.util.DataFormat;

public class TextRenderers {
    private static final int barLength = 25;
    public static Renderer.ProgressRenderer PROGRESS = (title, tasksCompleted, tasksTotal, percent) -> {
        String barFilled = getSolid((int) (percent * barLength));
        if (barFilled.length() < barLength) barFilled += getFractional(percent);
        System.out.printf("\r%s: |%-25s| %s/%s %25s",
                title,
                barFilled,
                tasksCompleted,
                tasksTotal,
                "");

    };
    public static Renderer.BitrateRenderer BITRATE = (title, bytesCompleted, bytesTotal, bitrate, percent) -> {
        String barFilled = getSolid((int) (percent * barLength));
        if (barFilled.length() < barLength) barFilled += getFractional(percent);
        DataFormat format = DataFormat.getMaximumDataRate(bytesTotal);
        System.out.printf("\r%s: |%-25s| %s/%s (%s) %25s",
                title,
                barFilled,
                format.toFileSize(bytesCompleted),
                format.toFileSize(bytesTotal),
                DataFormat.getBitrate(bitrate),
                "");
    };

    private static String getSolid(int amount) {
        return new String(new char[amount]).replace("\0", "█");
    }

    private static char getFractional(double percent) {
        int last = (int) (percent * (barLength << 3)) & 7;
        switch (last) {
            case 0: return '▏';
            case 1: return '▎';
            case 2: return '▍';
            case 3: return '▌';
            case 4: return '▋';
            case 5: return '▊';
            case 6: return '▉';
            case 7: return '█';
            default: return ' ';
        }
    }
}