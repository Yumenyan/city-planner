package jp.citybuilder;

import jp.citybuilder.job.JobManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CityBuilderMod implements ModInitializer {
    public static final String MOD_ID = "citybuilder";
    public static final Logger LOG = LoggerFactory.getLogger("CityBuilder");

    private static CityConfig config = new CityConfig();
    private static BuildingCatalog catalog;
    private static JobManager jobs = new JobManager();

    public static CityConfig config() { return config; }
    public static BuildingCatalog catalog() { return catalog; }
    public static JobManager jobs() { return jobs; }

    /** config.json と建物カタログを読み直す */
    public static void reload() {
        config = CityConfig.load();
        catalog = BuildingCatalog.load();
    }

    @Override
    public void onInitialize() {
        reload();
        ServerNet.register();
        CityCommands.register();
        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> jobs.tick(server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ServerNet.sendCatalog(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> jobs = new JobManager());
    }
}
