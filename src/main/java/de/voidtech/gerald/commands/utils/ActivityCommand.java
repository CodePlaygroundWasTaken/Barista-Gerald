package main.java.de.voidtech.gerald.commands.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

import main.java.de.voidtech.gerald.GlobalConstants;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.entities.GlobalConfig;
import main.java.de.voidtech.gerald.service.DatabaseService;
import main.java.de.voidtech.gerald.service.GlobalConfigService;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.entities.EntityBuilder;

public class ActivityCommand extends AbstractCommand {
	private GlobalConfigService globalConfService = GlobalConfigService.getInstance();
	private DatabaseService dbService = DatabaseService.getInstance();

	@Override
	public void executeInternal(Message message, List<String> args) 
	{
		if (StringUtils.join(args.toArray(), " ").length() > 128) message.getChannel().sendMessage("Too many characters! The activity can only be 128 letters").queue();
		else {
			ActivityWrapper activityWrapperOpt = ActivityWrapper
					.getActivityWrapperOpt(StringUtils.join(args.toArray(), " "));

			if (activityWrapperOpt != null) {
				String statusMessage = StringUtils.join(args.toArray(), " ").substring(activityWrapperOpt.toString().length());
				message.getJDA().getPresence().setActivity(EntityBuilder.createActivity(statusMessage,GlobalConstants.STREAM_URL, activityWrapperOpt.getActivityType()));
				
				updatePersistentActivity(activityWrapperOpt.getActivityType(), statusMessage);

			} else {
				message.getChannel().sendMessage("Please provide a valid activity: `playing, watching, listening to, streaming`").queue();
			}
		}
	}
	
	@Override
	public String getDescription() {
		return "sets the activity of the Barista";
	}

	@Override
	public String getUsage() {
		return "activity {activity} {message}";
	}
	
	private void updatePersistentActivity(ActivityType activityType, String status)
	{
		try(Session session = dbService.getSessionFactory().openSession())
		{
			session.getTransaction().begin();
			
			GlobalConfig globalConf = globalConfService.getGlobalConfig();
			globalConf.setActivity(activityType);
			globalConf.setStatus(status);
			
			session.saveOrUpdate(globalConf);
			session.getTransaction().commit();
		}
	}
	
	/**
	 * 
	 * Do not, under no circumstances, change the order of this enum
	 * Except if you know what you're doing
	 */
	private enum ActivityWrapper
	{
		DEFAULT("playing"),
		STREAMING("streaming"),
		LISTENING("listening to"),
		WATCHING("watching");
		
		private String humanReadable;
		
		ActivityWrapper(String humanReadable)
		{
			this.humanReadable = humanReadable;
		}
		
		@Override
		public String toString()
		{
			return this.humanReadable;
		}
		
		public ActivityType getActivityType()
		{
			return ActivityType.values()[this.ordinal()];
		}
		
		public static ActivityWrapper getActivityWrapperOpt(String activity)
		{
			if(activity.startsWith("listening to")) return ActivityWrapper.LISTENING;
			else if(activity.startsWith("watching")) return ActivityWrapper.WATCHING;
			else if(activity.startsWith("streaming")) return ActivityWrapper.STREAMING;
			else if(activity.startsWith("playing")) return ActivityWrapper.DEFAULT;
			return null;
		}
	}

}
