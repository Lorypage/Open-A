package cn.lazymoon.mixin.injector.input;

import cn.lazymoon.command.completion.CommandCompleter;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {

    @Shadow @Final TextFieldWidget textField;
    @Shadow @Nullable private ChatInputSuggestor.SuggestionWindow window;
    @Shadow @Nullable private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private boolean windowActive;
    @Shadow boolean completingSuggestions;

    @Shadow public abstract void show(boolean narrateFirstSuggestion);

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String text = textField.getText();
        if (!text.startsWith(".")) return;
        ci.cancel();

        if (!completingSuggestions) {
            textField.setSuggestion(null);
            window = null;
        }

        String input = text.substring(1);
        String[] args = input.split(" ", -1);
        int cursor = textField.getCursor();

        int start = Math.max(text.lastIndexOf(' ', cursor - 1) + 1, 1);
        SuggestionsBuilder builder = new SuggestionsBuilder(text, start);
        CommandCompleter.getSuggestions(args).forEach(builder::suggest);

        pendingSuggestions = builder.buildFuture();
        pendingSuggestions.thenRun(() -> {
            if (!pendingSuggestions.isDone()) return;
            MinecraftClient.getInstance().execute(() -> {
                if (windowActive) show(false);
            });
        });
    }
}
