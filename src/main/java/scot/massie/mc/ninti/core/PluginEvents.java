package scot.massie.mc.ninti.core;

import scot.massie.lib.events.InvokablePriorityEvent;
import scot.massie.lib.events.OrderedEvent;
import scot.massie.lib.events.PriorityEvent;
import scot.massie.lib.events.ProtectedPriorityEvent;
import scot.massie.lib.events.args.EventArgs;

public final class PluginEvents
{
    public static final class DataLoadEventArgs implements EventArgs
    {

    }

    public static final class DataSaveEventArgs implements EventArgs
    {

    }

    private PluginEvents()
    {}

    static final InvokablePriorityEvent<DataLoadEventArgs> onDataLoaded_internal = new OrderedEvent<>();
    static final InvokablePriorityEvent<DataSaveEventArgs> onDataSaved_internal = new OrderedEvent<>();

    public static final PriorityEvent<DataLoadEventArgs> onDataLoaded
            = new ProtectedPriorityEvent<>(onDataLoaded_internal);

    public static final PriorityEvent<DataSaveEventArgs> onDataSaved
            = new ProtectedPriorityEvent<>(onDataSaved_internal);
}
