package main.java.de.voidtech.gerald.commands;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;

public class SayCommand extends AbstractCommand{

	@Override
	public void execute(Message message, List<String> args) {
		String msg = String.join(" ", args);
		message.getChannel().sendMessage(msg).queue();		
	}

}