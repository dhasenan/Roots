package epicsquid.roots.item;

import epicsquid.mysticallib.item.ItemBase;
import epicsquid.mysticallib.network.PacketHandler;
import epicsquid.mysticallib.particle.particles.ParticleGlitter;
import epicsquid.mysticallib.proxy.ClientProxy;
import epicsquid.mysticallib.util.ItemUtil;
import epicsquid.mysticallib.util.Util;
import epicsquid.roots.capability.runic_shears.RunicShearsCapability;
import epicsquid.roots.capability.runic_shears.RunicShearsCapabilityProvider;
import epicsquid.roots.config.GeneralConfig;
import epicsquid.roots.config.MossConfig;
import epicsquid.roots.init.ModBlocks;
import epicsquid.roots.init.ModItems;
import epicsquid.roots.init.ModRecipes;
import epicsquid.roots.network.fx.MessageRunicShearsAOEFX;
import epicsquid.roots.network.fx.MessageRunicShearsFX;
import epicsquid.roots.recipe.RunicShearEntityRecipe;
import epicsquid.roots.recipe.RunicShearRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.*;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RunicShearsItem extends ItemBase {
  public static AxisAlignedBB bounding = new AxisAlignedBB(-2, -2, -2, 2, 2, 2);
  private Random random;

  public RunicShearsItem(@Nonnull String name) {
    super(name);
    setMaxDamage(357);
    setMaxStackSize(1);
    setHasSubtypes(false);
    random = new Random();
  }

  @Override
  public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
    Item item = repair.getItem();
    if (item instanceof BlockItem) {
      Block block = ((BlockItem) item).getBlock();
      return ModBlocks.runestoneBlocks.contains(block);
    }

    return false;
  }

  @Override
  @Nonnull
  public ActionResultType onItemUse(PlayerEntity player, World world, BlockPos pos, Hand hand, Direction facing, float hitX, float hitY, float hitZ) {
    BlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (block == ModBlocks.imbuer) {
      return ActionResultType.PASS;
    }

    BlockState moss = MossConfig.scrapeResult(state);
    BlockState moss2 = MossConfig.mossConversion(state);

    if (moss != null || moss2 != null) {
      if (!world.isRemote) {
        AxisAlignedBB bounds = bounding.offset(pos);
        BlockPos start = new BlockPos(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos stop = new BlockPos(bounds.maxX, bounds.maxY, bounds.maxZ);
        List<BlockPos> affectedBlocks = new ArrayList<>();
        for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(start, stop)) {
          BlockState pState = world.getBlockState(p);
          BlockState m = MossConfig.scrapeResult(pState);
          if (m != null) {
            affectedBlocks.add(p.toImmutable());
            world.setBlockState(p, m);
            world.scheduleBlockUpdate(p, m.getBlock(), 1, m.getBlock().tickRate(world));
            ItemUtil.spawnItem(world, player.getPosition().add(0, 1, 0), new ItemStack(ModItems.terra_moss));
          }
        }
        if (!affectedBlocks.isEmpty()) {
          if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).damageItem(1 + Math.min(6, random.nextInt(affectedBlocks.size())), player);
          }
          MessageRunicShearsAOEFX message = new MessageRunicShearsAOEFX(affectedBlocks);
          PacketHandler.sendToAllTracking(message, world.provider.getDimension(), pos);
        }
      }
      player.swingArm(hand);
      world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1f, 1f);
      return ActionResultType.SUCCESS;
    }

    RunicShearRecipe recipe = ModRecipes.getRunicShearRecipe(block);

    if (recipe != null) {
      if (!world.isRemote) {

        if (block instanceof CropsBlock) {
          if (((CropsBlock) block).isMaxAge(world.getBlockState(pos))) {
            world.setBlockState(pos, ((CropsBlock) block).withAge(0));
          } else {
            return ActionResultType.SUCCESS;
          }
        } else {
          world.setBlockState(pos, recipe.getReplacementBlock().getDefaultState());
        }
        ItemUtil.spawnItem(world, player.getPosition().add(0, 1, 0), recipe.getDrop().copy());
        if (!player.capabilities.isCreativeMode) {
          player.getHeldItem(hand).damageItem(1, player);
        }
        player.swingArm(hand);
        world.playSound(null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1f, 1f);
      } else {
        for (int i = 0; i < 50; i++) {
          ClientProxy.particleRenderer.spawnParticle(world, Util.getLowercaseClassName(ParticleGlitter.class), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1), random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1),
              random.nextDouble() * 0.1 * (random.nextDouble() > 0.5 ? -1 : 1), 120, 0.855 + random.nextDouble() * 0.05, 0.710, 0.943 - random.nextDouble() * 0.05, 1, random.nextDouble() + 0.5, random.nextDouble() * 2);
        }
      }
    }
    return ActionResultType.SUCCESS;
  }

  @Override
  public boolean itemInteractionForEntity(ItemStack itemstack, PlayerEntity player, LivingEntity entity, Hand hand) {
    World world = player.world;
    Random rand = itemRand;

    if (entity instanceof IShearable) {
      int count = 0;
      if (Items.SHEARS.itemInteractionForEntity(itemstack, player, entity, hand)) count++;

      float radius = GeneralConfig.RunicShearsRadius;
      List<MobEntity> entities = Util.getEntitiesWithinRadius(entity.world, (Entity e) -> e instanceof IShearable, entity.getPosition(), radius, radius / 2, radius);
      for (MobEntity e : entities) {
        e.captureDrops = true;
        if (Items.SHEARS.itemInteractionForEntity(itemstack, player, e, hand)) count++;
        e.captureDrops = false;
        if (!world.isRemote) {
          for (ItemEntity ent : e.capturedDrops) {
            ent.setPosition(entity.posX, entity.posY, entity.posZ);
            ent.motionY = 0;
            ent.motionX = 0;
            ent.motionZ = 0;
            ent.world.spawnEntity(ent);
          }
        }
      }
      if (count > 0) {
        player.swingArm(hand);
        return true;
      }
    }

    if (entity.isChild()) return true;

    RunicShearEntityRecipe recipe = ModRecipes.getRunicShearRecipe(entity);
    if (recipe != null) {
      player.swingArm(hand);
      if (!world.isRemote) {
        RunicShearsCapability cap = entity.getCapability(RunicShearsCapabilityProvider.RUNIC_SHEARS_CAPABILITY, null);
        if (cap != null) {
          if (cap.canHarvest()) {
            cap.setCooldown(recipe.getCooldown());
            ItemEntity ent = entity.entityDropItem(recipe.getDrop().copy(), 1.0F);
            ent.motionY += rand.nextFloat() * 0.05F;
            ent.motionX += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            ent.motionZ += (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            if (!player.capabilities.isCreativeMode) {
              itemstack.damageItem(1, entity);
            }
            world.playSound(player, entity.getPosition(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1f, 1f);
            IMessage packet = new MessageRunicShearsFX(entity);
            PacketHandler.sendToAllTracking(packet, entity);
            return true;
          } else {
            // TODO: play particles (failure)?
            player.sendStatusMessage(new TranslationTextComponent("roots.runic_shears.cooldown").setStyle(new Style().setColor(TextFormatting.DARK_PURPLE)), true);
          }
        }
      }
    }

    return false;
  }
}