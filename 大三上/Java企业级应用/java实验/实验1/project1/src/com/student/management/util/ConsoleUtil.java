package com.student.management.util;

/**
 * 控制台工具类
 * 提供进度条、边框等
 */
public class ConsoleUtil {
    public static final String RESET = "\u001B[0m"; // 重置所有格式

    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    /**
     * 显示一个简单的文本进度条
     * @param totalSteps 总步数 (例如 100)
     * @param currentStep 当前步数
     * @param message 进度条旁边显示的消息
     */
    public static void displayProgressBar(int totalSteps, int currentStep, String message) {
        int barWidth = 50; // 进度条的宽度 (字符数)
        float progress = (float) currentStep / totalSteps;
        int filledWidth = (int) (barWidth * progress);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barWidth; i++) {
            if (i < filledWidth) {
                bar.append("=");
            } else if (i == filledWidth) {
                // 在进度条头部加一个 > 符号 (如果还没满)
                bar.append(progress < 1.0f ? ">" : "=");
            } else {
                bar.append(".");
            }
        }
        bar.append("]");

        // \r (Carriage Return) 让光标回到行首, 实现原地更新
        System.out.printf("\r%s %3d%% %s", bar.toString(), (int) (progress * 100), message);

        // 如果进度完成, 打印换行符
        if (currentStep >= totalSteps) {
            System.out.println();
        }
        // 强制刷新输出缓冲区, 确保进度条实时显示
        System.out.flush();
    }

    /**
     * 模拟耗时操作并显示进度条
     * @param durationMillis 模拟的总耗时 (毫秒)
     * @param message 进度条消息
     */
    public static void simulateLoading(long durationMillis, String message) {
        int steps = 100; // 将总时间分成100步来更新进度条
        long stepDelay = durationMillis / steps;

        for (int i = 0; i <= steps; i++) {
            displayProgressBar(steps, i, message);
            try {
                // 短暂休眠
                Thread.sleep(stepDelay);
            } catch (InterruptedException e) {
                // 如果线程被中断, 停止进度条并恢复中断状态
                Thread.currentThread().interrupt();
                System.out.println("\nLoading interrupted.");
                break;
            }
        }
    }

    // --- 菜单边框 ---
    public static final String BORDER_TOP_LEFT = "╔";
    public static final String BORDER_TOP_RIGHT = "╗";
    public static final String BORDER_BOTTOM_LEFT = "╚";
    public static final String BORDER_BOTTOM_RIGHT = "╝";
    public static final String BORDER_HORIZONTAL = "═";
    public static final String BORDER_VERTICAL = "║";

    /**
     * 打印一个带边框的菜单标题
     * @param title 标题文字
     * @param totalWidth 菜单的总宽度 (字符数, 需要手动估计)
     */
    public static void printMenuHeader(String title, int totalWidth) {
        // 计算标题左右的填充长度
        int titleWidth = title.length() ; // 假设标题是中文
        int paddingTotal = totalWidth - titleWidth - 2; // 减去两边的║
        int paddingLeft = paddingTotal / 2;
        int paddingRight = paddingTotal - paddingLeft;

        System.out.println(BORDER_TOP_LEFT + BORDER_HORIZONTAL.repeat(totalWidth - 2) + BORDER_TOP_RIGHT);
        System.out.println(" ".repeat(paddingLeft) + title );//+ " ".repeat(paddingRight) + BORDER_VERTICAL
        System.out.println(BORDER_VERTICAL + BORDER_HORIZONTAL.repeat(totalWidth - 2) + BORDER_VERTICAL); // 分隔线
    }

    /**
     * 打印菜单底部边框
     * @param totalWidth 菜单的总宽度
     */
    public static void printMenuFooter(int totalWidth) {
        System.out.println(BORDER_BOTTOM_LEFT + BORDER_HORIZONTAL.repeat(totalWidth - 2) + BORDER_BOTTOM_RIGHT);
    }
}