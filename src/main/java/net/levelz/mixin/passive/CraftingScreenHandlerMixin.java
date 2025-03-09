package net.levelz.mixin.passive;

import net.levelz.experience.SkillExperienceManager;
import net.levelz.experience.SkillExperienceManager.SkillXpType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * This mixin adds XP to smithing skill when crafting items
 */
@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {

    @Inject(method = "updateResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/inventory/CraftingResultInventory;setStack(ILnet/minecraft/item/ItemStack;)V"),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void updateResultMixin(ScreenHandler handler, World world, PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory, RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci, CraftingRecipeInput craftingRecipeInput, ServerPlayerEntity serverPlayerEntity, ItemStack itemStack) {
        if (player != null && !player.getWorld().isClient() && !player.isCreative() && !itemStack.isEmpty()) {
            // Award smithing XP when crafting tools, weapons or armor
            if (itemStack.get(DataComponentTypes.TOOL) != null || itemStack.isDamageable()) {
                int xpAmount = 2;

                // More XP for more valuable items
                if (itemStack.getMaxDamage() > 250) {
                    xpAmount += 2;
                }

                SkillExperienceManager.getInstance().awardSkillXp(
                        player, 7, xpAmount, SkillXpType.SMITHING_CRAFT);
            }
        }
    }
}