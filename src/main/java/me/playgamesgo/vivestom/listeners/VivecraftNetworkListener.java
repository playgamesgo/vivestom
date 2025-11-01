package me.playgamesgo.vivestom.listeners;


import me.playgamesgo.vivestom.ViveStom;
import me.playgamesgo.vivestom.VivePlayer;
import me.playgamesgo.vivestom.util.MetadataHelper;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VivecraftNetworkListener {
    private final ViveStom viveStom;

    public VivecraftNetworkListener(ViveStom viveStom) {
        this.viveStom = viveStom;
    }

    public enum PacketDiscriminators {
        VERSION,
        REQUESTDATA,
        HEADDATA,
        CONTROLLER0DATA,
        CONTROLLER1DATA,
        WORLDSCALE,
        DRAW,
        MOVEMODE,
        UBERPACKET,
        TELEPORT,
        CLIMBING,
        SETTING_OVERRIDE,
        HEIGHT,
        ACTIVEHAND,
        CRAWL,
        NETWORK_VERSION,
        VR_SWITCHING,
        IS_VR_ACTIVE,
        VR_PLAYER_STATE
    }

    public void onPluginMessageReceived(String channel, Player sender, byte[] payload) {
        if (!channel.equalsIgnoreCase(ViveStom.CHANNEL)) return;
        if (payload.length == 0) return;

        VivePlayer vivePlayer = ViveStom.getVivePlayers().get(sender.getUuid());
        PacketDiscriminators disc = PacketDiscriminators.values()[payload[0]];
        if (vivePlayer == null && disc != PacketDiscriminators.VERSION) return;

        byte[] data = Arrays.copyOfRange(payload, 1, payload.length);
        switch (disc) {
            case CONTROLLER0DATA:
                vivePlayer.controller0data = data;
                MetadataHelper.updateMetadata(vivePlayer);
                break;
            case CONTROLLER1DATA:
                vivePlayer.controller1data = data;
                MetadataHelper.updateMetadata(vivePlayer);
                break;
            case DRAW:
                vivePlayer.draw = data;
                break;
            case HEADDATA:
                vivePlayer.hmdData = data;
                MetadataHelper.updateMetadata(vivePlayer);
                break;
            case REQUESTDATA:
                //only we can use that word.
                break;
            case VERSION:
                vivePlayer = new VivePlayer(sender);
                ByteArrayInputStream byin = new ByteArrayInputStream(data);
                DataInputStream da = new DataInputStream(byin);
                InputStreamReader is = new InputStreamReader(da);
                BufferedReader br = new BufferedReader(is);
                ViveStom.getVivePlayers().put(sender.getUuid(), vivePlayer);

                sender.sendPluginMessage(ViveStom.CHANNEL, StringToPayload(PacketDiscriminators.VERSION, "Vivecraft-Spigot-Extensions"));

                try {
                    String version = br.readLine();
                    vivePlayer.version = version;
                    vivePlayer.setVR(!version.contains("NONVR"));

                    if (viveStom.getConfig().sendPlayerDataEnabled())
                        sender.sendPluginMessage(ViveStom.CHANNEL, new byte[]{(byte) PacketDiscriminators.REQUESTDATA.ordinal()});

                    if (viveStom.getConfig().crawlingEnabled())
                        sender.sendPluginMessage(ViveStom.CHANNEL, new byte[]{(byte) PacketDiscriminators.CRAWL.ordinal()});

                    sender.sendPluginMessage(ViveStom.CHANNEL, new byte[]{(byte) PacketDiscriminators.VR_SWITCHING.ordinal(), 0});

                    if (viveStom.getConfig().climbeyEnabled()) {

                        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        byteArrayOutputStream.write(PacketDiscriminators.CLIMBING.ordinal());
                        byteArrayOutputStream.write(1); // climbey allowed

                        String mode = viveStom.getConfig().climbeyBlockMode();
                        if (!viveStom.getPermissionChecker().apply(viveStom.getConfig().climbeyPermission())) {
                            if (mode.trim().equalsIgnoreCase("include")) byteArrayOutputStream.write(1);
                            else if (mode.trim().equalsIgnoreCase("exclude")) byteArrayOutputStream.write(2);
                            else byteArrayOutputStream.write(0);
                        } else {
                            byteArrayOutputStream.write(0);
                        }

                        for (String block : viveStom.blockList()) {
                            if (!writeString(byteArrayOutputStream, block))
                                System.out.println("ViveStom [WARNING]: Block name too long: " + block);
                        }

                        final byte[] p = byteArrayOutputStream.toByteArray();
                        sender.sendPluginMessage(ViveStom.CHANNEL, p);
                    }

                    if (viveStom.getConfig().teleportLimitedSurvival()) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "limitedTeleport", true);
                        writeSetting(baos, "teleportLimitUp", Math.clamp(viveStom.getConfig().upLimit(), 0, 4));
                        writeSetting(baos, "teleportLimitDown", Math.clamp(viveStom.getConfig().downLimit(), 0, 16));
                        writeSetting(baos, "teleportLimitHoriz", Math.clamp(viveStom.getConfig().horizontalLimit(), 0, 32));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(ViveStom.CHANNEL, p);
                    }

                    if (viveStom.getConfig().worldScaleLimitRange()) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "worldScale.min", Math.clamp(viveStom.getConfig().worldScaleMin(), 0.1, 100));
                        writeSetting(baos, "worldScale.max", Math.clamp(viveStom.getConfig().worldScaleMax(), 0.1, 100));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(ViveStom.CHANNEL, p);
                    }

                    if (viveStom.getConfig().teleportEnabled())
                        sender.sendPluginMessage(ViveStom.CHANNEL, new byte[]{(byte) PacketDiscriminators.TELEPORT.ordinal()});

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case WORLDSCALE:
                ByteArrayInputStream a = new ByteArrayInputStream(data);
                DataInputStream b = new DataInputStream(a);
                try {
                    vivePlayer.worldScale = b.readFloat();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case HEIGHT:
                ByteArrayInputStream a1 = new ByteArrayInputStream(data);
                DataInputStream b1 = new DataInputStream(a1);
                try {
                    vivePlayer.heightScale(b1.readFloat());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            case TELEPORT:
                if (!viveStom.getConfig().teleportEnabled())
                    break;

                ByteArrayInputStream in = new ByteArrayInputStream(data);
                DataInputStream d = new DataInputStream(in);
                try {
                    float x = d.readFloat();
                    float y = d.readFloat();
                    float z = d.readFloat();
                    Pos newPos = new Pos(x, y, z, sender.getPosition().yaw(), sender.getPosition().pitch());
                    sender.refreshPosition(newPos);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case CLIMBING:
                sender.refreshOnGround(true);
                break;
            case ACTIVEHAND:
                ByteArrayInputStream a2 = new ByteArrayInputStream(data);
                DataInputStream b2 = new DataInputStream(a2);
                try {
                    vivePlayer.activeHand(b2.readByte());
                    if (vivePlayer.isSeated()) vivePlayer.activeHand((byte) 0x00);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case CRAWL:
                if (!viveStom.getConfig().crawlingEnabled())
                    break;
                ByteArrayInputStream a3 = new ByteArrayInputStream(data);
                DataInputStream b3 = new DataInputStream(a3);
                try {
                    vivePlayer.crawling = b3.readBoolean();
                    if (vivePlayer.crawling)
                        sender.setPose(EntityPose.SWIMMING);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case IS_VR_ACTIVE:
                ByteArrayInputStream vrb = new ByteArrayInputStream(data);
                DataInputStream vrd = new DataInputStream(vrb);
                boolean vr;
                try {
                    vr = vrd.readBoolean();
                    if (vivePlayer.isVR() == vr) break;
                    vivePlayer.setVR(vr);
                    if (!vr) {
                        viveStom.sendVRActiveUpdate(vivePlayer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case NETWORK_VERSION:
                //don't care yet.
                break;
            case VR_PLAYER_STATE:
                //todo.
                break;
            default:
                break;
        }
    }

    public void writeSetting(ByteArrayOutputStream output, String name, Object value) {
        if (!writeString(output, name)) {
            System.out.println("ViveStom [WARNING]: Setting name too long: " + name);
            return;
        }
        if (!writeString(output, value.toString())) {
            System.out.println("ViveStom [WARNING]: Setting value too long: " + value);
            writeString(output, "");
        }
    }

    public static byte[] StringToPayload(PacketDiscriminators version, String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write((byte) version.ordinal());
        if (!writeString(output, input)) {
            output.reset();
            return output.toByteArray();
        }

        return output.toByteArray();

    }

    public static boolean writeString(ByteArrayOutputStream output, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        try {
            if (!writeVarInt(output, len, 2)) return false;
            output.write(bytes);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static int varIntByteCount(int toCount) {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : ((toCount & 0xFFFFC000) == 0 ? 2 : ((toCount & 0xFFE00000) == 0 ? 3 : ((toCount & 0xF0000000) == 0 ? 4 : 5)));
    }

    public static boolean writeVarInt(ByteArrayOutputStream to, int toWrite, int maxSize) {
        if (varIntByteCount(toWrite) > maxSize) return false;
        while ((toWrite & -128) != 0) {
            to.write(toWrite & 127 | 128);
            toWrite >>>= 7;
        }

        to.write(toWrite);
        return true;
    }
}
