package com.blamejared.clumps.entities;

import com.blamejared.clumps.Clumps;
import com.blamejared.clumps.events.EXPMergeEvent;
import net.minecraft.enchantment.*;
import net.minecraft.entity.*;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

public class EntityXPOrbBig extends ExperienceOrbEntity implements IEntityAdditionalSpawnData {
    
    /**
     * The closest EntityPlayer to this orb.
     */
    private PlayerEntity closestPlayer;
    /**
     * Threshold color for tracking players
     */
    private int xpTargetColor;
    
    public EntityXPOrbBig(World worldIn, double x, double y, double z, int expValue) {
        super(Clumps.BIG_ORB_ENTITY_TYPE, worldIn);
        this.setPosition(x, y, z);
        this.rotationYaw = (float) (this.rand.nextDouble() * 360.0D);
        this.setMotion((this.rand.nextDouble() * (double) 0.2F - (double) 0.1F) * 2.0D, this.rand.nextDouble() * 0.2D * 2.0D, (this.rand.nextDouble() * (double) 0.2F - (double) 0.1F) * 2.0D);
        this.xpValue = expValue;
    }
    
    public EntityXPOrbBig(EntityType<? extends ExperienceOrbEntity> type, World world) {
        super(Clumps.BIG_ORB_ENTITY_TYPE, world);
    }
    
    public EntityXPOrbBig(World world) {
        super(Clumps.BIG_ORB_ENTITY_TYPE, world);
    }
    
    @Override
    public void tick() {
        if(!world.isRemote && this.xpValue == 0) {
            this.remove();
            return;
        }
        if(this.delayBeforeCanPickup > 0) {
            --this.delayBeforeCanPickup;
        }
        
        this.prevPosX = this.getPosX();
        this.prevPosY = this.getPosY();
        this.prevPosZ = this.getPosZ();
        
        if(this.areEyesInFluid(FluidTags.WATER)) {
            this.applyFloatMotion();
        } else if(!this.hasNoGravity()) {
            this.setMotion(this.getMotion().add(0.0D, -0.03D, 0.0D));
        }
        
        if(this.world.getFluidState(this.getPosition()).isTagged(FluidTags.LAVA)) {
            this.setMotion((double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F), (double) 0.2F, (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F));
            this.playSound(SoundEvents.ENTITY_GENERIC_BURN, 0.4F, 2.0F + this.rand.nextFloat() * 0.4F);
        }
        
        if(!this.world.hasNoCollisions(this.getBoundingBox())) {
            this.pushOutOfBlocks(this.getPosX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.getPosZ());
        }
        
        if(this.xpTargetColor < this.xpColor - 20 + this.getEntityId() % 100) {
            if(this.closestPlayer == null || this.closestPlayer.getDistance(this) > 64.0D) {
                this.closestPlayer = this.world.getClosestPlayer(this, 8.0D);
            }
            
            this.xpTargetColor = this.xpColor;
        }
        
        if(this.closestPlayer != null && this.closestPlayer.isSpectator()) {
            this.closestPlayer = null;
        }
        
        if(this.closestPlayer != null) {
            Vector3d vec3d = new Vector3d(this.closestPlayer.getPosX() - this.getPosX(), this.closestPlayer.getPosY() + (double) this.closestPlayer.getEyeHeight() / 2.0D - this.getPosY(), this.closestPlayer.getPosZ() - this.getPosZ());
            double d1 = vec3d.lengthSquared();
            if(d1 < 64.0D) {
                double d2 = 1.0D - Math.sqrt(d1) / 8.0D;
                this.setMotion(this.getMotion().add(vec3d.normalize().scale(d2 * d2 * 0.1D)));
            }
        }
        
        this.move(MoverType.SELF, this.getMotion());
        float f = 0.98F;
        
        if(this.onGround) {
            BlockPos underPos = new BlockPos(this.getPosX(), this.getBoundingBox().minY - 1.0D, this.getPosZ());
            f = this.world.getBlockState(underPos).getSlipperiness(this.world, underPos, this) * 0.98F;
        }
        
        this.setMotion(this.getMotion().mul((double) f, 0.98D, (double) f));
        if(this.onGround) {
            this.setMotion(this.getMotion().mul(1.0D, -0.9D, 1.0D));
        }
        ++this.xpColor;
        ++this.xpOrbAge;
        if(this.xpOrbAge >= 6000) {
            this.remove();
        }
        if(world.getGameTime() % 5 == 0) {
            List<EntityXPOrbBig> orbs = world.getEntitiesWithinAABB(EntityXPOrbBig.class, new AxisAlignedBB(getPosX() - 2, getPosY() - 2, getPosZ() - 2, getPosX() + 2, getPosY() + 2, getPosZ() + 2), EntityPredicates.IS_ALIVE);
            int newSize = 0;
            if(orbs.size() > 0) {
                EntityXPOrbBig orb = orbs.get(world.rand.nextInt(orbs.size()));
                if(!orb.getUniqueID().equals(this.getUniqueID()) && orb.xpValue <= this.xpValue && orb.xpValue != 0) {
                    newSize += orb.getXpValue() + xpValue;
                }
                if(newSize > xpValue) {
                    if(!world.isRemote) {
                        EntityXPOrbBig newOrb = new EntityXPOrbBig(world, getPosX(), getPosY(), getPosZ(), newSize);
                        MinecraftForge.EVENT_BUS.post(new EXPMergeEvent(this, orb, newOrb));

                        newOrb.setMotion(0, 0, 0);
                        world.addEntity(newOrb);
                        remove();
                    }
                    // This doesn't cause removed packets and kills the orb the next time it ticks
                    // This line has also been moved here so the orb's xpValue is still correct in the EXPMergeEvent
                    orb.xpValue = 0;
                }
                orbs.clear();
            }
        }
    }
    
    private void applyFloatMotion() {
        Vector3d vec3d = this.getMotion();
        this.setMotion(vec3d.x * (double) 0.99F, Math.min(vec3d.y + (double) 5.0E-4F, (double) 0.06F), vec3d.z * (double) 0.99F);
    }
    
    /**
     * Called by a player entity when they collide with an entity
     */
    public void onCollideWithPlayer(PlayerEntity entityIn) {
        if(!this.world.isRemote) {
            if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(entityIn, this)))
                return;
            entityIn.xpCooldown = 0;
            entityIn.onItemPickup(this, 1);
            Map.Entry<EquipmentSlotType, ItemStack> entry = EnchantmentHelper.getRandomItemWithEnchantment(Enchantments.MENDING, entityIn);
            if(entry != null) {
                ItemStack itemstack = entry.getValue();
                if(!itemstack.isEmpty() && itemstack.isDamaged()) {
                    int i = Math.min((int) (this.xpValue * itemstack.getXpRepairRatio()), itemstack.getDamage());
                    this.xpValue -= this.durabilityToXp(i);
                    itemstack.setDamage(itemstack.getDamage() - i);
                }
            }
            
            if(this.xpValue > 0) {
                entityIn.giveExperiencePoints(this.xpValue);
            }
            
            this.remove();
            
        }
    }
    
    private int durabilityToXp(int durability) {
        return durability / 2;
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeInt(this.xpValue);
    }
    
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        this.xpValue = additionalData.readInt();
    }
}
