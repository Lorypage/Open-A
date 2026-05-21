package cn.lazymoon.mixin.injector.splash;

import cn.lazymoon.ingameui.splash.ArcaneSplashRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow public static Identifier LOGO;

    @Shadow @Final private static IntSupplier BRAND_ARGB;

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private ResourceReload reload;
    @Shadow @Final private Consumer<Optional<Throwable>> exceptionHandler;
    @Shadow @Final private boolean reloading;

    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = context.getScaledWindowWidth();
        int j = context.getScaledWindowHeight();
        long l = Util.getMeasuringTimeMs();

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = l;
        }

        float f = this.reloadCompleteTime > -1L ? (float) (l - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float) (l - this.reloadStartTime) / 500.0F : -1.0F;
        float h;

        if (f >= 1.0F) {
            if (this.client.currentScreen != null) {
            }

            int k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && g < 1.0F) {
            }

            int k = MathHelper.ceil(MathHelper.clamp(g, 0.15, 1.0) * 255.0);
            h = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            int k = BRAND_ARGB.getAsInt();
            float m = (k >> 16 & 0xFF) / 255.0F;
            float n = (k >> 8 & 0xFF) / 255.0F;
            float o = (k & 0xFF) / 255.0F;
            h = 1.0F;
        }

        int k = (int) (context.getScaledWindowWidth() * 0.5);
        int p = (int) (context.getScaledWindowHeight() * 0.5);
        double d = Math.min(context.getScaledWindowWidth() * 0.75, context.getScaledWindowHeight()) * 0.25;
        int q = (int) (d * 0.5);
        double e = d * 4.0;
        int r = (int) (e * 0.5);
        int s = ColorHelper.getWhite(h);


        int t = (int) (context.getScaledWindowHeight() * 0.8325);
        float u = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + u * 0.050000012F, 0.0F, 1.0F);

        if (f < 1.0F) {
        }

        if (f >= 2.0F) {
            // 你如果想在加载结束后直接切主菜单，可以在这里改：
            // this.client.setScreen(new ArcaneMainMenuScreen());
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && (!this.reloading || g >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable var24) {
                this.exceptionHandler.accept(Optional.of(var24));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
            }
        }
        ArcaneSplashRenderer.render(context, progress);
    }

    @Unique
    private void arcane$renderProgressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity) {
        int i = MathHelper.ceil((maxX - minX - 2) * this.progress);
        int j = Math.round(opacity * 255.0F);
        int k = ColorHelper.getArgb(j, 255, 255, 255);

    }

    @Unique
    private static int arcane$withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | (alpha << 24);
    }

    @Environment(EnvType.CLIENT)
    static class LogoTexture extends ReloadableTexture {
        public LogoTexture() {
            super(SplashOverlay.LOGO);
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            ResourceFactory resourceFactory = MinecraftClient.getInstance().getDefaultResourcePack().getFactory();
            InputStream inputStream = resourceFactory.open(SplashOverlay.LOGO);

            TextureContents var4;
            try {
                var4 = new TextureContents(NativeImage.read(inputStream), new TextureResourceMetadata(true, true));
            } catch (Throwable var7) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var4;
        }
    }
}