package scot.massie.mc.ninti.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static scot.massie.mc.ninti.core.StaticUtilFunctions.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("ninticore")
public class NintiCore
{
    public static final String PERMISSION_ROOT              = "ninti.permissions";
    public static final String PERMISSION_READ              = "ninti.permissions.read";
    public static final String PERMISSION_READ_PLAYERS      = "ninti.permissions.read.players";
    public static final String PERMISSION_READ_GROUPS       = "ninti.permissions.read.groups";
    public static final String PERMISSION_WRITE             = "ninti.permissions.write";
    public static final String PERMISSION_WRITE_PLAYERS     = "ninti.permissions.write.players";
    public static final String PERMISSION_WRITE_GROUPS      = "ninti.permissions.write.groups";
    public static final String PERMISSION_FILEHANDLING_SAVE = "ninti.permissions.files.save";
    public static final String PERMISSION_FILEHANDLING_LOAD = "ninti.permissions.files.load";

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public NintiCore()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        Permissions.Presets.addPermission(Permissions.Presets.ADMIN,  PERMISSION_ROOT);
        Permissions.Presets.addPermission(Permissions.Presets.MOD,    PERMISSION_READ);
        Permissions.Presets.addPermission(Permissions.Presets.PLAYER, PERMISSION_READ_GROUPS);

        Permissions.Suggestions.add(PERMISSION_READ_PLAYERS,
                                    PERMISSION_READ_GROUPS,
                                    PERMISSION_WRITE_PLAYERS,
                                    PERMISSION_WRITE_GROUPS,
                                    PERMISSION_FILEHANDLING_SAVE,
                                    PERMISSION_FILEHANDLING_LOAD);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        Permissions.load();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event)
    {
        // do something when the server starts
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event)
    { event.getDispatcher().register(PermissionsCommandHandler.permissionCommand); }

    @SubscribeEvent
    public void onSave(final WorldEvent.Save worldSaveEvent)
    {
        if(!(getWorldId(worldSaveEvent).equals("minecraft:overworld")))
            return;

        Permissions.save();
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {

    }
}
