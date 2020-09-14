package epicsquid.roots.entity.mob;

import epicsquid.roots.util.EntityUtil;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;

public class EntityHuskSlave extends EntityHusk {
  public EntityHuskSlave(World worldIn) {
    super(worldIn);
  }

  @Override
  protected void applyEntityAI() {
    this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityMob.class, 10, false, false, EntityUtil::isHostile));
  }
}