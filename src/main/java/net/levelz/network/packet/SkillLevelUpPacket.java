package net.levelz.network.packet;

import net.levelz.LevelzMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Packet for notifying the client about skill level up
 */
public record SkillLevelUpPacket(int skillId, int level) implements CustomPayload {

    public static final CustomPayload.Id<SkillLevelUpPacket> PACKET_ID =
            new CustomPayload.Id<>(LevelzMain.identifierOf("skill_levelup_packet"));

    public static final PacketCodec<RegistryByteBuf, SkillLevelUpPacket> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeInt(value.skillId);
                buf.writeInt(value.level);
            }, buf -> new SkillLevelUpPacket(buf.readInt(), buf.readInt()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}