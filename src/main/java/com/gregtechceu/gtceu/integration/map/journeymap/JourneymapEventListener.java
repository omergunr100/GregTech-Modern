package com.gregtechceu.gtceu.integration.map.journeymap;

import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.map.ButtonState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import journeymap.client.api.display.IThemeButton;
import journeymap.client.api.event.forge.FullscreenDisplayEvent;

import java.util.ArrayList;

public class JourneymapEventListener {

    public static void init() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(JourneymapEventListener::onFullscreenAddonButton);
    }

    @OnlyIn(Dist.CLIENT)
    protected static void onFullscreenAddonButton(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        if (!ConfigHolder.INSTANCE.compat.minimap.toggle.journeyMapIntegration) {
            return;
        }
        var display = event.getThemeButtonDisplay();
        var buttons = new ArrayList<IThemeButton>(ButtonState.getAllButtons().size());
        for (var state : ButtonState.getAllButtons()) {
            buttons.add(display.addThemeToggleButton("gtceu.button." + state.name, state.name, state.enabled,
                    b -> {
                        ButtonState.toggleButton(state);
                        buttons.stream().filter(btn -> btn.getToggled() || btn == b).forEach(IThemeButton::toggle);
                    }));
        }
    }
}
