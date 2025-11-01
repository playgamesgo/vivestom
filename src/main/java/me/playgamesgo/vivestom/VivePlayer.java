package me.playgamesgo.vivestom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import me.playgamesgo.vivestom.listeners.VivecraftNetworkListener;
import me.playgamesgo.vivestom.math.MathUtil;
import me.playgamesgo.vivestom.math.Quaternion;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;

public class VivePlayer {
    public byte[] hmdData;
    public byte[] controller0data;
    public byte[] controller1data;
    public byte[] draw;
    public float worldScale;
    private float heightScale = 1f;
    boolean isTeleportMode;
    boolean isReverseHands;
    boolean isVR;
    private byte activeHand;
    public boolean crawling;

    public Vec offset = new Vec(0, 0, 0);
    public Player player;
    public String version;

    public VivePlayer(Player player) {
        this.player = player;
    }

    public float getDraw() {
        try {
            if (draw != null) {
                ByteArrayInputStream byin = new ByteArrayInputStream(draw);
                DataInputStream da = new DataInputStream(byin);

                float draw = da.readFloat();

                da.close();
                return draw;
            }
        } catch (IOException ignored) {}

        return 0;
    }

    public byte activeHand() {
        return activeHand;
    }

    public void activeHand(byte hand) {
        this.activeHand = hand;
    }

    public float heightScale() {
        return heightScale;
    }

    public void heightScale(float heightScale) {
        this.heightScale = heightScale;
    }

    @SuppressWarnings("unused")
    public Vec getHMDDir() {
        try {
            if (hmdData != null) {
                ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
                DataInputStream da = new DataInputStream(byin);

                boolean isSeated = da.readBoolean();
                float lx = da.readFloat();
                float ly = da.readFloat();
                float lz = da.readFloat();

                float w = da.readFloat();
                float x = da.readFloat();
                float y = da.readFloat();
                float z = da.readFloat();

                Vec forward = new Vec(0, 0, -1);
                Quaternion q = new Quaternion(x, y, z, w);
                Vec out = MathUtil.mul(forward, q);

                da.close();
                return new Vec(out.x(), out.y(), out.z());
            }
        } catch (IOException ignored) {}

        return player.getPosition().direction();
    }

    @SuppressWarnings("unused")
    public Quaternion getHMDRot() {
        try {
            if (hmdData != null) {
                ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
                DataInputStream da = new DataInputStream(byin);

                boolean isSeated = da.readBoolean();
                float lx = da.readFloat();
                float ly = da.readFloat();
                float lz = da.readFloat();

                float w = da.readFloat();
                float x = da.readFloat();
                float y = da.readFloat();
                float z = da.readFloat();

                da.close();
                return new Quaternion(x, y, z, w);
            }
        } catch (IOException ignored) {}

        return new Quaternion();
    }

    @SuppressWarnings("unused")
    public Pos getHMDPos() {
        try {
            if (hmdData != null) {
                ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
                DataInputStream da = new DataInputStream(byin);

                boolean isSeated = da.readBoolean();
                float lx = da.readFloat();
                float ly = da.readFloat();
                float lz = da.readFloat();

                da.close();

                return player.getPosition().add(lx, ly, lz).add(offset.x(), offset.y(), offset.z());
            }
        } catch (IOException ignored) {}

        return player.getPosition();
    }

    @SuppressWarnings("unused")
    public Vec getControllerDir(int controller) {
        byte[] data = controller0data;

        if (controller == 1) data = controller1data;
        if (this.isSeated()) controller = 0;
        if (data != null) {
            ByteArrayInputStream byin = new ByteArrayInputStream(data);
            DataInputStream da = new DataInputStream(byin);

            try {
                this.isReverseHands = da.readBoolean();

                float lx = da.readFloat();
                float ly = da.readFloat();
                float lz = da.readFloat();

                float w = da.readFloat();
                float x = da.readFloat();
                float y = da.readFloat();
                float z = da.readFloat();

                Vec forward = new Vec(0, 0, -1);
                Quaternion q = new Quaternion(x, y, z, w);
                Vec out = MathUtil.mul(forward, q);

                da.close();
                return new Vec(out.x(), out.y(), out.z());
            } catch (IOException ignored) {}
        }

        return player.getPosition().direction();
    }

    @SuppressWarnings("unused")
    public Quaternion getControllerRot(int controller) {
        byte[] data = controller0data;

        if (controller == 1) data = controller1data;
        if (this.isSeated()) controller = 0;
        if (data != null) {
            ByteArrayInputStream byin = new ByteArrayInputStream(data);
            DataInputStream da = new DataInputStream(byin);

            try {
                this.isReverseHands = da.readBoolean();

                float lx = da.readFloat();
                float ly = da.readFloat();
                float lz = da.readFloat();

                float w = da.readFloat();
                float x = da.readFloat();
                float y = da.readFloat();
                float z = da.readFloat();

                da.close();
                return new Quaternion(x, y, z, w);
            } catch (IOException ignored) {}
        }

        return new Quaternion();
    }

    public Pos getControllerPos(int controller) {
        try {
            if (controller0data != null) {
                ByteArrayInputStream byin = new ByteArrayInputStream(controller == 0 ? controller0data : controller1data);
                DataInputStream da = new DataInputStream(byin);

                this.isReverseHands = da.readBoolean();
                float x = da.readFloat();
                float y = da.readFloat();
                float z = da.readFloat();

                da.close();

                if (this.isSeated()) {
                    Vec dir = this.getHMDDir();
                    dir = dir.rotateAroundY((float) Math.toRadians(controller == 0 ? -35 : 35));
                    dir = new Vec(dir.x(), 0, dir.z());
                    dir = dir.normalize();
                    return this.getHMDPos().add(dir.x() * 0.3 * worldScale, -0.4 * worldScale, dir.z() * 0.3 * worldScale);
                }

                return player.getPosition().add(x, y, z).add(offset.x(), offset.y(), offset.z());
            }
        } catch (IOException ignored) {}

        return player.getPosition();
    }

    public boolean isVR() {
        return this.isVR;
    }

    public void setVR(boolean vr) {
        this.isVR = vr;

        if (!vr) {
            this.hmdData = null;
            this.controller0data = null;
            this.controller1data = null;
            this.draw = null;
        }
    }

    public boolean isSeated() {
        try {
            if (hmdData == null) return false;
            if (hmdData.length < 29) return false; //old client

            ByteArrayInputStream byin = new ByteArrayInputStream(hmdData);
            DataInputStream da = new DataInputStream(byin);

            boolean seated = da.readBoolean();

            da.close();
            return seated;
        } catch (IOException ignored) {}

        return false;
    }

    public byte[] getUberPacket() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write((byte) VivecraftNetworkListener.PacketDiscriminators.UBERPACKET.ordinal());
            output.write(java.nio.ByteBuffer.allocate(8).putLong(player.getUuid().getMostSignificantBits()).array());
            output.write(java.nio.ByteBuffer.allocate(8).putLong(player.getUuid().getLeastSignificantBits()).array());
            if (hmdData.length < 29) output.write(0); // old client
            output.write(hmdData);
            output.write(controller0data);
            output.write(controller1data);
            output.write(java.nio.ByteBuffer.allocate(4).putFloat(worldScale).array());
            output.write(java.nio.ByteBuffer.allocate(4).putFloat(heightScale).array());
        } catch (IOException ignored) {}

        return output.toByteArray();
    }

    public byte[] getVRPacket() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            output.write((byte) VivecraftNetworkListener.PacketDiscriminators.IS_VR_ACTIVE.ordinal());
            output.write(isVR ? 1 : 0);
            output.write(java.nio.ByteBuffer.allocate(8).putLong(player.getUuid().getMostSignificantBits()).array());
            output.write(java.nio.ByteBuffer.allocate(8).putLong(player.getUuid().getLeastSignificantBits()).array());
        } catch (IOException ignored) {}

        return output.toByteArray();
    }
}
