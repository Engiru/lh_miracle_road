package dev.lhkongyu.lhmiracleroad.event;

import dev.lhkongyu.lhmiracleroad.LHMiracleRoad;
import dev.lhkongyu.lhmiracleroad.capability.PlayerOccupationAttribute;
import dev.lhkongyu.lhmiracleroad.capability.PlayerOccupationAttributeProvider;
import dev.lhkongyu.lhmiracleroad.tool.LHMiracleRoadTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = LHMiracleRoad.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CuriosWatcherEvent {

    /**
     * Lightweight server tick watcher: on each player tick (END phase), compare the Curios inventory signature.
     * If it changed, recompute punishment state so Curios-only slots (e.g., spellbook) enforce requirements.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        Optional<String> sigOpt = LHMiracleRoadTool.getCuriosSignature(player);
        // Curios not loaded or unavailable: do nothing
        if (sigOpt.isEmpty()) return;
        boolean changed = false;
        String newSig = sigOpt.get();
        String oldSig = LHMiracleRoadTool.CURIOS_SIGNATURES.get(player.getUUID());
        if (oldSig == null || !oldSig.equals(newSig)) {
            LHMiracleRoadTool.CURIOS_SIGNATURES.put(player.getUUID(), newSig);
            changed = true;
        }

        // Periodic fallback (every 10 ticks ~0.5s) to ensure reapplication after other attribute changes
        boolean shouldSweep = changed || LHMiracleRoadTool.curiosPeriodicTick(player, 10);
        if (!shouldSweep) return;

        Optional<PlayerOccupationAttribute> opt = player.getCapability(PlayerOccupationAttributeProvider.PLAYER_OCCUPATION_ATTRIBUTE_PROVIDER).resolve();
        if (opt.isEmpty()) return;
        LHMiracleRoadTool.playerPunishmentStateUpdate(player, opt.get());
    }
}
