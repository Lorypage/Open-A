package cn.lazymoon.sxmurxy.providers;

import net.minecraft.util.Identifier;

public final class ResourceProvider {

	public static Identifier getShaderIdentifier(String name) {
		return Identifier.of("sxmurxy", "core/" + name);
	}

}