package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightCompatDistContractTest {
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    void epicFightClientApiVerificationIsClientGated() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/HotBath.java");
        int clientApi = source.indexOf("yesman.epicfight.client.renderer.FirstPersonRenderer");
        int registration = source.lastIndexOf("CompatManager.registerCompat(", clientApi);
        int clientGate = source.lastIndexOf("if (FMLEnvironment.dist == Dist.CLIENT)", registration);

        assertTrue(clientApi >= 0, "Epic Fight client API classes should stay explicit for client verification");
        assertTrue(registration >= 0, "Epic Fight compat should be registered through CompatManager");
        assertTrue(clientGate >= 0, "Epic Fight compat registration must be guarded by client dist");
        assertTrue(clientGate < registration, "Dedicated servers must skip Epic Fight registration before API verification");
        assertTrue(registration - clientGate < 300, "The nearest client dist guard should wrap the Epic Fight registration");
        assertTrue(registration < clientApi, "Epic Fight client API names should only appear inside the guarded registration");
    }

    @Test
    void epicFightInitStillHasServerSideNoopGuard() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/compat/EpicFightCompat.java");
        assertTrue(source.contains("!FMLEnvironment.dist.isClient()"),
                "EpicFightCompat.init should remain a no-op on dedicated servers");
    }
}
