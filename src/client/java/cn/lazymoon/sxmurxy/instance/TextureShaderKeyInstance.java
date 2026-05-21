package cn.lazymoon.sxmurxy.instance;

import cn.lazymoon.sxmurxy.providers.ResourceProvider;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;

public class TextureShaderKeyInstance {

    public static final ShaderProgramKey TEXTURE_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

}
