package me.playgamesgo.vivestom;

import me.playgamesgo.vivestom.listeners.VivecraftNetworkListener;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.utils.time.TimeUnit;

import java.util.*;
import java.util.function.Function;

public class ViveStom {
    public final static String CHANNEL = "vivecraft:data";

    private static final Map<UUID, VivePlayer> vivePlayers = new HashMap<>();
    private static ViveStom INSTANCE;

    private final List<String> blockList = new ArrayList<>();
    private final VivecraftNetworkListener networkListener = new VivecraftNetworkListener(this);
    private Config config;
    private Function<String, Boolean> permissionChecker;

    public static ViveStom init(Function<String, Boolean> permissionChecker) {
        if (INSTANCE != null) return INSTANCE;
        return new ViveStom(permissionChecker);
    }

    public static ViveStom init(Config config, Function<String, Boolean> permissionChecker) {
        if (INSTANCE != null) {
            INSTANCE.config = config;
            return INSTANCE;
        }
        return new ViveStom(config, permissionChecker);
    }

    private ViveStom(Function<String, Boolean> permissionChecker) {
        this(Config.DEFAULT_CONFIG, permissionChecker);
    }

    private ViveStom(Config config, Function<String, Boolean> permissionChecker) {
        if (INSTANCE != null) throw new IllegalStateException("ViveStom is already enabled!");
        INSTANCE = this;
        this.config = config;
        this.permissionChecker = permissionChecker;

        MinecraftServer.getSchedulerManager().buildTask(this::sendPosData).repeat(1, TimeUnit.SERVER_TICK).schedule();

        MinecraftServer.getGlobalEventHandler().addListener(PlayerPluginMessageEvent.class, e -> networkListener.onPluginMessageReceived(e.getIdentifier(), e.getPlayer(), e.getMessage()));
    }

    public Config getConfig() {
        return this.config;
    }

    public Function<String, Boolean> getPermissionChecker() {
        return this.permissionChecker;
    }

    public List<String> blockList() {
        return blockList;
    }

    public static Map<UUID, VivePlayer> getVivePlayers() {
        return vivePlayers;
    }

    public void sendPosData() {
        for (VivePlayer sendTo : vivePlayers.values()) {
            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline()) continue; // dunno y but just in case.

            for (VivePlayer v : vivePlayers.values()) {
                if (v == sendTo || v == null || v.player == null ||
                        !v.player.isOnline() || v.player.getInstance() != sendTo.player.getInstance() ||
                        v.hmdData == null || v.controller0data == null || v.controller1data == null) {
                    continue;
                }

                double d = sendTo.player.getPosition().distanceSquared(v.player.getPosition());
                if (d < 256 * 256) sendTo.player.sendPluginMessage(CHANNEL, v.getUberPacket());
            }
        }
    }

    public void sendVRActiveUpdate(VivePlayer v) {
        if (v == null) return;

        var payload = v.getVRPacket();
        for (VivePlayer sendTo : vivePlayers.values()) {

            if (sendTo == null || sendTo.player == null || !sendTo.player.isOnline()) continue; // dunno y but just in case.
            if (v == sendTo || v.player == null || !v.player.isOnline()) continue;

            sendTo.player.sendPluginMessage(CHANNEL, payload);
        }
    }

    public record Config(boolean crawlingEnabled, boolean sendPlayerDataEnabled, boolean climbeyEnabled,
                         String climbeyBlockMode, String climbeyPermission, boolean teleportEnabled,
                         boolean teleportLimitedSurvival, int upLimit, int downLimit, int horizontalLimit,
                         boolean worldScaleLimitRange, double worldScaleMin, double worldScaleMax) {
        public static final Config DEFAULT_CONFIG = new Config(
                true,
                true,
                true,
                "None",
                "vive.climbanywhere",
                true,
                false,
                1,
                4,
                16,
                false,
                2,
                0.5
        );
    }
}