package me.vaimok.skid.client.modules.combat;

import me.vaimok.skid.api.util.moduleUtil.*;
import me.vaimok.skid.client.modules.Module;
import me.vaimok.skid.client.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;


/**
 @author kambing
 @since 30/8/21
 credits: salhack for calcs
 */

public final class ObiAssist extends Module {

    private final Setting<Boolean> packet = register(new Setting<Boolean>("PacketSwitch", true));
    private final Setting<Boolean> packethand = register(new Setting<Boolean>("PacketHand", true));
    private final Setting<Boolean> render = register(new Setting<Boolean>("Render", true));
    private final Setting<Double> range = register(new Setting<Double>("TargetMaxRange", 10.0, 5.0, 15.0));
    private final Setting<Double> delay = register(new Setting<Double>("Delay (MS)", 200.0, 0.0, 500.0));


    private static ObiAssist instance;

    private final Timer delayTimer = new Timer ( );

    public ObiAssist() {
        super("ObiAssist", "place obsidian to support your  .tocrystal", Category.COMBAT, true, false, false);
        instance = this;
    }

    @Override
    public void onUpdate() {
        EntityPlayer target = NekoAura.target;
        int slot = InventoryUtil.findHotbarBlock(BlockObsidian.class);
        int old = mc.player.inventory.currentItem;
        EnumHand hand = null;

        if (NekoAura.getInstance().currentTarget == null)
            return;

        if (NekoAura.getInstance().placePos != null)
            return;

        if (NekoAura.getInstance().isEnabled() && NekoAura.getInstance().currentTarget != null && NekoAura.getInstance().placePos == null) {
            if (slot != -1) {
                if (delayTimer.passedMs(delay.getValue().longValue())) {
                    if (mc.player.inventory.currentItem != slot) {
                        if (packet.getValue()) {
                            if (mc.player.isHandActive()) {
                                hand = mc.player.getActiveHand();
                            }
                            mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
                        }
                    }
                }
            }
        }
        try {

            if (!(target.getDistance(mc.player) > range.getValue())) {
                float range = NekoAura.getInstance().placeRange.getValue();

                float targetDMG = 0.0f;
                float minDmg = NekoAura.getInstance().minMinDmg.getValue();

                BlockPos targetPos = null;

                for (BlockPos pos : BlockUtil.getSphere(PlayerUtil.getPlayerPosFloored(), range, (int) range, false, true, 0)) {
                    BlockUtil.ValidResult result = BlockUtil.valid(pos);

                    if (result != BlockUtil.ValidResult.Ok)
                        continue;

                    if (!crystalPlacePosPredict(pos))
                        continue;

                    float tempDMG = DamageUtil.calculateDamage(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, target);


                    if (tempDMG < minDmg)
                        continue;

                    if (tempDMG >= targetDMG) {
                        targetPos = pos;
                        targetDMG = tempDMG;
                    }
                }

                if (targetPos != null && render.getValue()) {
                    NekoAura.getInstance().renderPos = targetPos;
                }

                if (targetPos != null) {
                    BlockUtil.placeBlock(targetPos);
                    delayTimer.reset();
                }
                if (packet.getValue()) {
                    if (slot != -1) {
                        mc.player.connection.sendPacket(new CPacketHeldItemChange(old));
                        if (packethand.getValue() && hand != null) {
                            mc.player.setActiveHand(hand);
                        }
                    }
                }
            }

        } catch (NullPointerException ignored) {
            //to avoid ticking entity crash
        }
    }

    public static boolean crystalPlacePosPredict(final BlockPos pos) {

        final Block floor = mc.world.getBlockState(pos.add(0, 1, 0)).getBlock();
        final Block ceil = mc.world.getBlockState(pos.add(0, 2, 0)).getBlock();

        if (floor == Blocks.AIR && ceil == Blocks.AIR) {
            if (mc.world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(pos.add(0, 1, 0))).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
