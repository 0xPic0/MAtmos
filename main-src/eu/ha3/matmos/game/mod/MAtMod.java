package eu.ha3.matmos.game.mod;

import com.google.common.base.Optional;
import eu.ha3.easy.StopWatchStatistic;
import eu.ha3.easy.TimeStatistic;
import eu.ha3.matmos.expansions.Expansion;
import eu.ha3.matmos.expansions.Stable;
import eu.ha3.matmos.expansions.volume.VolumeUpdatable;
import eu.ha3.matmos.game.system.MAtmosUtility;
import eu.ha3.matmos.game.system.Simulacrum;
import eu.ha3.matmos.game.system.SoundAccessor;
import eu.ha3.matmos.game.user.UserControl;
import eu.ha3.matmos.game.user.VisualDebugger;
import eu.ha3.matmos.log.MAtLog;
import eu.ha3.matmos.pluggable.PluggableIntoMinecraft;
import eu.ha3.mc.haddon.Identity;
import eu.ha3.mc.haddon.OperatorCaster;
import eu.ha3.mc.haddon.PrivateAccessException;
import eu.ha3.mc.haddon.implem.HaddonIdentity;
import eu.ha3.mc.haddon.implem.HaddonImpl;
import eu.ha3.mc.haddon.supporting.SupportsFrameEvents;
import eu.ha3.mc.haddon.supporting.SupportsTickEvents;
import eu.ha3.mc.quick.chat.Chatter;
import eu.ha3.mc.quick.update.NotifiableHaddon;
import eu.ha3.mc.quick.update.UpdateNotifier;
import eu.ha3.util.property.simple.ConfigProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextFormatting;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.util.*;

/* x-placeholder */

public class MAtMod extends HaddonImpl implements SupportsFrameEvents, SupportsTickEvents, NotifiableHaddon, IResourceManagerReloadListener, SoundAccessor, Stable {
	private static final boolean _COMPILE_IS_UNSTABLE = true;
	
	// Identity
	protected final String NAME = "MAtmos";
	protected final int VERSION = 37;
	protected final String FOR = "1.12";
	protected final String ADDRESS = "http://matmos.ha3.eu";
	protected final Identity identity = new HaddonIdentity(NAME, VERSION, FOR, ADDRESS);
	
	// NotifiableHaddon and UpdateNotifier
	private final ConfigProperty config = new ConfigProperty();
	private final Chatter chatter = new Chatter(this, "<MAtmos> ");
	private final UpdateNotifier updateNotifier = new UpdateNotifier(this, "http://q.mc.ha3.eu/query/matmos-main-version-vn.json?ver=%d");
	
	// State
	private boolean isListenerInstalled; 
	private Optional<Simulacrum> simulacrum = Optional.absent();
	private boolean isUnderwaterMode;
	
	// Components
	private UserControl userControl;
	
	// Use once
	private boolean hasFirstTickPassed;
	
	// Debug
	private StopWatchStatistic timeStat = new StopWatchStatistic();
	
	// Debug queue
	private Object queueLock = new Object();
	private List<Runnable> queue = new ArrayList<Runnable>();
	private boolean hasResourcePacks_FixMe;
	
	public MAtMod() {
		MAtLog.setRefinedness(MAtLog.INFO);
	}
	
	@Override
	public void onLoad() {
		util().registerPrivateGetter("getSoundManager", SoundHandler.class, 5, "sndManager", "field_147694_f", "f");
		util().registerPrivateGetter("getSoundSystem", SoundManager.class, 3, "sndHandler", "field_148622_c", "d");
		
		util().registerPrivateGetter("isInWeb", Entity.class, 30, "isInWeb", "field_70134_J", "E");
		
		((OperatorCaster) op()).setTickEnabled(true);
		((OperatorCaster) op()).setFrameEnabled(true);
		
		TimeStatistic timeMeasure = new TimeStatistic(Locale.ENGLISH);
		userControl = new UserControl(this);

		// Create default configuration
		updateNotifier.fillDefaults(config);
		config.setProperty("world.height", 256);
		config.setProperty("dump.sheets.enabled", false);
		config.setProperty("start.enabled", true);
		config.setProperty("reversed.controls", false);
		config.setProperty("sound.autopreview", true);
		config.setProperty("globalvolume.scale", 1f);
		config.setProperty("key.code", 65);
		config.setProperty("useroptions.altitudes.high", true);
		config.setProperty("useroptions.altitudes.low", true);
		config.setProperty("useroptions.biome.override", -1);
		config.setProperty("debug.mode", 0);
		config.setProperty("minecraftsound.ambient.volume", 1f);
		config.setProperty("version.last", VERSION);
		config.setProperty("version.warnunstable", 3);
		config.commit();
		
		// Load configuration from source
		try {
			config.setSource(new File(util().getMcFolder(), "matmos/userconfig.cfg").getCanonicalPath());
			config.load();
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error caused config not to work: " + e.getMessage());
		}
		
		resetAmbientVolume();
		
		updateNotifier.loadConfig(config);
		
		// This registers stuff to Minecraft (key bindings...)
		userControl.load();
		
		MAtLog.info("Took " + timeMeasure.getSecondsAsString(3) + " seconds to setup MAtmos base.");
		if (config.getBoolean("start.enabled")) start();
	}
	
	private void resetAmbientVolume() {
		setSoundLevelAmbient(config.getFloat("minecraftsound.ambient.volume"));
	}
	
	private void overrideAmbientVolume() {
		if (config.getFloat("minecraftsound.ambient.volume") <= 0) return;
		setSoundLevelAmbient(0.01f);
	}
	
	private void setSoundLevelAmbient(float level) {
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		// For some reason it has to be set twice to validate it (???!)
		settings.setSoundLevel(SoundCategory.AMBIENT, level);
		settings.setSoundLevel(SoundCategory.AMBIENT, level);
	}
	
	public void start() {
		if (!isListenerInstalled) {
			isListenerInstalled = true;
			util().getClient().registerReloadListener(this);
		}
		refresh();
	}

	public void refresh() {
		deactivate();
		activate();
	}
	
	public boolean isActivated() {
		return simulacrum.isPresent();
	}

	@Override
	public void activate() {
		if (isActivated()) return;
		MAtLog.fine("Loading...");
		simulacrum = Optional.of(new Simulacrum(this));
		MAtLog.fine("Loaded.");
	}
	
	@Override
	public void deactivate() {
		if (!isActivated()) return;
		MAtLog.fine("Stopping...");
		simulacrum.get().dispose();
		simulacrum = Optional.absent();
		MAtLog.fine("Stopped.");
	}
	
	// Events
	
	@Override
	public void onFrame(float semi) {
		//Solly edit - only play sounds whilst the game is running (and not paused)
		if (!isActivated() || util().isGamePaused()) return;
		simulacrum.get().onFrame(semi);
		userControl.onFrame(semi);
	}
	
	@Override
	public void onTick() {
		userControl.onTick();
		if (isActivated()) {
			if (!queue.isEmpty()) {
				synchronized (queueLock) {
					while (!queue.isEmpty()) queue.remove(0).run();
				}
			}
			
			timeStat.reset();
			simulacrum.get().onTick();
			timeStat.stop();
			
			if (MAtmosUtility.isUnderwaterAnyGamemode()) {
				if (!isUnderwaterMode) {
					isUnderwaterMode = true;
					overrideAmbientVolume();
				}
			} else if (isUnderwaterMode) {
				isUnderwaterMode = false;
				resetAmbientVolume();
			}
		} else if (isUnderwaterMode) {
			isUnderwaterMode = false;
			resetAmbientVolume();
		}
		
		if (!hasFirstTickPassed) {
			hasFirstTickPassed = true;
			updateNotifier.attempt();
			if (_COMPILE_IS_UNSTABLE) {
				int lastVersion = config.getInteger("version.last");
				int warns = config.getInteger("version.warnunstable");
				if (lastVersion != VERSION){
					warns = 3;
					config.setProperty("version.last", VERSION);
				}
				if (warns > 0) {
					warns--;
					config.setProperty("version.warnunstable", warns);
					getChatter().printChat(
	                        TextFormatting.RED, "You are using an ", TextFormatting.YELLOW, "Unofficial Beta", TextFormatting.RED, " version of MAtmos.");
					getChatter().printChatShort("By using this version, you understand that this mod isn't intended for " +
	                        "actual game sessions, MAtmos may not work, might crash, the sound ambience is incomplete, etc. Use at your own risk. ");
					getChatter().printChatShort("Please check regularly for updates and resource pack updates.");
					if (warns > 0) getChatter().printChatShort("This message will appear ", TextFormatting.YELLOW, warns, " more times.");
				}
				if (config.commit()) config.save();
			}
			
			if (isDebugMode()) {
				getChatter().printChat(TextFormatting.GOLD, "Developer mode is enabled in the Advanced options.");
				getChatter().printChatShort("This affects performance. Your game may run slower.");
			}
			
			if (!simulacrum.get().hasResourcePacks()) {
				hasResourcePacks_FixMe = true;
				if (simulacrum.get().hasDisabledResourcePacks()) {
					chatter.printChat(TextFormatting.RED, "Resource Pack not enabled yet!");
					chatter.printChatShort(TextFormatting.WHITE, "You need to activate \"MAtmos Resource Pack\" in the Minecraft Options menu for it to run.");
				} else {
					chatter.printChat(TextFormatting.RED, "Resource Pack missing from resourcepacks/!");
					chatter.printChatShort(TextFormatting.WHITE,"You may have forgotten to put the Resource Pack file into your resourcepacks/ folder.");
				}
			}
		}

		if (isActivated() && hasResourcePacks_FixMe && simulacrum.get().hasResourcePacks()) {
			hasResourcePacks_FixMe = false;
			chatter.printChat(TextFormatting.GREEN, "It should work now!");
		}
	}
	
	@Override
	public void dispose() {
		if (isActivated()) simulacrum.get().dispose();
	}
	
	@Override
	public void interrupt() {
		if (isActivated()) simulacrum.get().interruptBrutally();
	}
	
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		MAtLog.warning("ResourceManager has changed. Unintended side-effects may happen.");
		interrupt();
		// Initiate hot reload
		if (isActivated()) {
			simulacrum.get().interruptBrutally();
			deactivate();
			activate();
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Expansion> getExpansionList() {
		if (isActivated()) return simulacrum.get().getExpansions();
		return (Map<String, Expansion>) Collections.EMPTY_MAP;
	}
	
	public boolean isInitialized() {
		return isListenerInstalled;
	}
	
	@Override
	public Identity getIdentity() {
		return identity;
	}
	
	@Override
	public Chatter getChatter() {
		return chatter;
	}
	
	@Override
	public ConfigProperty getConfig() {
		return config;
	}
	
	@Override
	public void saveConfig() {
		// If there were changes...
		if (config.commit()) {
			MAtLog.info("Saving configuration...");
			// Write changes on disk.
			config.save();
		}
	}
	
	@Override
	public SoundManager getSoundManager() {
		try {
			return (SoundManager) util().getPrivate(Minecraft.getMinecraft().getSoundHandler(), "getSoundManager");
		} catch (PrivateAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public SoundSystem getSoundSystem() {
		try {
			return (SoundSystem) util().getPrivate(getSoundManager(), "getSoundSystem");
		} catch (PrivateAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public VolumeUpdatable getGlobalVolumeControl() {
		return simulacrum.get().getGlobalVolumeControl();
	}
	
	public boolean hasResourcePacksLoaded() {
		return isActivated() && simulacrum.get().hasResourcePacks();
	}
	
	public boolean hasNonethelessResourcePacksInstalled() {
		return isActivated() && simulacrum.get().hasDisabledResourcePacks();
	}
	
	public void synchronize() {
		if (isActivated()) simulacrum.get().synchronize();
	}
	
	public void saveExpansions() {
		if (isActivated()) simulacrum.get().saveConfig();
	}
	
	public VisualDebugger getVisualDebugger() {
		// UNCHECKED!
		return simulacrum.get().getVisualDebugger();
	}
	
	public StopWatchStatistic getLag() {
		return timeStat;
	}
	
	public void queueForNextTick(Runnable runnable) {
		synchronized (queueLock) {
			queue.add(runnable);
		}
	}
	
	public boolean isDebugMode() {
		return config.getInteger("debug.mode") > 0;
	}
	
	public void changedDebugMode() {
		if (isDebugMode()) {
			getChatter().printChat(TextFormatting.GOLD, "Dev/Editor mode enabled.");
			getChatter().printChatShort("Enabling this mode may cause Minecraft to run slower.");
		} else {
			getChatter().printChat(TextFormatting.GOLD, "Dev/Editor mode disabled.");
		}
		refresh();
	}
	
	public boolean isEditorAvailable() {
		return util().isPresent("eu.ha3.matmos.editor.EditorMaster");
	}
	
	public Runnable instantiateRunnableEditor(PluggableIntoMinecraft pluggable) {
		return util().<Runnable>getInstantiator("eu.ha3.matmos.editor.EditorMaster", PluggableIntoMinecraft.class).instantiate(pluggable);
	}

	public Optional<Expansion> getExpansionEffort(String expansionName) {
		if (!isActivated() || !simulacrum.get().getExpansions().containsKey(expansionName)) return Optional.absent();
		return Optional.of(simulacrum.get().getExpansions().get(expansionName));
	}
}
