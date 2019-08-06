package me.TechsCode.TechDiscordBot.modules;

import me.TechsCode.TechDiscordBot.*;
import me.TechsCode.TechDiscordBot.objects.DefinedQuery;
import me.TechsCode.TechDiscordBot.objects.Requirement;
import me.TechsCode.TechDiscordBot.util.CustomEmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TicketSystem extends Module {

    private final DefinedQuery<Role> SUPPORTER_ROLE = new DefinedQuery<Role>() {
        @Override
        protected Query<Role> newQuery() {
            return bot.getRoles("Supporter");
        }
    };

    private final DefinedQuery<Category> TICKET_CATEGORY = new DefinedQuery<Category>() {
        @Override
        protected Query<Category> newQuery() {
            return bot.getCategories("tickets");
        }
    };

    private final DefinedQuery<TextChannel> CREATION_CHANNEL = new DefinedQuery<TextChannel>() {
        @Override
        protected Query<TextChannel> newQuery() {
            return bot.getChannels("tickets");
        }
    };

    private Message lastInstructions;

    private String[] closeCommands = new String[]{"!solved", "!close", "-close"};

    public TicketSystem(TechDiscordBot bot) {
        super(bot);
    }

    @SubscribeEvent
    public void closeCommand(MessageReceivedEvent e){
        TextChannel channel = (TextChannel) e.getChannel();

        if(isTicketChat(channel)) {
            if(ArrayUtils.contains(closeCommands, e.getMessage().getContentDisplay().toLowerCase())){
                e.getMessage().delete().submit();

                boolean closedByUser = channel.getTopic().contains(e.getAuthor().getAsMention());

                new CustomEmbedBuilder("Ticket")
                        .setText(closedByUser ? "Thank you for contacting us "+e.getAuthor().getAsMention()+"! Consider writing a review if you enjoyed the support" : e.getAuthor().getAsMention()+" has closed this support ticket")
                        .send(channel);

                channel.delete().completeAfter(20, TimeUnit.SECONDS);

                if(closedByUser){
                    TextChannel creationChannel = CREATION_CHANNEL.query().first();

                    new CustomEmbedBuilder(closedByUser ? "Solved Ticket" : "Closed Ticket")
                            .setText("The ticket ("+channel.getName()+") from "+e.getAuthor().getAsMention()+" is now solved. Thanks for contacting us")
                            .success().send(creationChannel);

                    sendInstructions(creationChannel);
                }
            }
        }
    }

    @SubscribeEvent
    public void createChannel(MessageReceivedEvent e) {
        if(e.getMember().getUser().isBot()) return;

        TextChannel channel = (TextChannel) e.getChannel();
        TextChannel creationChannel = CREATION_CHANNEL.query().first();

        if(!channel.equals(creationChannel)) return;

        TextChannel ticketChat = getOpenTicketChat(e.getMember());

        e.getMessage().delete().complete();

        if(ticketChat != null){
            new CustomEmbedBuilder("Error")
                    .setText("You already have an open ticket ("+ticketChat.getAsMention()+")").error()
                    .sendTemporary(creationChannel, 10);

            return;
        }

        ticketChat = createTicketChannel(e.getMember());

        ticketChat.getManager().clearOverridesRemoved();
        ticketChat.getManager().clearOverridesAdded();

        Collection<Permission> permissionsAllow = new ArrayList<>();
        permissionsAllow.add(Permission.MESSAGE_ADD_REACTION);
        permissionsAllow.add(Permission.MESSAGE_ATTACH_FILES);
        permissionsAllow.add(Permission.MESSAGE_EMBED_LINKS);
        permissionsAllow.add(Permission.MESSAGE_READ);
        permissionsAllow.add(Permission.MESSAGE_WRITE);
        permissionsAllow.add(Permission.MESSAGE_HISTORY);

        ticketChat.getManager()
                .putPermissionOverride(SUPPORTER_ROLE.query().first(), permissionsAllow, Arrays.asList(Permission.MESSAGE_TTS))
                .putPermissionOverride(e.getMember(), permissionsAllow, Arrays.asList(Permission.MESSAGE_TTS))
                .putPermissionOverride(bot.getGuild().getPublicRole(), new ArrayList<>(), Arrays.asList(Permission.MESSAGE_READ, Permission.MESSAGE_WRITE))
                .complete();

        new CustomEmbedBuilder("Ticket Info")
                .setText(e.getMessage().getContentDisplay())
                .setFooter("Ticket created by "+e.getAuthor().getName())
                .send(ticketChat);

        new CustomEmbedBuilder("New Ticket")
                .setText(e.getAuthor().getAsMention()+" created a new ticket ("+ticketChat.getAsMention()+")")
                .send(creationChannel);
    }

    public void sendInstructions(TextChannel textChannel){
        if(lastInstructions != null){
            lastInstructions.delete().complete();
        }

        CustomEmbedBuilder howItWorksMessage = new CustomEmbedBuilder("How to create a ticket")
                .setText("You want to receive direct support from us? \nType in your question or issue below and we will get to you as soon as possible!");

        lastInstructions = howItWorksMessage.send(textChannel);
    }

    public boolean isTicketChat(TextChannel channel) {
        return channel.getName().contains("ticket-");
    }

    public TextChannel createTicketChannel(Member member) {
        String name = "ticket-"+ UUID.randomUUID().toString().split("-")[0];

        return (TextChannel) bot.getGuild().getController().createTextChannel(name)
                .setParent(TICKET_CATEGORY.query().first())
                .setTopic("Ticket from " + member.getAsMention() + " | Problem Solved? Please type in !solved")
                .complete();
    }

    public TextChannel getOpenTicketChat(Member member) {
        for(TextChannel channel : bot.getGuild().getTextChannels()) {
            if(isTicketChat(channel)){
                String topic = channel.getTopic();
                if(topic != null) {
                    if (topic.contains(member.getAsMention())) {
                        return channel;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onEnable() {
        sendInstructions(CREATION_CHANNEL.query().first());
    }

    @Override
    public void onDisable() {
        if(lastInstructions != null){
            lastInstructions.delete().complete();
        }
    }

    @Override
    public String getName() {
        return "Ticket System";
    }

    @Override
    public Requirement[] getRequirements() {
        return new Requirement[]{
                new Requirement(CREATION_CHANNEL, 1, "Missing Creation Channel (#tickets)"),
                new Requirement(TICKET_CATEGORY, 1, "Missing Tickets Category (tickets)"),
                new Requirement(SUPPORTER_ROLE, 1, "Missing 'Supporter' Role")
        };
    }
}