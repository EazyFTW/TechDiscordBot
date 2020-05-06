package me.TechsCode.TechDiscordBot.module.modules;

import me.TechsCode.TechDiscordBot.TechDiscordBot;
import me.TechsCode.TechDiscordBot.module.Module;
import me.TechsCode.TechDiscordBot.objects.DefinedQuery;
import me.TechsCode.TechDiscordBot.objects.Query;
import me.TechsCode.TechDiscordBot.objects.Requirement;
import me.TechsCode.TechDiscordBot.util.TechEmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class StaffEmbedModule extends Module {

    private final DefinedQuery<Role> STAFF_ROLE = new DefinedQuery<Role>() {
        @Override
        protected Query newQuery() {
            return bot.getRoles("Staff");
        }
    };

    public StaffEmbedModule(TechDiscordBot bot) {
        super(bot);
    }

    @SubscribeEvent
    public void receive(GuildMessageReceivedEvent e) {
        if(e.getMember() == null) return;
        if(e.getAuthor().isBot()) return;
        if(!e.getMember().getRoles().contains(STAFF_ROLE.query().first())) return;

        String message = e.getMessage().getContentDisplay();

        if(message.startsWith("^^ ") && message.endsWith(" ^^")) {
            e.getMessage().delete().complete();

            String text = message.substring(3, message.length()-3);
            e.getChannel().sendMessage(text).complete();
            return;
        }

        if(message.startsWith("^ ")) {
            e.getMessage().delete().complete();

            String text = message.substring(2);
            String[] arguments = text.split("\\^");

            if(arguments.length != 2 && arguments.length != 3) {
                new TechEmbedBuilder("Invalid Arguments").setText("Usage: ^ Title ^ Message ^ (Optional) Thumbnail ^").error().sendTemporary(e.getChannel(), 5);
                return;
            }

            if(arguments.length == 3) {
                new TechEmbedBuilder(arguments[0])
                        .setFooter("Posted by " + e.getAuthor().getName())
                        .setText(arguments[1])
                        .setThumbnail(arguments[2])
                        .send(e.getChannel());
            } else {
                new TechEmbedBuilder(arguments[0])
                        .setFooter("Posted by " + e.getAuthor().getName())
                        .setText(arguments[1])
                        .send(e.getChannel());
            }
        }
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public String getName() { return "Staff Embed"; }

    @Override
    public Requirement[] getRequirements() {
        return new Requirement[] {
                new Requirement(STAFF_ROLE, 1, "Missing 'Staff' Role")
        };
    }
}