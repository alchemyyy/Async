package com.axalotl.async.common.mixin.lithium;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.HashCommon;
import net.caffeinemc.mods.lithium.common.world.chunk.LithiumHashPalette;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(value = LithiumHashPalette.class, remap = false)
public class LithiumHashPaletteConcurrentMixin<T> {

    @Shadow
    private T[] entries;

    @Shadow
    private int size;

    // volatile обеспечивает, что все потоки видят актуальную ссылку на список
    @Unique
    private volatile CopyOnWriteArrayList<T> async$concurrentEntries;

    @Inject(method = "<init>(Lnet/minecraft/core/IdMap;ILnet/minecraft/world/level/chunk/PaletteResize;)V", at = @At("RETURN"))
    private void initConcurrentList(CallbackInfo ci) {
        this.async$concurrentEntries = new CopyOnWriteArrayList<>();
        int cap = this.entries != null ? this.entries.length : 0;
        for (int i = 0; i < cap; i++) {
            this.async$concurrentEntries.add(null);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/core/IdMap;ILnet/minecraft/world/level/chunk/PaletteResize;Ljava/util/List;)V", at = @At("RETURN"))
    private void initConcurrentListWithData(CallbackInfo ci) {
        if (this.async$concurrentEntries == null) {
            this.async$concurrentEntries = new CopyOnWriteArrayList<>();
            if (this.entries != null) {
                this.async$concurrentEntries.addAll(Arrays.asList(this.entries));
            }
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/core/IdMap;Lnet/minecraft/world/level/chunk/PaletteResize;I[Ljava/lang/Object;Lit/unimi/dsi/fastutil/objects/Reference2IntOpenHashMap;I)V", at = @At("RETURN"))
    private void initConcurrentListPrivate(CallbackInfo ci) {
        if (this.async$concurrentEntries == null) {
            this.async$concurrentEntries = new CopyOnWriteArrayList<>();
            if (this.entries != null) {
                this.async$concurrentEntries.addAll(Arrays.asList(this.entries));
            }
        }
    }

    @WrapMethod(method = "valueFor")
    private T concurrentValueFor(int id, Operation<T> original) {
        // Чтение без блокировок.
        // Если вылетит IndexOutOfBoundsException - значит палитра сломана/очищена во время чтения.
        // Пусть падает (Fail Fast), никаких try-catch.
        CopyOnWriteArrayList<T> list = this.async$concurrentEntries;
        if (list != null && id >= 0 && id < list.size()) {
            T entry = list.get(id);
            if (entry != null) {
                return entry;
            }
        }
        return original.call(id);
    }

    // synchronized на запись гарантирует, что два потока не добавят записи с одинаковым ID
    @WrapMethod(method = "addEntry")
    private synchronized int concurrentAddEntry(Object obj, Operation<Integer> original) {
        // 1. Получаем ID от Lithium (оригинала)
        int assignedId = original.call(obj);

        // 2. Синхронизируем наш список
        if (this.async$concurrentEntries != null) {
            // Если Lithium расширил массив (resize), мы должны догнать его размер
            while (this.async$concurrentEntries.size() <= assignedId) {
                this.async$concurrentEntries.add(null);
            }
            this.async$concurrentEntries.set(assignedId, (T) obj);
        }

        return assignedId;
    }

    @WrapMethod(method = "maybeHas")
    private boolean concurrentMaybeHas(java.util.function.Predicate<T> predicate, Operation<Boolean> original) {
        CopyOnWriteArrayList<T> list = this.async$concurrentEntries;
        if (list != null) {
            // Итератор CopyOnWriteArrayList безопасен (snapshot style)
            for (T entry : list) {
                if (entry != null && predicate.test(entry)) {
                    return true;
                }
            }
            return false;
        }
        return original.call(predicate);
    }

    @WrapMethod(method = "resize")
    private synchronized void concurrentResize(int neededCapacity, Operation<Void> original) {
        original.call(neededCapacity);

        if (this.async$concurrentEntries != null) {
            int newSize = HashCommon.nextPowerOfTwo(neededCapacity + 1);
            while (this.async$concurrentEntries.size() < newSize) {
                this.async$concurrentEntries.add(null);
            }
        }
    }

    @WrapMethod(method = "clear")
    private synchronized void concurrentClear(Operation<Void> original) {
        original.call();

        if (this.async$concurrentEntries != null) {
            this.async$concurrentEntries.clear();
            // Восстанавливаем структуру (заполняем null-ами под размер массива)
            int cap = this.entries != null ? this.entries.length : 0;
            for (int i = 0; i < cap; i++) {
                this.async$concurrentEntries.add(null);
            }
        }
    }
}