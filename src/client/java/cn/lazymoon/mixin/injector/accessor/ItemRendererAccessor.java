package cn.lazymoon.mixin.injector.accessor;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface ItemRendererAccessor {
    @Accessor("itemRenderState")
    ItemRenderState getItemRenderState();
}
