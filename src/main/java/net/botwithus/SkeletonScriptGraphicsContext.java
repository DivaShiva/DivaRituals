package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {

    private SkeletonScript script;

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, SkeletonScript script) {
        super(scriptConsole);
        this.script = script;
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("Necromancy Rituals", ImGuiWindowFlag.None.getValue())) {
            if (ImGui.BeginTabBar("My bar", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.getValue())) {
                    ImGui.Text("Necromancy Ritual Bot");
                    ImGui.Separator();
                    
                    ImGui.Text("Current State: " + script.getBotState());
                    ImGui.Separator();
                    
                    // Start button
                    if (ImGui.Button("Start Rituals")) {
                        script.setBotState(SkeletonScript.BotState.SKILLING);
                    }
                    
                    ImGui.SameLine();
                    
                    // Stop button
                    if (ImGui.Button("Stop")) {
                        script.setBotState(SkeletonScript.BotState.IDLE);
                    }
                    
                    ImGui.Separator();
                    ImGui.Text("Handles disturbances:");
                    ImGui.Text("- Wandering Soul");
                    ImGui.Text("- Storm of Souls");
                    ImGui.Text("- Sparkling Glyph");
                    ImGui.Text("- Auto-restarts rituals");
                    
                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Other", ImGuiWindowFlag.None.getValue())) {
                    script.setSomeBool(ImGui.Checkbox("Are you cool?", script.isSomeBool()));
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            ImGui.End();
        }

    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}
