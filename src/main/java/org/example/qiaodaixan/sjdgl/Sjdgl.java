package org.example.qiaodaixan.sjdgl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Sjdgl implements ModInitializer {

    private final List<TimeRange> blockTimes = new ArrayList<>(); // 禁止时间段列表
    private final List<String> whitelist = new ArrayList<>(); // 白名单列表
    private String kickMessage = "服务器在此时间段不可用！"; // 踢出消息，默认值
    private static final File CONFIG_FILE = new File("config/sjdgl/config.json");
    private long lastModified = 0; // 配置文件的最后修改时间戳
    private MinecraftServer serverInstance; // 保存服务器实例

    @Override
    public void onInitialize() {
        // 检查并生成默认配置文件
        createDefaultConfigIfNotExists();

        // 首次加载配置文件
        loadConfig();

        // 注册服务器启动事件，保存服务器实例并启动定时检测任务
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            System.out.println("服务器已启动，开始定时检测在线玩家和配置文件！");
            startMonitorTask();
        });

        System.out.println("时间段管理插件已启动，已加载配置的禁止时间段和白名单！");
    }

    // 如果配置文件不存在，则创建默认配置文件
    private void createDefaultConfigIfNotExists() {
        if (!CONFIG_FILE.exists()) {
            try {
                CONFIG_FILE.getParentFile().mkdirs();
                CONFIG_FILE.createNewFile();

                // 写入默认配置
                Config defaultConfig = new Config();
                defaultConfig.block_times = new ArrayList<>();
                defaultConfig.block_times.add(new Config.TimeRangeJson("12:00", "15:00"));
                defaultConfig.block_times.add(new Config.TimeRangeJson("19:00", "22:00"));
                defaultConfig.whitelist = new ArrayList<>();
                defaultConfig.whitelist.add("Admin"); // 默认白名单玩家
                defaultConfig.kick_message = "服务器维护中，请稍后再试！";

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                    gson.toJson(defaultConfig, writer);
                }

                System.out.println("已创建默认配置文件：" + CONFIG_FILE.getAbsolutePath());

            } catch (IOException e) {
                System.err.println("无法创建配置文件：" + e.getMessage());
            }
        }
    }

    // 加载配置文件
    private void loadConfig() {
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Gson gson = new Gson();

            // 定义 JSON 对应的类型
            Type configType = new TypeToken<Config>() {}.getType();

            // 解析 JSON
            Config config = gson.fromJson(reader, configType);

            // 清空当前时间段列表并加载新配置
            blockTimes.clear();
            for (Config.TimeRangeJson range : config.block_times) {
                blockTimes.add(new TimeRange(
                        LocalTime.parse(range.start),
                        LocalTime.parse(range.end)
                ));
            }

            // 清空白名单并加载新配置
            whitelist.clear();
            if (config.whitelist != null) {
                whitelist.addAll(config.whitelist);
            }

            // 加载踢出消息
            if (config.kick_message != null && !config.kick_message.isEmpty()) {
                kickMessage = config.kick_message;
            }

            System.out.println("配置文件已重新加载！白名单: " + whitelist + " 禁止时间段: " + blockTimes);
            System.out.println("踢出消息: " + kickMessage);

        } catch (Exception e) {
            System.err.println("无法加载配置文件：" + e.getMessage());
        }
    }

    // 启动定时任务，检查配置文件和在线玩家
    private void startMonitorTask() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (serverInstance != null) {
                    // 检测配置文件是否被修改
                    checkAndReloadConfig();

                    // 检查在线玩家并踢出非白名单玩家
                    checkAndKickOnlinePlayers();
                }
            }
        }, 0, 5000); // 每 5 秒执行一次
    }

    // 检测配置文件是否被修改并重新加载
    private void checkAndReloadConfig() {
        if (CONFIG_FILE.exists() && CONFIG_FILE.lastModified() > lastModified) {
            lastModified = CONFIG_FILE.lastModified(); // 更新最后修改时间戳
            loadConfig(); // 重新加载配置文件
        }
    }

    // 检查并踢出处于禁止时间段的玩家
    private void checkAndKickOnlinePlayers() {
        List<ServerPlayerEntity> players = new ArrayList<>(serverInstance.getPlayerManager().getPlayerList());

        for (ServerPlayerEntity player : players) {
            if (isBlockedTime()) {
                // 如果玩家在白名单中，跳过踢出
                if (isWhitelisted(player)) {
                    System.out.println("白名单玩家: " + player.getName().getString() + " 在禁止时间段内免于踢出。");
                    continue;
                }

                // 踢出非白名单玩家
                System.out.println("踢出玩家: " + player.getName().getString() + "，原因: " + kickMessage);
                kickPlayer(player);
            }
        }
    }

    // 检查当前时间是否在禁止时间段内
    private boolean isBlockedTime() {
        LocalTime now = LocalTime.now();
        for (TimeRange range : blockTimes) {
            if (now.isAfter(range.start) && now.isBefore(range.end)) {
                return true;
            }
        }
        return false;
    }

    // 检查玩家是否在白名单中
    private boolean isWhitelisted(ServerPlayerEntity player) {
        return whitelist.contains(player.getName().getString());
    }

    // 踢出玩家
    private void kickPlayer(ServerPlayerEntity player) {
        player.networkHandler.disconnect(net.minecraft.text.Text.of(kickMessage));
    }

    // 内部类表示时间范围
    private static class TimeRange {
        private final LocalTime start;
        private final LocalTime end;

        public TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "[" + start + " - " + end + "]";
        }
    }

    // JSON 配置文件的类
    private static class Config {
        private List<TimeRangeJson> block_times;
        private List<String> whitelist;
        private String kick_message;

        private static class TimeRangeJson {
            private String start;
            private String end;

            public TimeRangeJson(String start, String end) {
                this.start = start;
                this.end = end;
            }
        }
    }
}
