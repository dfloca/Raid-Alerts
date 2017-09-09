import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.events.message.priv.*;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Random;

public class RaidAlerts extends ListenerAdapter{

	public static void main(String[] args) {
		//construct builder for a BOT account
		//if CLIENT account wanted, use AccountType.CLIENT
		try{
			JDA jda = new JDABuilder(AccountType.BOT)
					.setToken("MzU0Mzg0ODc4MTUxOTI1NzYx.DI9fyQ.D09nF1D97pp-gyF3O1uKVxOn5bU") 	//token of the account that is logging in
					.addEventListener(new RaidAlerts()) //instance of a class that will handle events
					.buildBlocking();					//2 ways to login: blocking vs async. Blocking guarantees that JDA will be completely loaded
		}
		catch(LoginException ex){
			//if anything goes wrong in terms of auth, this is the exception that will represent it
			ex.printStackTrace();
		}
		catch(InterruptedException ex){
			ex.printStackTrace();
		}
		catch(RateLimitedException ex){
			ex.printStackTrace();
		}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event){
		JDA jda = event.getJDA();
		long responseNumber = event.getResponseNumber();
		
		User author = event.getAuthor(); //user that sent the message
		Message message = event.getMessage(); //message that was received
		MessageChannel channel = event.getChannel(); 
		
		String msg = message.getContent(); //returns readable version of the message
		User me = jda.getUserById(141738733031522304L);
		
		//User user;
		String checkStr = "Pink Ball Bollards Art";
		String checkStr2 = "Forest Court";
		String checkStr3 = "Sunningdale Rd E";
		
		boolean bot = author.isBot(); //determines if message sender is bot or not
		
		if(event.isFromType(ChannelType.TEXT)){
			Guild guild = event.getGuild();
			TextChannel textChannel = event.getTextChannel();
			Member member = event.getMember();
			
			String name;
			if(message.isWebhookMessage()){
				name = author.getName();
			}
			else{
				name = member.getEffectiveName(); //nickname, otherwise username
			}
			
			System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
		}
		else if(event.isFromType(ChannelType.PRIVATE)){
			//message was sent from a private channel
			PrivateChannel privateChannel = event.getPrivateChannel();
			
			System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
		}
		else if(event.isFromType(ChannelType.GROUP)){
			//Groups are CLIENT only
			Group group = event.getGroup();
			String groupName = group.getName() != null ? group.getName() : ""; //group names can be null
			
			System.out.printf("[GRP: %s]<%s>: %s\n", groupName, author.getName(), msg);
		}
		
		if(msg.equals("@ping")){
			//This will send a message, "pong!" by constructing a RestAction and "queueing" the action with the Requster
			//By calling queue(), we send the Request to the Requester which will send it to discord. Using queue() or any
			//of its different forms will handle ratelimiting for you automatically
			channel.sendMessage("pong").queue();
		}
		else if(msg.equals("@roll")){
			//In this case, we have an example showing how to use the Success consumer for a RestAction. The Success consumer
            // will provide you with the object that results after you execute your RestAction. As a note, not all RestActions
            // have object returns and will instead have Void returns. You can still use the success consumer to determine when
            // the action has been completed!
			Random rand = new Random();
			int roll = rand.nextInt(6) + 1;
			channel.sendMessage("Your roll: " + roll).queue(sentMessage ->{
				if(roll < 3){
					channel.sendMessage("The roll for messageId: " + sentMessage.getId() + " wasn't very good... Must be bad luck!\n").queue();
				}
			});
		}
		/*else if(msg.startsWith("!kick")){
			//This is an admin command. That means that it requires specific permissions to use it, in this case
            // it needs Permission.KICK_MEMBERS. We will have a check before we attempt to kick members to see
            // if the logged in account actually has the permission, but considering something could change after our
            // check we should also take into account the possibility that we don't have permission anymore, thus Discord
            // response with a permission failure!
            //We will use the error consumer, the second parameter in queue!
			
			//we only want to deal with message sent in a Guild
			if(message.isFromType(ChannelType.TEXT)){
				//if no users are provided, can't kick anyone
				if(message.getMentionedUsers().isEmpty()){
					channel.sendMessage("You must mention 1 or more Users to be kicked!").queue();
				}
				else{
					Guild guild = event.getGuild();
					Member selfMember = guild.getSelfMember(); //currently logged in account's Member object
					
					//if logged in account doesn't have kick permissions, don't kick
					if(!selfMember.hasPermission(Permission.KICK_MEMBERS)){
						channel.sendMessage("Sorry! I don't have permission to kick members in this Guild!").queue();
						return;
					}
					
					//loop over all mentioned users, kicking them one at a time
					List<User> mentionedUsers = message.getMentionedUsers();
					for(User user : mentionedUsers){
						Member member = guild.getMember(user); //get the member object for each mentioned user to kick them
						
						//make sure that we can interact with them. Interacting with a Member means you are higher 
						//in the role hierarchy than they are. No one is above the Guild's Owner
						if(!selfMember.canInteract(member)){
							channel.sendMessage("Cannot kick member: " + member.getEffectiveName() + ", they are higher in the hierarchy than I am!").queue();
							continue;
						}
						
						//using queue means we never have to deal with RateLimits
						//JDA does all the work
						guild.getController().kick(member).queue(
								success -> channel.sendMessage("Kicked " + member.getEffectiveName() + "! Cya!").queue(),
								error -> {
									//The failure consumer provides a throwable. In this case we want to check for a PermissionException
									if(error instanceof PermissionException){
										PermissionException pe = (PermissionException) error;
										Permission missingPermission = pe.getPermission(); //get exactly which permission is missing
										
										channel.sendMessage("PermissionError kicking [" + member.getEffectiveName() + "]: " + error.getMessage()).queue();
									}
									else{
										channel.sendMessage("Unknown error while kicking [" + member.getEffectiveName() + "]: < " + error.getClass().getSimpleName() + ">: " + error.getMessage()).queue();
									}
								}
						);
					}
				}				
			}
			else{
				channel.sendMessage("This is a Guild-Only command!").queue();
			}
		}*/
		/*else if(msg.equals("!block")){
			try{
				//complete returns the Message object
				//The complete() overload queues the Message for execution and will return when the message was sent
				//Handles rate limits automatically
				Message sentMessage = channel.sendMessage("I blocked and will return the message!").complete();
				//Should only be used if you are expecting to handle rate limits yourself
				//The completion will not succeed if a rate limit is breached and throw a RateLimitException
				Message sentRatelimitMessage = channel.sendMessage("I expect rate limitation and know how to handle it!").complete(false);
			}
			catch(RateLimitedException ex){
				System.out.println("Whoops! Got ratelimited when attempting to use a .complete() on a RestAction! Retry After: " + ex.getRetryAfter());
			}
			//Note tht RateLimitException is the only checked-exception thrown by .complete()
			catch(RuntimeException ex){
				System.out.println("Unfortunately something went wrong when we tried to send the Message and .complete() threw an Exception.");
				ex.printStackTrace();
			}
		}*/
		else if(msg.toLowerCase().contains(checkStr3.toLowerCase())){
			try{
				if(channel.equals(me.openPrivateChannel())){
					System.out.println("This is already my private channel!");
					return;
				}
				else{
					me.openPrivateChannel().complete().sendMessage("RoWOW found a raid! \n" /*+ msg*/).queue();
				}
				
			}
			catch(RuntimeException ex){
				System.out.println("Unfortunately something went wrong when we tried to send the Message and .complete() threw an Exception.");
				ex.printStackTrace();
			}
		}
		else if(msg.equals("@help")){
			System.out.println("Use '@ping` to receive a pong \n Use `@roll` to roll dice \n Use @stop to unsummon me");
		}
		else if(msg.equals("@stop")){
			System.exit(0);
		}
	}

}
