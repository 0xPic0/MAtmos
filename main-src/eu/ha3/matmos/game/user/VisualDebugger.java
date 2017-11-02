package eu.ha3.matmos.game.user;

import eu.ha3.matmos.engine.core.implem.LongFloatSimplificator;
import eu.ha3.matmos.engine.core.interfaces.Sheet;
import eu.ha3.matmos.game.data.ModularDataGatherer;
import eu.ha3.matmos.game.data.abstractions.scanner.Progress;
import eu.ha3.matmos.game.data.abstractions.scanner.ScannerModule;
import eu.ha3.matmos.game.mod.MAtMod;
import eu.ha3.mc.haddon.supporting.SupportsFrameEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

/*
--filenotes-placeholder
*/

public class VisualDebugger implements SupportsFrameEvents
{
	private final MAtMod mod;
	private final ModularDataGatherer dataGatherer;
	
	private DebugMode mode = DebugMode.NONE;
	private VisualExpansionDebugging ed;
	private String scanDebug;
	private boolean deltas = false;
	
	public VisualDebugger(MAtMod mod, ModularDataGatherer dataGatherer)
	{
		this.mod = mod;
		this.dataGatherer = dataGatherer;
	}
	
	public List<String> obtainSheetNamesCopy()
	{
		List<String> sheetNames = new ArrayList<>(this.dataGatherer.getData().getSheetNames());
		Collections.sort(sheetNames);
		return sheetNames;
	}
	
	public void debugModeExpansion(VisualExpansionDebugging ed)
	{
		this.ed = ed;
		this.mode = DebugMode.EXPANSION;
	}
	
	public void debugModeScan(String name)
	{
		this.scanDebug = name;
		this.mode = DebugMode.SCAN;
	}
	
	public void noDebug()
	{
		this.mode = DebugMode.NONE;
	}
	
	@Override
	public void onFrame(float semi)
	{
		if (this.mod.isDebugMode())
		{
			this.mod.util().prepareDrawString();
			this.mod.util().drawString(
                    TextFormatting.GRAY.toString() + this.mod.getLag().getMilliseconds() + "ms", 1f, 1f, 0, 0, '3', 0, 0, 0,
				0, true);
		}
		
		if (this.mode == DebugMode.NONE)
			return;
		
		if (this.mode == DebugMode.EXPANSION
			&& semi >= 0f && this.mod.util().getCurrentScreen() != null
			&& !(this.mod.util().getCurrentScreen() instanceof GuiChat))
			return;
		
		switch (this.mode)
		{
		case SCAN:
			debugScan();
			break;
		case EXPANSION:
			this.ed.onFrame(semi);
			break;
		default:
			break;
		}
		
	}
	
	private void debugScan()
	{
		debugScanWithSheet(this.dataGatherer.getData().getSheet(this.scanDebug), false);
		if (this.deltas && this.dataGatherer.getData().getSheetNames().contains(this.scanDebug + "_delta"))
		{
			debugScanWithSheet(this.dataGatherer.getData().getSheet(this.scanDebug + "_delta"), true);
		}
	}
	
	private void debugScanWithSheet( final Sheet sheet, boolean isDeltaPass )
	{
		Minecraft mc = Minecraft.getMinecraft();
		int fac = new ScaledResolution(mc).getScaleFactor();
		float scale = 2f / fac;
		GL11.glPushMatrix();
		GL11.glScalef(scale, scale, 1.0F);
		
		final int ALL = 50;
		
		List<String> sort = new ArrayList<>(sheet.keySet());
		
		if ( scanDebug.startsWith("scan_") )
		{
			try
			{
				Collections.sort(sort, (String o1, String o2) -> 
				{
					Long l1 = LongFloatSimplificator.longOf(sheet.get(o1));
					Long l2 = LongFloatSimplificator.longOf(sheet.get(o2));
					
					if (l1 == null && l2 == null)
						return o1.compareTo(o2);
					else if (l1 == null)
						return -1;
					else if (l2 == null)
						return 1;
					
					if (l1 > l2)
						return 1;
					else if (l1 < l2)
						return -1;
					else
						return o1.compareTo(o2);
				});
				Collections.reverse(sort);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
		}
		
		int total = 0;
		for (String index : sort)
		{
			if (!index.contains("^"))
			{
				Long l = LongFloatSimplificator.longOf(sheet.get(index));
				if (l != null)
				{
					total = total + (int) (long) l;
				}
			}
		}
		
		FontRenderer fontRenderer = mc.fontRenderer;
		
		int lineNumber = 0;
		
		if (this.scanDebug.startsWith("scan_large"))
		{
			Progress progressObject = this.dataGatherer.getLargeScanProgress();
			float progress = (float) progressObject.getProgress_Current() / progressObject.getProgress_Total();
			
			fontRenderer.drawStringWithShadow(
				"Scan ["
					+ mc.world.getHeight() + "]: " + StringUtils.repeat("|", (int) (100 * progress)) + " ("
					+ (int) (progress * 100) + "%)", 20, 2 + 9 * lineNumber, 0xFFFFCC);
		}
		
		lineNumber = lineNumber + 1;
		
		int leftAlign = 2 + (isDeltaPass ? 300 : 0);
		
		for (String index : sort)
		{
			if (lineNumber <= 100 && !index.contains("^"))
			{
				if (this.scanDebug.startsWith("scan_") || this.scanDebug.equals("block_contact"))
				{
					Long count = LongFloatSimplificator.longOf(sheet.get(index));
					if (count != null)
					{
						if (count > 0)
						{
							float scalar = (float) count / total;
							String percentage =
								!this.scanDebug.endsWith(ScannerModule.THOUSAND_SUFFIX) ? Float.toString(Math
									.round(scalar * 1000f) / 10f) : Integer.toString(Math.round(scalar * 100f));
							if (percentage.equals("0.0"))
							{
								percentage = "0";
							}
							
							int fill = Math.round(scalar * ALL * 2 /* * 2*/);
							int superFill = 0;
							if (fill > ALL * 2)
							{
								fill = ALL * 2;
							}
							if (fill > ALL)
							{
								superFill = fill - ALL;
							}
							
							String bars = "";
							if (superFill > 0)
							{
								bars = bars + TextFormatting.YELLOW + StringUtils.repeat("|", superFill);
							}
							bars =
								bars
									+ TextFormatting.RESET
									+ StringUtils.repeat("|", fill - superFill * 2);
							
							if (index.startsWith("minecraft:"))
							{
								index = index.substring(10);
							}
							
							fontRenderer.drawStringWithShadow(bars
								+ (fill == ALL * 2
									? TextFormatting.YELLOW + "++" + TextFormatting.RESET : "") + " ("
								+ count + ", " + percentage + "%) " + index, leftAlign, 2 + 9 * lineNumber, 0xFFFFFF);
							lineNumber = lineNumber + 1;
						}
					}
				}
				else if ( scanDebug.startsWith("detect_") )
				{
					String val = sheet.get(index);
					if ( !val.equals("0") && !val.equals(Integer.toString(Integer.MAX_VALUE)) )
					{
						if ( !index.equals("0") )
						{
							Entity ent = mc.world.getEntityByID(Integer.parseInt(index));
							String entName = ( ent == null ? "Unknown entity: ".concat(index) : 
									EntityList.getEntityString(ent)
											.concat(": ")
											.concat(ent.getName()) );
							
							fontRenderer.drawStringWithShadow( 
									index
										+ " ("
										+ entName
										+ "): " 
										+ val,
									leftAlign,
									2 + 9 * lineNumber,
									0xFFFFFF );
						}
						else
						{
							fontRenderer.drawStringWithShadow(
								index + " (Player): " + val, leftAlign, 2 + 9 * lineNumber, 0xFFFFFF);
						}
						
						lineNumber = lineNumber + 1;
					}
				}
				else
				{
					String val = sheet.get(index);
					int color = 0xFFFFFF;
					if (val.equals("0"))
					{
						color = 0xFF0000;
					}
					else if (val.equals("1"))
					{
						color = 0x0099FF;
					}
					fontRenderer.drawStringWithShadow(index + ": " + val, leftAlign, 2 + 9 * lineNumber, color);
					lineNumber = lineNumber + 1;
				}
			}
		}
		GL11.glPopMatrix();
	}
	
	public void toggleDeltas()
	{
		this.deltas = !this.deltas;
	}
	
	private enum DebugMode
	{
		NONE, SCAN, EXPANSION;
	}	
}