package addon.fmod.volytra;

import addon.fmod.modules.VolytraFly;
import addon.fmod.volytra.autonav.ButtonSetting;
import addon.fmod.volytra.hud.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Modules;

public final class VolytraIntegration {
    private VolytraIntegration() {}

    public static void register() {
        SettingsWidgetFactory.registerCustomFactory(ButtonSetting.class, theme -> (table, setting) -> {
            ButtonSetting button = (ButtonSetting) setting;
            var widget = table.add(theme.button(button.buttonText)).expandCellX().widget();
            widget.action = () -> button.action.run();
        });
        VolytraFly module = Modules.get().get(VolytraFly.class);
        module.autonavGo.action = module::goToTypedCoords;
        Hud.get().register(SpeedometerHud.INFO);
        Hud.get().register(ModeLightsHud.INFO);
        Hud.get().register(VolytraAssistantHud.INFO);
        Hud.get().register(ElytraFuelHud.INFO);
        MeteorClient.EVENT_BUS.subscribe(new FutureSightVisual());
        MeteorClient.EVENT_BUS.subscribe(new AutonavRouteVisual());
    }
}
