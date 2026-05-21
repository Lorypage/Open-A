package cn.lazymoon.features.module.impl.combat;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.AttackEvent;
import cn.lazymoon.event.impl.player.PostUpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.utils.client.ClientData;
import net.minecraft.entity.LivingEntity;

@ModuleInfo(name = "SuperKnockBack",description = "Helping you attack others can super knockback",key = 0,category = Category.Combat,hidden = false)
public class SuperKnockBack extends Module {
   public static ModeValue mode = new ModeValue("Mode", new String[]{"Sprint Reset"}, "Sprint Reset");

   public static LivingEntity target;
   public static boolean handle;

   @EventTarget
   public void onAttack(AttackEvent event) {
      handle = true;
      target = (LivingEntity) event.getEntity();
   }

   @EventTarget
   public void onPostUpdate(PostUpdateEvent event) {
      if (mc.player == null) return;
      if (ClientData.realSprint && handle) {
         if (mode.is("Sprint Reset") && (!AntiKnockback.receiveVelocity && !AntiKnockback.buffer)) {
             if (target != null && (target.hurtTime == 0 || target.hurtTime == 1)) {
                 mc.player.setSprinting(false);
                 handle = false;
             }
         }
      }
   }
}
