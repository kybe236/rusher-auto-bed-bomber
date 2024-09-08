package org.kybe;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class Main extends Plugin {
	
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("Hello World!");
		
		//creating and registering a new module
		final AutoBedBomb autoBedBomb = new AutoBedBomb();
		RusherHackAPI.getModuleManager().registerFeature(autoBedBomb);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("Example plugin unloaded!");
	}
	
}