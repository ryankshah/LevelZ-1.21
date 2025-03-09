package net.levelz.network.packet;

import net.levelz.LevelzMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Packet for syncing skill progress between server and client
 */
public record SkillProgressPacket(int skillId, float progress) implements CustomPayload {

    public static final CustomPayload.Id<SkillProgressPacket> PACKET_ID =
            new CustomPayload.Id<>(LevelzMain.identifierOf("skill_progress_packet"));

    public static final PacketCodec<RegistryByteBuf, SkillProgressPacket> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeInt(value.skillId);
                buf.writeFloat(value.progress);
            }, buf -> new SkillProgressPacket(buf.readInt(), buf.readFloat()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}