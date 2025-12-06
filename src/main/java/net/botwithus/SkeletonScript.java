package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;

import java.util.Random;

public class SkeletonScript extends LoopingScript {

    // NPC and Object IDs
    private static final int[] PLATFORM_IDS = {127315, 127316, 127314, 129034, 129033, 129032};
    private static final int WANDERING_SOUL = 30493;
    private static final int SHAMBLING_HORROR = 30494;
    private static final int MOTH = 30419;
    private static final int SOUL_STORM_1 = 30498;
    private static final int SOUL_STORM_2 = 30499;
    private static final int SPARKLING_GLYPH = 30492;
    private static final int CORRUPT_GLYPH_1 = 30495;
    private static final int CORRUPT_GLYPH_2 = 30496;
    private static final int CORRUPT_GLYPH_3 = 30497;

    private BotState botState = BotState.IDLE;
    private boolean someBool = true;
    private Random random = new Random();
    private boolean repairCheck = false;
    private int repairFail = 0;
    private boolean soulActive = false;
    private boolean soulDismissed = false;
    private boolean stormActive = false;
    private boolean stormBanished = false;
    private boolean sparklingActive = false;
    private boolean sparklingExpelled = false;
    private boolean corruptActive = false;
    private boolean ritualCompleted = false;
    private boolean needsRepair = false;
    private boolean ritualStarted = false;
    private int startAttempts = 0;

    enum BotState {
        //define your own states here
        IDLE,
        SKILLING,
        BANKING,
        //...
    }

    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        setupChatListener();
    }

    private void setupChatListener() {
        subscribe(ChatMessageEvent.class, chatMessageEvent -> {
            String message = chatMessageEvent.getMessage();
            
            // Log all chat messages
            println("[CHAT] " + message);
            
            // Check if message contains the red color code
            if (message.contains("<col=EB2F2F>")) {
                
                if (message.contains("necroplasm for this ritual")) {
                    // Ritual complete - stop script
                    println("Script stopped - ritual complete");
                    botState = BotState.IDLE;
                }
                
                if (message.contains("durability of 1")) {
                    repairCheck = true;
                }
                
                if (message.contains("have the materials to repair the following") || 
                    message.contains("need the following materials to repair")) {
                    // Materials check - could add logic here if needed
                }
                
                if (message.contains("nothing to repair")) {
                    repairFail++;
                }
            }
            
            // Check for wandering soul appearance
            if (message.contains("A wandering soul has appeared on the ritual site")) {
                soulActive = true;
                soulDismissed = false;
                println("Wandering soul appeared!");
            }
            
            // Check for wandering soul dismissal
            if (message.contains("You dismiss the wandering soul from the ritual site")) {
                soulDismissed = true;
                println("Wandering soul dismissed successfully");
            }
            
            // Check for wandering soul timeout
            if (message.contains("The wandering soul abandons the ritual site")) {
                soulActive = false;
                println("Wandering soul timed out");
            }
            
            // Check for storm of souls start
            if (message.contains("A storm of souls has taken over a ritual component")) {
                stormActive = true;
                stormBanished = false;
                println("Storm of souls detected!");
            }
            
            // Check for storm of souls end
            if (message.contains("You banish the storm of souls from the ritual component")) {
                stormBanished = true;
                println("Storm of souls banished successfully");
            }
            
            // Check for sparkling glyph start
            if (message.contains("A cloud of sparkles has taken over a glyph")) {
                sparklingActive = true;
                sparklingExpelled = false;
                println("Sparkling glyph detected!");
            }
            
            // Check for sparkling glyph end
            if (message.contains("You expel the sparkles from the glyph")) {
                sparklingExpelled = true;
                println("Sparkles expelled successfully");
            }
            
            // Check for corrupt glyphs start
            if (message.contains("Some corrupt glyphs have appeared on the ritual site")) {
                corruptActive = true;
                println("Corrupt glyphs detected!");
            }
            
            // Check for corrupt glyphs end
            if (message.contains("You successfully deactivate all of the corrupt glyphs")) {
                corruptActive = false;
                println("All corrupt glyphs deactivated successfully");
            }
            
            // Check for ritual completion
            if (message.contains("You complete the ritual")) {
                ritualCompleted = true;
                println("Ritual completed! Restarting...");
            }
            
            // Check for durability requirement
            if (message.contains("Your components do not all have the required durability of 1 to start this ritual")) {
                needsRepair = true;
                startAttempts++;
                println("Components need repair! Attempt " + startAttempts + "/3");
                
                // Stop script after 3 failed attempts
                if (startAttempts >= 3) {
                    println("Failed to start ritual after 3 attempts - stopping script");
                    botState = BotState.IDLE;
                }
            }
            
            // Check for successful ritual start (any message indicating ritual is active)
            if (message.contains("You start") || message.contains("ritual") && !message.contains("durability")) {
                ritualStarted = true;
                startAttempts = 0; // Reset attempts on success
            }
        });
    }

    // Continue ritual on platform after disturbance
    private void continueRitual() {
        SceneObject platform = SceneObjectQuery.newQuery().ids(PLATFORM_IDS).option("Continue ritual").results().nearest();
        
        if (platform != null) {
            println("Continuing ritual on platform...");
            if (platform.interact("Continue ritual")) {
                Execution.delay(random.nextLong(1200, 1800));
            }
        }
    }

    // Repair all components on pedestal
    private boolean repairComponents() {
        if (!needsRepair) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        // Don't interact if player is moving
        if (player.isMoving()) {
            println("Player is moving, waiting to repair...");
            return false;
        }
        
        SceneObject pedestal = SceneObjectQuery.newQuery().name("Pedestal").option("Repair all").results().nearest();
        
        if (pedestal != null) {
            println("Repairing all components on pedestal...");
            if (pedestal.interact("Repair all")) {
                needsRepair = false;
                Execution.delay(random.nextLong(1800, 2400));
                return true;
            }
        } else {
            println("Pedestal not found for repair");
            needsRepair = false;
        }
        
        return false;
    }

    // Start ritual on platform after completion
    private boolean startRitual() {
        if (!ritualCompleted) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        // Don't interact if player is moving
        if (player.isMoving()) {
            println("Player is moving, waiting to start ritual...");
            return false;
        }
        
        SceneObject platform = SceneObjectQuery.newQuery().ids(PLATFORM_IDS).option("Start ritual").results().nearest();
        
        if (platform != null) {
            println("Starting new ritual on platform... (Attempt " + (startAttempts + 1) + ")");
            
            // Reset flags
            ritualStarted = false;
            needsRepair = false;
            
            if (platform.interact("Start ritual")) {
                // Wait up to 3 seconds (5 ticks) for an error response
                int ticks = 0;
                while (!needsRepair && ticks < 5) {
                    Execution.delay(600); // One game tick
                    ticks++;
                }
                
                // If needs repair, it will be handled in next loop
                if (needsRepair) {
                    println("Repair needed, will attempt repair...");
                    return true;
                }
                
                // No error message after 3 seconds - assume success
                ritualCompleted = false;
                startAttempts = 0;
                println("Ritual started successfully (no error detected)!");
                return true;
            }
        } else {
            println("Platform not found for starting ritual");
        }
        
        return false;
    }

    // Scan for visible disturbance NPCs and return the first one found
    private Npc scanForDisturbances() {
        // Check for Wandering Soul
        Npc soul = NpcQuery.newQuery().id(WANDERING_SOUL).results().nearest();
        if (soul != null) return soul;
        
        // Check for Shambling Horror
        Npc horror = NpcQuery.newQuery().id(SHAMBLING_HORROR).results().nearest();
        if (horror != null) return horror;
        
        // Check for Moth
        Npc moth = NpcQuery.newQuery().id(MOTH).results().nearest();
        if (moth != null) return moth;
        
        // No disturbance NPCs found
        return null;
    }

    // Dismiss wandering soul and wait for confirmation (tick-based)
    private boolean dismissWanderingSoul() {
        if (!soulActive) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        println("Handling wandering soul...");
        
        Npc soul = NpcQuery.newQuery().id(WANDERING_SOUL).results().nearest();
        
        if (soul == null) {
            println("No wandering soul NPC found");
            soulActive = false;
            return false;
        }
        
        // Don't interact if player is moving
        if (player.isMoving()) {
            println("Player is moving, waiting...");
            return false;
        }
        
        // Reset the flag before interaction
        soulDismissed = false;
        
        // Interact with the soul
        if (soul.interact("Dismiss")) {
            println("Clicked dismiss on wandering soul");
            
            // Wait for the chat message confirmation (max 10 ticks = 6 seconds)
            int ticks = 0;
            while (!soulDismissed && ticks < 10) {
                Execution.delay(600); // One game tick
                ticks++;
            }
            
            if (soulDismissed) {
                soulActive = false;
                continueRitual();
                return true;
            } else {
                println("Timeout waiting for soul dismissal confirmation");
                soulActive = false;
                return false;
            }
        } else {
            println("Failed to interact with wandering soul");
            return false;
        }
    }

    // Handle storm of souls disturbance
    private boolean handleStorm() {
        if (!stormActive) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        println("Handling storm of souls...");
        
        // Find storm NPC by type (30498 or 30499)
        Npc stormNpc = NpcQuery.newQuery().byType(SOUL_STORM_1).results().nearest();
        if (stormNpc == null) {
            stormNpc = NpcQuery.newQuery().byType(SOUL_STORM_2).results().nearest();
        }
        
        if (stormNpc == null) {
            println("No storm NPC found");
            stormActive = false;
            return false;
        }
        
        // Don't interact if player is moving
        if (player.isMoving()) {
            println("Player is moving, waiting...");
            return false;
        }
        
        // Interact with the storm
        if (stormNpc.interact("Dissipate")) {
            println("Clicked dissipate on storm");
            
            // Wait for the chat message confirmation (max 15 ticks = 9 seconds)
            int ticks = 0;
            while (!stormBanished && ticks < 15) {
                Execution.delay(600); // One game tick
                ticks++;
                
                // Keep clicking dissipate if storm is still active
                if (ticks % 3 == 0 && !stormBanished) {
                    stormNpc = NpcQuery.newQuery().byType(SOUL_STORM_1).results().nearest();
                    if (stormNpc == null) {
                        stormNpc = NpcQuery.newQuery().byType(SOUL_STORM_2).results().nearest();
                    }
                    if (stormNpc != null && !player.isMoving()) {
                        stormNpc.interact("Dissipate");
                        println("Continuing to dissipate storm...");
                    }
                }
            }
            
            if (stormBanished) {
                stormActive = false;
                continueRitual();
                return true;
            } else {
                println("Timeout waiting for storm banishment");
                stormActive = false;
                return false;
            }
        } else {
            println("Failed to interact with storm");
            return false;
        }
    }

    // Handle sparkling glyph disturbance
    private boolean handleSparkling() {
        if (!sparklingActive) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        println("Handling sparkling glyph...");
        
        // Find sparkling glyph NPC by type (30492)
        Npc sparklingNpc = NpcQuery.newQuery().byType(SPARKLING_GLYPH).results().nearest();
        
        if (sparklingNpc == null) {
            println("No sparkling glyph NPC found");
            sparklingActive = false;
            return false;
        }
        
        // Don't interact if player is moving
        if (player.isMoving()) {
            println("Player is moving, waiting...");
            return false;
        }
        
        // Interact with the sparkling glyph
        if (sparklingNpc.interact("Restore")) {
            println("Clicked restore on sparkling glyph");
            
            // Wait for the chat message confirmation (max 10 ticks = 6 seconds)
            int ticks = 0;
            while (!sparklingExpelled && ticks < 10) {
                Execution.delay(600); // One game tick
                ticks++;
            }
            
            if (sparklingExpelled) {
                sparklingActive = false;
                continueRitual();
                return true;
            } else {
                println("Timeout waiting for sparkles expulsion");
                sparklingActive = false;
                return false;
            }
        } else {
            println("Failed to interact with sparkling glyph");
            return false;
        }
    }

    // Handle corrupt glyphs disturbance
    private boolean handleCorruptGlyphs() {
        if (!corruptActive) {
            return false;
        }
        
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null) {
            return false;
        }
        
        println("Handling corrupt glyphs...");
        
        // Keep deactivating corrupt glyphs until none are found
        while (corruptActive) {
            // Find corrupt glyph NPC by type (30495, 30496, or 30497)
            Npc corruptNpc = NpcQuery.newQuery().byType(CORRUPT_GLYPH_1).results().nearest();
            if (corruptNpc == null) {
                corruptNpc = NpcQuery.newQuery().byType(CORRUPT_GLYPH_2).results().nearest();
            }
            if (corruptNpc == null) {
                corruptNpc = NpcQuery.newQuery().byType(CORRUPT_GLYPH_3).results().nearest();
            }
            
            if (corruptNpc == null) {
                println("No more corrupt glyphs found");
                corruptActive = false;
                continueRitual();
                return true;
            }
            
            // Don't interact if player is moving
            if (player.isMoving()) {
                println("Player is moving, waiting...");
                Execution.delay(600);
                continue;
            }
            
            // Interact with the corrupt glyph
            if (corruptNpc.interact("Deactivate")) {
                println("Clicked deactivate on corrupt glyph");
                Execution.delay(random.nextLong(800, 1200));
            } else {
                println("Failed to interact with corrupt glyph");
                Execution.delay(600);
            }
        }
        
        return true;
    }

    @Override
    public void onLoop() {
        //Loops every 100ms by default, to change:
        //this.loopDelay = 500;
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState == BotState.IDLE) {
            //wait some time so we dont immediately start on login.
            Execution.delay(random.nextLong(3000,7000));
            return;
        }
        
        switch (botState) {
            case IDLE -> {
                //do nothing
                println("We're idle!");
                Execution.delay(random.nextLong(1000,3000));
            }
            case SKILLING -> {
                //do some code that handles your skilling
                Execution.delay(handleSkilling(player));
            }
            case BANKING -> {
                //handle your banking logic, etc
            }
        }
    }

    private long handleSkilling(LocalPlayer player) {
        // Get player animation state
        int playerAnim = player.getAnimationId();
        boolean isIdle = playerAnim == -1;
        
        // Priority -1: Check if components need repair
        if (repairComponents()) {
            return random.nextLong(600, 1200);
        }
        
        // Priority 0: Check if ritual completed and restart, OR check if we need to start initial ritual
        if (startRitual()) {
            return random.nextLong(600, 1200);
        }
        
        // Check if player is idle and needs to interact with platform
        if (isIdle && !ritualCompleted) {
            SceneObject startPlatform = SceneObjectQuery.newQuery().ids(PLATFORM_IDS).option("Start ritual").results().nearest();
            if (startPlatform != null) {
                println("Player idle and ritual not running, starting ritual...");
                ritualCompleted = true; // Set flag so startRitual() will run
                return random.nextLong(600, 1200);
            }
            
            // Check if player is idle and platform has "Continue ritual" option
            SceneObject continuePlatform = SceneObjectQuery.newQuery().ids(PLATFORM_IDS).option("Continue ritual").results().nearest();
            if (continuePlatform != null) {
                println("Player idle but ritual paused, continuing ritual...");
                continueRitual();
                return random.nextLong(600, 1200);
            }
        }
        
        // Priority 1: Handle chat-triggered disturbances first
        if (handleStorm()) {
            return random.nextLong(600, 1200);
        }
        
        if (handleSparkling()) {
            return random.nextLong(600, 1200);
        }
        
        if (handleCorruptGlyphs()) {
            return random.nextLong(600, 1200);
        }
        
        // Priority 2: Check for wandering soul
        if (dismissWanderingSoul()) {
            return random.nextLong(600, 1200);
        }
        
        // Priority 3: Scan for other disturbances
        Npc disturbance = scanForDisturbances();
        if (disturbance != null) {
            println("Disturbance found: " + disturbance.getName());
            // Handle other disturbances here as needed
            return random.nextLong(600, 1200);
        }
        
        // No disturbances, wait for next check
        return random.nextLong(600, 1200);
    }

    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public boolean isSomeBool() {
        return someBool;
    }

    public void setSomeBool(boolean someBool) {
        this.someBool = someBool;
    }
}
