package net.fabricacs.engine.mixin;

import org.newdawn.slick.Graphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 给原版 {@code LightMapLayer} 补一次 {@code Graphics.flush()}。
 *
 * <h2>为什么需要</h2>
 * 迁移版 {@code LightMapLayer} 与 asplit 里的原版**只差这一处**：迁移版在
 * {@code draw(Image,float,float,MyDraw,UniScreen,Color,int,DDDDI)} 方法末尾、
 * {@code glDisable(GL_TEXTURE_2D)} 之后多调了一次 {@code org.newdawn.slick.Graphics.flush()}。
 *
 * <p>光照层只在**战斗**渲染路径上跑。少这一次 flush，兼容层的批渲染顺序被打乱 →
 * 战斗画面全空，而编辑器界面完全正常（编辑器不跑光照层）。这正是"整批 GL 路由"
 * 那次回归的根因。</p>
 *
 * <h2>注入方式</h2>
 * 该方法里 {@code Graphics.resetTransform()} **只出现 1 次**，且它是实例方法——
 * {@code @Redirect} 的处理器第一个参数就是**接收者**，因此不需要 {@code @Local}
 * （本项目所用 Mixin 没有该注解）就能拿到那个 Graphics 实例。
 *
 * <p>与迁移版的唯一偏差：本 mixin 在 {@code resetTransform()} 之后立刻 flush，
 * 而迁移版是在 {@code bindNone()} / {@code glDisable} 之后。批次内容相同，
 * 只是早两条指令；如后续发现问题，可改锚点到别的调用。</p>
 */
@Mixin(targets = "com.zarkonnen.airships.LightMapLayer", remap = false)
public abstract class LightMapLayerMixin {

    /** 目标方法描述符（原版 asplit-*.zip 里的签名，逐字对应）。 */
    private static final String DRAW =
            "draw(Lorg/newdawn/slick/Image;FFLcom/zarkonnen/airships/MyDraw;"
            + "Lcom/zarkonnen/airships/UniScreen;Lorg/newdawn/slick/Color;IDDDDI)V";

    @Redirect(
            method = DRAW,
            at = @At(value = "INVOKE", target = "Lorg/newdawn/slick/Graphics;resetTransform()V"),
            remap = false)
    private void acbric$resetTransformThenFlush(Graphics g) {
        g.resetTransform();
        g.flush();
    }
}
