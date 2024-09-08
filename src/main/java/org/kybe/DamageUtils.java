package org.kybe;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.utils.ChatUtils;

public class DamageUtils {
	private static Minecraft mc = Minecraft.getInstance();

	public static float getBedDamage(Vec3 explosion, Entity entity) {
		if (mc.player == null) return 0.0F;

		/*
		 * Base naked damage
		 */
		float f = 5.0F * 2.0F;
		double d = Math.sqrt(entity.distanceToSqr(explosion)) / (double)f;
		double e = (1.0 - d) * (double)Explosion.getSeenPercent(explosion, entity);
		final float[] result = {(float) ((e * e + e) / 2.0 * 7.0 * (double) f + 1.0)};

		/*
		 * Armor reduction
		 */
		Iterable<ItemStack> armor = mc.player.getArmorSlots();
		for (ItemStack itemStack : armor) {
			if (itemStack.getItem() instanceof ArmorItem) {
				ArmorItem armorItem = (ArmorItem) itemStack.getItem();
				result[0] = calculateDamage(result[0], armorItem.getDefense(), armorItem.getToughness());
			}
		}

		/*
		 * Blast protection reduction
		 */
		Registry<Enchantment> enchantmentRegistry = mc.level.registryAccess().registry(Registries.ENCHANTMENT).orElseThrow();
		Holder<Enchantment> blastprotection = enchantmentRegistry.getHolderOrThrow(Enchantments.BLAST_PROTECTION);

		int blastProtectionLevel = 0;
		for (ItemStack itemStack : armor) {
			blastProtectionLevel += itemStack.getEnchantments().getLevel(blastprotection);
		}
		result[0] = applyBlastProtection(result[0], blastProtectionLevel);

		Holder<Enchantment> protection = enchantmentRegistry.getHolderOrThrow(Enchantments.PROTECTION);
		int protectionLevel = 0;
		for (ItemStack itemStack : armor) {
			protectionLevel += itemStack.getEnchantments().getLevel(protection);
		}
		result[0] = applyProtection(result[0], protectionLevel);

		return result[0];
	}

	public static float calculateDamage(float baseDamage, int defensePoints, float toughness) {
		float cappedDefensePoints = Math.min(defensePoints, 20);

		float damageReduction = cappedDefensePoints / 5.0f;

		float toughnessReduction = cappedDefensePoints - (4.0f * baseDamage / (toughness + 8.0f));

		float effectiveReduction = Math.max(damageReduction, toughnessReduction);

		float reductionFactor = 1.0f - (Math.min(effectiveReduction, 20.0f) / 25.0f);

		return baseDamage * reductionFactor;
	}

	public static float applyBlastProtection(float baseDamage, int blastProtectionLevel) {
		int clampedLevel = Math.min(blastProtectionLevel, 4);

		float reductionPercentage = clampedLevel * 0.08f;

		float reducedDamage = baseDamage * (1 - reductionPercentage);

		float maxReduction = baseDamage * 0.80f; // 80% reduction cap
		reducedDamage = Math.max(reducedDamage, baseDamage - maxReduction);

		return reducedDamage;
	}

	public static float applyProtection(float baseDamage, int protectionLevel) {
		// Ensure protectionLevel is between 0 and 4 (Protection IV)
		int clampedLevel = Math.min(protectionLevel, 4);

		// Calculate protection reduction percentage
		float protectionReductionPercentage = clampedLevel * 0.04f; // 4% per level

		// Apply protection reduction
		float reducedDamageAfterProtection = baseDamage * (1 - protectionReductionPercentage);

		return reducedDamageAfterProtection;
	}

}
