package addon.fmod.modules;

import addon.fmod.FMod;
import addon.fmod.features.containers.ContainerBrowser;
import addon.fmod.features.containers.ContainerTracker;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

/** Original FMod implementation of the requested indexing behavior; does not import the unlicensed JEFF source. */
public final class ChestIndex extends Module {
    private final Setting<Double> range = settings.getDefaultGroup().add(new DoubleSetting.Builder().name("range")
        .description("Automatically inspect reachable received containers. Never exceeds normal interaction reach.").defaultValue(4).range(1,6).sliderRange(1,6).build());
    public ChestIndex() { super(FMod.CATEGORY,"chest-index","Indexes reachable container contents and nested shulkers into ChestTracker's private catalog."); }
    public double range() { return range.get(); }
    @Override public void onActivate() { ContainerTracker.INSTANCE.rescan(); }
    @Override public void onDeactivate() { ContainerTracker.INSTANCE.flush(); }
    @Override public String getInfoString() { return ContainerTracker.INSTANCE.records().size() + " indexed"; }
    @Override public WWidget getWidget(GuiTheme theme) {
        var list=theme.verticalList();
        list.add(theme.label("One shared scanner. Pauses for StashMover and user-opened screens.",400));
        list.add(theme.button("Browse / search indexed contents")).widget().action=ContainerBrowser::open;
        list.add(theme.button("Rescan reachable containers")).widget().action=ContainerTracker.INSTANCE::rescan;
        return list;
    }
}
