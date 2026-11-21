package com.dentomas.deathgoddess.world;

import com.dentomas.deathgoddess.DeathGoddessMod;
import com.dentomas.deathgoddess.GoddessConfig;
import com.dentomas.deathgoddess.entity.GoddessOfDeath;
import com.dentomas.deathgoddess.entity.OmenEye;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Counts the Lords that players kill and, once enough have fallen, now and then opens an eye above
 * a player who is not expecting it.
 */
@Mod.EventBusSubscriber(modid = DeathGoddessMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WatchDirector {
    public static final TagKey<EntityType<?>> LORDS = TagKey.create(Registry.ENTITY_TYPE_REGISTRY, DeathGoddessMod.id("lords"));
    private static final int RETRY_TICKS = 300;
    /** Server tick at which the next appearance is attempted; negative until first scheduled. */
    private static long nextAttempt = -1L;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        nextAttempt = -1L;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.level.isClientSide && victim.getType().is(LORDS)) {
            lordFell(victim, event.getSource().getEntity() instanceof Player);
        }
    }

    /**
     * Some bosses (the Ender Dragon among them) play out a death of their own and never die the
     * ordinary way, so a Lord leaving the world as killed counts too. Recording twice is harmless.
     */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide() && entity instanceof LivingEntity victim && victim.getType().is(LORDS)
                && victim.getRemovalReason() == Entity.RemovalReason.KILLED) {
            lordFell(victim, false);
        }
    }

    private static void lordFell(LivingEntity victim, boolean killedByPlayer) {
        MinecraftServer server = victim.getServer();
        // Bosses often die to something indirect, so a player close by is credit enough.
        boolean byPlayers = killedByPlayer || victim.getKillCredit() instanceof Player
                || victim.level.getNearestPlayer(victim, 64.0D) != null;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (server == null || !byPlayers || id == null) {
            return;
        }
        LordLedger ledger = LordLedger.get(server);
        if (ledger.record(id.toString()) && ledger.count() == GoddessConfig.LORDS_REQUIRED.get()) {
            // She has just taken notice: the first look comes sooner than the usual wait.
            schedule(server, 0.4D);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !GoddessConfig.WATCH_ENABLED.get()) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();
        if (nextAttempt < 0L) {
            schedule(server, 1.0D);
        }
        if (now < nextAttempt) {
            return;
        }
        if (LordLedger.get(server).count() < GoddessConfig.LORDS_REQUIRED.get()) {
            nextAttempt = now + 1200L;
            return;
        }
        List<ServerPlayer> unsuspecting = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isUnsuspecting(player)) {
                unsuspecting.add(player);
            }
        }
        if (unsuspecting.isEmpty()) {
            // Everyone is busy or on guard: wait for a quiet moment rather than skip the visit.
            nextAttempt = now + RETRY_TICKS;
            return;
        }
        RandomSource random = server.overworld().random;
        OmenEye.open(unsuspecting.get(random.nextInt(unsuspecting.size())));
        schedule(server, 1.0D);
    }

    private static void schedule(MinecraftServer server, double scale) {
        int min = GoddessConfig.WATCH_MIN_MINUTES.get();
        int max = Math.max(min, GoddessConfig.WATCH_MAX_MINUTES.get());
        double minutes = min + server.overworld().random.nextDouble() * (max - min);
        nextAttempt = server.getTickCount() + (long) (minutes * scale * 1200.0D);
    }

    /** True for a player who is alive, settled and not in a fight: the moment they least expect it. */
    private static boolean isUnsuspecting(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.isSleeping()) {
            return false;
        }
        int calm = GoddessConfig.CALM_SECONDS.get() * 20;
        int age = player.tickCount;
        if (age < Math.max(calm, 600) || player.getHealth() < player.getMaxHealth() * 0.5F) {
            return false;
        }
        if (age - player.getLastHurtByMobTimestamp() < calm || age - player.getLastHurtMobTimestamp() < calm) {
            return false;
        }
        if (!player.level.getEntitiesOfClass(OmenEye.class, player.getBoundingBox().inflate(32.0D)).isEmpty()
                || !player.level.getEntitiesOfClass(GoddessOfDeath.class, player.getBoundingBox().inflate(64.0D)).isEmpty()) {
            return false;
        }
        return player.level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12.0D),
                mob -> mob.getTarget() == player).isEmpty();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deathgoddess").requires(source -> source.hasPermission(2))
                .then(Commands.literal("watch")
                        .executes(context -> watch(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> watch(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("lords")
                        .executes(context -> listLords(context.getSource()))
                        .then(Commands.literal("set").then(Commands.argument("count", IntegerArgumentType.integer(0, 100))
                                .executes(context -> setLords(context.getSource(), IntegerArgumentType.getInteger(context, "count")))))));
    }

    private static int watch(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            OmenEye.open(player);
        }
        source.sendSuccess(Component.translatable("commands.deathgoddess.watch", players.size()), true);
        return players.size();
    }

    private static int listLords(CommandSourceStack source) {
        LordLedger ledger = LordLedger.get(source.getServer());
        source.sendSuccess(Component.translatable("commands.deathgoddess.lords", ledger.count(),
                GoddessConfig.LORDS_REQUIRED.get(), String.join(", ", ledger.slain())), false);
        return ledger.count();
    }

    /** For testing: replaces the record with the given number of placeholder Lords. */
    private static int setLords(CommandSourceStack source, int count) {
        LordLedger ledger = LordLedger.get(source.getServer());
        ledger.clear();
        for (int i = 1; i <= count; i++) {
            ledger.record("debug:lord_" + i);
        }
        return listLords(source);
    }

    private WatchDirector() {
    }
}
