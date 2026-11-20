package com.dentomas.deathgoddess.item;

import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.dentomas.deathgoddess.registry.ModEntities;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Placed on the ground to call the Goddess of Death. */
public class MourningEffigyItem extends Item {
    public MourningEffigyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            notify(player, "item.deathgoddess.mourning_effigy.peaceful");
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        GoddessOfDeath goddess = ModEntities.GODDESS.get().create(server);
        if (goddess == null) {
            return InteractionResult.FAIL;
        }
        float yaw = player == null ? 0.0F : Mth.wrapDegrees(player.getYRot() + 180.0F);
        goddess.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        goddess.yBodyRot = yaw;
        goddess.yHeadRot = yaw;
        if (!server.noCollision(goddess)) {
            notify(player, "item.deathgoddess.mourning_effigy.no_room");
            return InteractionResult.FAIL;
        }
        goddess.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
        server.addFreshEntity(goddess);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    private static void notify(@Nullable Player player, String key) {
        if (player != null) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.deathgoddess.mourning_effigy.tooltip").withStyle(ChatFormatting.DARK_RED));
    }
}
