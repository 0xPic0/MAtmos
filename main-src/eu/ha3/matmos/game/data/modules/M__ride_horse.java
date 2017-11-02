package eu.ha3.matmos.game.data.modules;

import eu.ha3.matmos.engine.core.interfaces.Data;
import eu.ha3.matmos.game.data.abstractions.module.Module;
import eu.ha3.matmos.game.data.abstractions.module.ModuleProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;

/*
--filenotes-placeholder
*/

public class M__ride_horse extends ModuleProcessor implements Module
{
	public M__ride_horse(Data data)
	{
		super(data, "ride_horse");
	}
	
	@Override
	protected void doProcess()
	{
		Entity xride = Minecraft.getMinecraft().player.getRidingEntity();
		
		if (xride == null || !(xride instanceof EntityHorse))
		{
			//setValue("jumping", false);
			setValue("rearing", false);
			setValue("saddled", false);
			setValue("leashed", false);
			setValue("chested", false);
			setValue("tame", false);
			setValue("type", 0);
			setValue("variant", 0);
			setValue("name_tag", "");
			setValue("health1k", 0);
			setValue("leashed_to_player", false);
			setValue("ridden_by_owner", false);
			setValue("leashed_to_owner", false);
			setValue("leash_distance", 0);
			setValue("temper", 0);
			setValue("owner_name", "");
			setValue("reproduced", false);
			setValue("bred", false);
			
			return;
		}
		
		EntityHorse ride = (EntityHorse) xride;
		
		//setValue("jumping", ride.isHorseJumping()); // not functionnal
		setValue("rearing", ride.isRearing());
		setValue("saddled", ride.isHorseSaddled());
		setValue("leashed", ride.getLeashed());
		//setValue("chested", ride.isChested()); TODO: find out usage
		setValue("tame", ride.isTame());
		
		setValue("type", !ride.isDead/*getType().isUndead()*/);
		setValue("variant", ride.getHorseVariant());
		
		setValue("name_tag", ride.getCustomNameTag());
		
		setValue("health1k", (int) (ride.getHealth() * 1000));
		setValue("leashed_to_player", ride.getLeashed() && ride.getLeashHolder() instanceof EntityPlayer);
        // dag edit func_152119_ch() -> getOwnerId()
		setValue(
			"ridden_by_owner",
			ride.getControllingPassenger() instanceof EntityPlayer
				&& ride.getOwnerUniqueId() != null
				&& ride.getOwnerUniqueId().equals(((EntityPlayer) ride.getControllingPassenger()).getGameProfile().getId()));
        // dag edit func_152119_ch() -> getOwnerId()
		setValue(
			"leashed_to_owner",
			ride.getLeashHolder() instanceof EntityPlayer
				&& !ride.getOwnerUniqueId().equals("")
				&& ride.getOwnerUniqueId().equals(((EntityPlayer) ride.getLeashHolder()).getGameProfile().getId().toString()));
		
		if (ride.getLeashed() && ride.getLeashHolder() != null)
		{
			Entity e = ride.getLeashHolder();
			setValue("leash_distance", (int) (e.getDistance(ride) * 1000));
		}
		else
		{
			setValue("leash_distance", 0);
		}
		
		// Server only?
		setValue("temper", ride.getTemper());
        // dag edit func_152119_ch() -> getOwnerId()
		setValue("owner_uuid", ride.getOwnerUniqueId().toString());
		//setValue("reproduced", ride.getHasReproduced()); TODO: find out usage
        // dag edit func_110205_ce() -> isBreeding() [this is probably wrong, not sure what 'bred' represents]
        setValue("bred", ride.isBreeding());
	}
}