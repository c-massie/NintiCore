package scot.massie.mc.ninti.core;

import scot.massie.lib.events.InvokablePriorityEvent;
import scot.massie.lib.events.OrderedEvent;
import scot.massie.lib.events.PriorityEvent;
import scot.massie.lib.events.ProtectedPriorityEvent;
import scot.massie.lib.events.args.EventArgs;

/**
 * Statically accessible events representing server lifecycle events for plugins/mods.
 */
public final class PluginEvents
{
    /**
     * Event args for when plugin/mod data is loaded.
     */
    public static final class DataLoadEventArgs implements EventArgs
    { }

    /**
     * Event args for when plugin/mod data is saved.
     */
    public static final class DataSaveEventArgs implements EventArgs
    { }

    private PluginEvents()
    {}

    static final InvokablePriorityEvent<DataLoadEventArgs> onDataLoaded_internal = new OrderedEvent<>();
    static final InvokablePriorityEvent<DataSaveEventArgs> onDataSaved_internal = new OrderedEvent<>();

    /**
     * Single event for the loading of plugins/mods dependant on this library. Triggered by users or by the loading of
     * the default world.
     */
    public static final PriorityEvent<DataLoadEventArgs> onDataLoaded
            = new ProtectedPriorityEvent<>(onDataLoaded_internal);

    /**
     * Single event for the saving of plugins/mods dependant on this library. Triggered by users or by the saving of
     * the default world.
     */
    public static final PriorityEvent<DataSaveEventArgs> onDataSaved
            = new ProtectedPriorityEvent<>(onDataSaved_internal);
}
