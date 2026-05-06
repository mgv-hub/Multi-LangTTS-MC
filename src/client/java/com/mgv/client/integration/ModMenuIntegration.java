package com.mgv.client.integration;

import com.mgv.client.CoreModClient;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// ModMenuIntegration: bridges ModMenu's config button to our ClothConfig screen
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // delegate to CoreModClient to ensure config reloads fresh before showing UI
        return parent -> {
            CoreModClient.openConfigScreen();
            // return null: CoreModClient already sets the screen via MinecraftClient.setScreen()
            return null;
        };
    }
}