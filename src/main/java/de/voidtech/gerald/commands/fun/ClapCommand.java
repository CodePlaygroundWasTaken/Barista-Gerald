package main.java.de.voidtech.gerald.commands.fun;

import java.util.List;

import main.java.de.voidtech.gerald.commands.AbstractCommand;
import net.dv8tion.jda.api.entities.Message;

public class ClapCommand extends AbstractCommand {

	@Override
	public void executeInternal(Message message, List<String> args) {
		String finalMessage = "👏" + String.join("👏", args) + "👏";
		message.getChannel().sendMessage(finalMessage).queue();
	}

	@Override
	public String getDescription() {
		return "👏does👏this👏to👏your👏messages👏";
	}

	@Override
	public String getUsage() {
		return "clap a very normal message";
	}
}
