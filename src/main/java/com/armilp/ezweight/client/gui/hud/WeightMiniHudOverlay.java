package com.armilp.ezweight.client.gui.hud;

import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.data.WeightSyncData;
import com.armilp.ezweight.player.PlayerWeightHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezweight", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WeightMiniHudOverlay {

    private static final ResourceLocation ICON_GREEN = new ResourceLocation("ezweight", "textures/gui/weight_green.png");
    private static final ResourceLocation ICON_YELLOW = new ResourceLocation("ezweight", "textures/gui/weight_yellow.png");
    private static final ResourceLocation ICON_RED = new ResourceLocation("ezweight", "textures/gui/weight_red.png");

    private static final long GREEN_HIDE_DELAY = 10000L; // ms
    private static final long FADE_OUT_DURATION = 1500L; // ms
    private static final long SCALE_ANIMATION_DURATION = 400L;
    private static final float SHAKE_AMPLITUDE = 1.5f;
    private static final float SHAKE_FREQUENCY = 5.0f;

    private enum WeightCategory { GREEN, YELLOW, RED }

    private static WeightCategory lastCategory = WeightCategory.GREEN;
    private static long lastCategoryChangeTime = System.currentTimeMillis();
    private static long greenEntryTime = -1;
    private static boolean fadingOut = false;
    private static float currentAlpha = 1.0f;


    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics graphics = event.getGuiGraphics();
        double weight = PlayerWeightHandler.getTotalWeight(mc.player);
        double maxWeight = WeightSyncData.getMaxWeight();
        double pct = Math.min(1.0, Math.max(0.0, weight / maxWeight));

        WeightCategory currentCategory = getCategoryForPercentage(pct);
        long now = System.currentTimeMillis();

        if (currentCategory != lastCategory) {
            lastCategory = currentCategory;
            lastCategoryChangeTime = now;
        }

        if (currentCategory == WeightCategory.GREEN) {
            if (greenEntryTime == -1) {
                greenEntryTime = now;
            }
        } else {
            greenEntryTime = -1;
        }

        float alpha = computeAlpha(currentCategory, now);
        if (alpha <= 0f) return;

        float scale = computeScale(now);
        float shakeX = computeShakeOffset(pct, now);

        ResourceLocation icon = getIconForCategory(currentCategory);
        int iconSize = WeightConfig.CLIENT.MINI_HUD_ICON_SIZE.get();
        int x = WeightConfig.CLIENT.MINI_HUD_X.get() + (int) shakeX;
        int y = WeightConfig.CLIENT.MINI_HUD_Y.get();

        renderIcon(graphics, icon, x, y, iconSize, scale, alpha);
    }

    private static WeightCategory getCategoryForPercentage(double pct) {
        if (pct >= 0.8) return WeightCategory.RED;
        if (pct >= 0.5) return WeightCategory.YELLOW;
        return WeightCategory.GREEN;
    }

    private static ResourceLocation getIconForCategory(WeightCategory category) {
        return switch (category) {
            case RED -> ICON_RED;
            case YELLOW -> ICON_YELLOW;
            case GREEN -> ICON_GREEN;
        };
    }

    private static float computeAlpha(WeightCategory category, long now) {
        if (category == WeightCategory.GREEN) {
            if (greenEntryTime == -1) greenEntryTime = now;

            long timeInGreen = now - greenEntryTime;
            if (timeInGreen >= GREEN_HIDE_DELAY) {
                fadingOut = true;
                long fadeTime = timeInGreen - GREEN_HIDE_DELAY;
                if (fadeTime >= FADE_OUT_DURATION) {
                    currentAlpha = 0f;
                } else {
                    currentAlpha = 1.0f - (fadeTime / (float) FADE_OUT_DURATION);
                }
            } else {
                fadingOut = false;
                currentAlpha = 1.0f;
            }
        } else {
            fadingOut = false;
            greenEntryTime = -1;
            currentAlpha = (category == WeightCategory.RED)
                    ? 0.8f + 0.2f * (float) Math.sin(now / 100.0)
                    : 1.0f;
        }

        return currentAlpha;
    }


    private static float computeScale(long now) {
        long elapsed = now - lastCategoryChangeTime;
        if (elapsed >= SCALE_ANIMATION_DURATION) return 1.0f;

        float progress = elapsed / (float) SCALE_ANIMATION_DURATION;
        float overshoot = 1.70158f;
        progress--;
        return 1.0f + (progress * progress * ((overshoot + 1) * progress + overshoot));
    }

    private static float computeShakeOffset(double pct, long now) {
        if (pct >= 0.9) {
            return (float) Math.sin(now / 100.0 * SHAKE_FREQUENCY) * SHAKE_AMPLITUDE;
        }
        return 0f;
    }

    private static void renderIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size, float scale, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, icon);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        graphics.pose().pushPose();
        graphics.pose().translate(x + size / 2f, y + size / 2f, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.pose().translate(-size / 2f, -size / 2f, 0);
        graphics.blit(icon, 0, 0, 0, 0, size, size, size, size);
        graphics.pose().popPose();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
