package net.fabricacs.engine.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让原版 {@code AGame.getStaticGameDirectory()} 支持 {@code -Dacs.staticdir} 覆盖。
 *
 * <h2>为什么需要</h2>
 * 迁移版与原版在这个方法上只差一处：迁移版在方法开头插入
 * {@code -Dacs.staticdir} 覆盖分支，让发布包启动脚本能跨平台显式指定资源根目录
 * （没有它就只能靠 {@code dev} 开关、macOS 的 bundle 布局或 class 文件所在目录推断）。
 *
 * <p>Acbric 场景下这条尤其必要：实例目录（放 mods）与数据目录（放 data）可以分开，
 * 本 MOD 的 {@code gradlew runGame -PstaticDir=...} 就是靠它工作的。</p>
 *
 * <h2>注入方式</h2>
 * {@code @Inject(at = HEAD, cancellable = true)}：命中时直接返回覆盖路径，
 * 未设置属性时不干预，原版逻辑照常执行。纯行为注入，不新增任何成员，
 * 因此不需要访问器接口。
 */
@Mixin(targets = "com.zarkonnen.airships.AGame", remap = false)
public abstract class AGameMixin {

    @Inject(
            method = "getStaticGameDirectory()Ljava/io/File;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private static void acbric$staticDirOverride(CallbackInfoReturnable<File> cir) {
        String override = System.getProperty("acs.staticdir");
        if (override != null && !override.isEmpty()) {
            cir.setReturnValue(new File(override).getAbsoluteFile());
        }
    }
}
