package main.java.de.voidtech.gerald.commands.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.entities.Tunnel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Command
public class TunnelCommand extends AbstractCommand {
	
	@Autowired
	private EventWaiter waiter;
	@Autowired
	private SessionFactory sessionFactory;

	private void fillTunnel(Message message) {
		if (tunnelExists(message.getChannel().getId())) {
			message.getChannel().sendMessage("Filling tunnel...").queue(sentMessage -> {
				Tunnel tunnel = getTunnel(message.getChannel().getId());
				
				String sourceChannel = tunnel.getSourceChannel();
				String destinationChannel = tunnel.getDestChannel();
				
				if (sourceChannel.equals(message.getChannel().getId())) {
					message.getJDA().getTextChannelById(destinationChannel).sendMessage("**This tunnel has been filled.**").queue();
				} else {
					message.getJDA().getTextChannelById(sourceChannel).sendMessage("**This tunnel has been filled.**").queue();
				}
				deleteTunnel(message.getChannel().getId());
				sentMessage.editMessage("**This tunnel has been filled.**").queue();
			});
		}
	}
	
	private void sendChannelVerificationRequest(TextChannel targetChannel, Message originChannelMessage) {
		targetChannel.sendMessage("**Incoming tunnel request from " +
				originChannelMessage.getGuild().getName() + " > " + originChannelMessage.getChannel().getName() +
				"**\nSay 'accept' within 15 seconds to allow this tunnel to be dug!").queue();
		
		waiter.waitForEvent(MessageReceivedEvent.class,
				event -> tunnelAcceptedStatement(((MessageReceivedEvent) event), targetChannel),
				event -> {
					boolean allowTunnel = event.getMessage().getContentRaw().toLowerCase().equals("accept");
					if (allowTunnel) {
						digTunnel(targetChannel, originChannelMessage.getChannel());
					} else {
						denyTunnel(originChannelMessage);
					}
				}, 30, TimeUnit.SECONDS, 
				() -> {
					originChannelMessage.getChannel().sendMessage("**Request timed out**").queue();
				});
	}
	
	private boolean tunnelAcceptedStatement(MessageReceivedEvent event, TextChannel targetChannel) {
		
		return (event.getMessage().getContentRaw().toLowerCase().equals("accept")
			&& (event.getAuthor().getId() != event.getJDA().getSelfUser().getId())
			&& event.getChannel().getId().equals(targetChannel.getId()));

	}
	
	private void denyTunnel(Message originChannelMessage) {
		originChannelMessage.getChannel().sendMessage("**Tunnel request denied**").queue();		
	}

	private void digTunnel(TextChannel targetChannel, MessageChannel originChannel) {
		targetChannel.sendMessage("**Tunnel Dug**").queue();
		originChannel.sendMessage("**Tunnel Dug**").queue();
		
		writeTunnelPair(originChannel.getId(), targetChannel.getId());
	}
	
	private void writeTunnelPair(String sourceChannelID, String destChannelID)
	{
		try(Session session = sessionFactory.openSession())
		{
			session.getTransaction().begin();
			
			Tunnel tunnel = new Tunnel(sourceChannelID, destChannelID);
			
			tunnel.setSourceChannel(sourceChannelID);
			tunnel.setDestChannel(destChannelID);
			
			session.saveOrUpdate(tunnel);
			session.getTransaction().commit();
		}
	}
	
	private boolean tunnelExists(String senderChannelID) {
		
		try(Session session = sessionFactory.openSession())
		{
			Tunnel tunnel = (Tunnel) session.createQuery("FROM Tunnel WHERE sourceChannelID = :senderChannelID OR destChannelID = :senderChannelID")
                    .setParameter("senderChannelID", senderChannelID)
                    .uniqueResult();
			return tunnel != null;
		}
	}
	
	private Tunnel getTunnel(String senderChannelID) {
		
		try(Session session = sessionFactory.openSession())
		{
			Tunnel tunnel = (Tunnel) session.createQuery("FROM Tunnel WHERE sourceChannelID = :senderChannelID OR destChannelID = :senderChannelID")
                    .setParameter("senderChannelID", senderChannelID)
                    .uniqueResult();
			return tunnel;
		}
	}
	
	private void deleteTunnel(String senderChannelID) {
		try(Session session = sessionFactory.openSession())
		{
			session.getTransaction().begin();
			session.createQuery("DELETE FROM Tunnel WHERE sourceChannelID = :senderChannelID OR destChannelID = :senderChannelID")
				.setParameter("senderChannelID", senderChannelID)
				.executeUpdate();
			session.getTransaction().commit();
		}
	}
	
	@Override
	public void executeInternal(Message message, List<String> args) 
	{	
		if(!message.getMember().hasPermission(Permission.ADMINISTRATOR)) return;
		
		if (args.get(0).equals("fill")) {
			fillTunnel(message);
		} else {
			String targetChannelID = args.get(0);
			TextChannel targetChannel = message.getJDA().getTextChannelCache().getElementById(targetChannelID);
	
			if (targetChannel == null) {
				message.getChannel().sendMessage("**That channel could not be found!**").queue();
			
			} else if (targetChannel.getId().equals(message.getChannel().getId())) {
					message.getChannel().sendMessage("**You can't dig a tunnel here!**").queue();
			
			} else {
				if (tunnelExists(message.getChannel().getId())) {
					message.getChannel().sendMessage("**There is already a tunnel here!**").queue();
				} else {
					sendChannelVerificationRequest(targetChannel, message);	
				}
			}	
		}
	}	

	@Override
	public String getDescription() {
		return "Tunnels allow you to form a bridge between two servers/channels! Using this command, Gerald will forward all text messages between the chosen channels";
	}

	@Override
	public String getUsage() {
		return "To create a tunnel: tunnel [channel ID]\nTo destroy a tunnel: tunnel fill";
	}

	@Override
	public String getName() {
		return "tunnel";
	}

}