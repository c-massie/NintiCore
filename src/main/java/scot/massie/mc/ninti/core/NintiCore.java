package scot.massie.mc.ninti.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import scot.massie.mc.ninti.core.currencies.Currencies;
import scot.massie.mc.ninti.core.currencies.Currency;
import scot.massie.mc.ninti.core.zones.Zones;
import scot.massie.mc.ninti.core.zones.ZonesCommandHandler;

import java.nio.file.Path;

import static scot.massie.mc.ninti.core.PluginUtils.*;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("ninticore")
public class NintiCore
{
    public static final String PERMISSION_PERMISSIONS_ROOT              = "ninti.permissions";
    public static final String PERMISSION_PERMISSIONS_READ              = "ninti.permissions.read";
    public static final String PERMISSION_PERMISSIONS_READ_PLAYERS      = "ninti.permissions.read.players";
    public static final String PERMISSION_PERMISSIONS_READ_GROUPS       = "ninti.permissions.read.groups";
    public static final String PERMISSION_PERMISSIONS_WRITE             = "ninti.permissions.write";
    public static final String PERMISSION_PERMISSIONS_WRITE_PLAYERS     = "ninti.permissions.write.players";
    public static final String PERMISSION_PERMISSIONS_WRITE_GROUPS      = "ninti.permissions.write.groups";
    public static final String PERMISSION_PERMISSIONS_FILEHANDLING_SAVE = "ninti.permissions.files.save";
    public static final String PERMISSION_PERMISSIONS_FILEHANDLING_LOAD = "ninti.permissions.files.load";

    public static final String PERMISSION_ZONES_ROOT                    = "ninti.zones";
    public static final String PERMISSION_ZONES_READ                    = "ninti.zones.read";
    public static final String PERMISSION_ZONES_WRITE                   = "ninti.zones.write";
    public static final String PERMISSION_ZONES_WRITE_CREATE            = "ninti.zones.write.create";
    public static final String PERMISSION_ZONES_WRITE_DELETE            = "ninti.zones.write.delete";
    public static final String PERMISSION_ZONES_WRITE_RENAME            = "ninti.zones.write.rename";
    public static final String PERMISSION_ZONES_WRITE_MODIFY_ADDTO      = "ninti.zones.write.modify.addto";
    public static final String PERMISSION_ZONES_WRITE_MODIFY_REMOVEFROM = "ninti.zones.write.modify.removefrom";
    public static final String PERMISSION_ZONES_FILEHANDLING_SAVE       = "ninti.zones.files.save";
    public static final String PERMISSION_ZONES_FILEHANDLING_LOAD       = "ninti.zones.files.load";

    public static final Path DATA_FOLDER = PluginUtils.getPluginDataFolder().resolve("ninti");

    public NintiCore()
    {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        //noinspection ThisEscapedInObjectConstruction
        MinecraftForge.EVENT_BUS.register(this);

        PluginEvents.onDataLoaded.register(args ->
        {
            Permissions.load();
            Zones.load();
        });

        PluginEvents.onDataSaved.register(args ->
        {
            Permissions.save();
            Zones.save();
        });
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        Permissions.Presets.addPermission(Permissions.Presets.ADMIN, PERMISSION_PERMISSIONS_ROOT);
        Permissions.Presets.addPermission(Permissions.Presets.ADMIN, PERMISSION_ZONES_ROOT);
        Permissions.Presets.addPermission(Permissions.Presets.MOD, PERMISSION_PERMISSIONS_READ);
        Permissions.Presets.addPermission(Permissions.Presets.MOD, PERMISSION_ZONES_ROOT);
        Permissions.Presets.addPermission(Permissions.Presets.PLAYER, PERMISSION_PERMISSIONS_READ_GROUPS);
        Permissions.Presets.addPermission(Permissions.Presets.PLAYER, PERMISSION_ZONES_READ);

        Permissions.Suggestions.add(PERMISSION_PERMISSIONS_READ_PLAYERS,
                                    PERMISSION_PERMISSIONS_READ_GROUPS,
                                    PERMISSION_PERMISSIONS_WRITE_PLAYERS,
                                    PERMISSION_PERMISSIONS_WRITE_GROUPS,
                                    PERMISSION_PERMISSIONS_FILEHANDLING_SAVE,
                                    PERMISSION_PERMISSIONS_FILEHANDLING_LOAD,
                                    PERMISSION_ZONES_READ,
                                    PERMISSION_ZONES_WRITE_CREATE,
                                    PERMISSION_ZONES_WRITE_DELETE,
                                    PERMISSION_ZONES_WRITE_RENAME,
                                    PERMISSION_ZONES_WRITE_MODIFY_ADDTO,
                                    PERMISSION_ZONES_WRITE_MODIFY_REMOVEFROM,
                                    PERMISSION_ZONES_FILEHANDLING_SAVE,
                                    PERMISSION_ZONES_FILEHANDLING_LOAD);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStarting(FMLServerStartingEvent event)
    {
        minecraftServer = event.getServer();
        Currencies.register(Currency.EXPERIENCE);
        Currencies.register(Currency.LEVELS);
        PluginEvents.onDataLoaded_internal.invoke(new PluginEvents.DataLoadEventArgs());
        // do something when the server starts
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(PermissionsCommandHandler.permissionCommand);
        event.getDispatcher().register(ZonesCommandHandler      .zonesCommand);
    }

    @SubscribeEvent
    public void onSave(final WorldEvent.Save worldSaveEvent)
    {
        if(!(getWorldId(worldSaveEvent).equals(getDefaultWorldId())))
            return;

        PluginEvents.onDataSaved_internal.invoke(new PluginEvents.DataSaveEventArgs());
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents
    {

    }
}
