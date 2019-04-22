package de.guntram.mcmod.fabrictools;

import net.fabricmc.api.ClientModInitializer;

public class DummyMain implements ClientModInitializer {
    // Fabric loader 0.4.1 had a bug where mixins didn't find their target
    // when mods didn't have any entry points defined, so let's give it
    // a definition.
    @Override
    public void onInitializeClient() {
        
    }
}
