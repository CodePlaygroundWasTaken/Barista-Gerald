package main.java.de.voidtech.gerald.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

import main.java.de.voidtech.gerald.service.GeraldConfig;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class ReadyListener implements EventListener {
	
	private static final Logger LOGGER = Logger.getLogger(GeraldConfig.class.getName());
	
	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof ReadyEvent)
			LOGGER.log(Level.INFO, "Coffee Machine is ready");
	}

}
