package eu.ha3.matmos.game.data.modules;

import eu.ha3.matmos.engine.core.interfaces.Data;
import eu.ha3.matmos.game.data.abstractions.module.Module;
import eu.ha3.matmos.game.data.abstractions.module.ModuleProcessor;
import eu.ha3.matmos.game.system.MAtMod;
import eu.ha3.matmos.game.system.MAtmosUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

/*
--filenotes-placeholder
*/

public class M__w_biome extends ModuleProcessor implements Module
{
	private final MAtMod mod;
	
	public M__w_biome(Data data, MAtMod mod)
	{
		super(data, "w_biome");
		this.mod = mod;
	}
	
	@Override
	protected void doProcess()
	{
		int biomej = this.mod.getConfig().getInteger("useroptions.biome.override");
		if (biomej <= -1)
		{
			//Solly edit - only calculate biome once
			Biome biome = calculateBiome();
			setValue("id", Biome.getIdForBiome(biome));
			setValue("biome_name", biome.getBiomeName());
		}
		else
		{
			setValue("id", biomej);
			setValue("biome_name", "");
		}
	}
	
	private Biome calculateBiome()
	{
		Minecraft mc = Minecraft.getMinecraft();

        // dag edit - use BlockPos
        BlockPos playerPos = MAtmosUtility.getPlayerPosition();

		Chunk chunk = mc.theWorld.getChunkFromBlockCoords(playerPos);
        // dag edit getBiomeGenForWorldCoords(..) -> getBiome(..)
		return chunk.getBiome(playerPos, mc.theWorld.getBiomeProvider());
	}
}
