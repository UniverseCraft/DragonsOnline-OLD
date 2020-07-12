package mc.dragons.core.events;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import mc.dragons.core.Dragons;
import mc.dragons.core.util.StringUtil;

public class WorldEventListeners implements Listener {
	private Logger LOGGER = Dragons.getInstance().getLogger();

	//@SuppressWarnings("deprecation")
	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		LOGGER.finest("Leaves decay event on " + event.getBlock().getType() + " at " + StringUtil.locToString(event.getBlock().getLocation()) + " [" + event.getBlock().getWorld().getName() + "]");
		//event.getBlock().setData((byte) (event.getBlock().getData() + 6));
		event.setCancelled(true);
	}
	
	@EventHandler
	public void onWeather(WeatherChangeEvent e) {
		LOGGER.finest("Weather change event in world " + e.getWorld().getName());
		e.setCancelled(e.toWeatherState());
	}
	
	/**
	 * Reverse-engineered from https://www.spigotmc.org/resources/anticroptrample.45465/
	 * @author Chaottiic
	 */
	@EventHandler
	public void onCropTrample(PlayerInteractEvent e) {
		LOGGER.finest("Player interact event in world " + e.getPlayer().getWorld().getName());
		if(e.getAction() == Action.PHYSICAL && e.getClickedBlock().getType() == Material.SOIL) {
			LOGGER.finest(" - It's a crop trample event! Cancelling.");
			e.setCancelled(true);
		}
	}
}
