package me.vaimok.skid.client.modules.combat;

import me.vaimok.skid.api.util.moduleUtil.*;
import me.vaimok.skid.client.modules.Module;
import me.vaimok.skid.client.setting.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockObsidian;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 @author kambing
 @since 30/8/21
 credits: salhack for calcs
 */

public final class ObiAssist extends Module {

    private final Setting<Boolean> packet = register(new Setting<Boolean>("PacketSwitch", true));
    private final Setting<Boolean> packethand = register(new Setting<Boolean>("PacketHand", true));
    private final Setting<Boolean> render = register(new Setting<Boolean>("Render", true));

    private static ObiAssist instance;

    public ObiAssist() {
        super("ObiAssist", "place obsidian to support your autocrystal", Category.COMBAT, true, false, false);
        instance = this;
    }

    /*
     * this is kinda chinese
     */

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

        //switch shit kinda messy
        if (NekoAura.getInstance().isEnabled() && NekoAura.getInstance().currentTarget != null) {
            if (slot != -1) {
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
        try {

            float range = NekoAura.getInstance().placeRange.getValue();

            float targetDMG = 0.0f;
            float minDmg = NekoAura.getInstance().minMinDmg.getValue();

            BlockPos targetPos = null;

            for (BlockPos pos : BlockUtil.getSphere(PlayerUtil.getPlayerPosFloored(), range, (int) range, false, true, 0)) {
                BlockUtil.ValidResult result = BlockUtil.valid(pos);

                if (result != BlockUtil.ValidResult.Ok)
                    continue;

                if (!CanPlaceCrystalIfObbyWasAtPos(pos))
                    continue;

                float tempDMG = DamageUtil.calculateDamage(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, target);


                if (tempDMG < minDmg)
                    continue;

                if (tempDMG >= targetDMG) {
                    targetPos = pos;
                    targetDMG = tempDMG;
                }
            }

            if (targetPos != null) {
                BlockUtil.placeBlock(targetPos);
            }
            if (packet.getValue()) {
                if (slot != -1) {
                    mc.player.connection.sendPacket(new CPacketHeldItemChange(old));
                    if (packethand.getValue() && hand != null) {
                        mc.player.setActiveHand(hand);
                    }
                }
            }

        }catch (NullPointerException ignored) {
            //to avoid ticking entity crash
        }
    }

    public static boolean CanPlaceCrystalIfObbyWasAtPos(final BlockPos pos) {

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
