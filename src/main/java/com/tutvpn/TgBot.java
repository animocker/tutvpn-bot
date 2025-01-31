package com.tutvpn;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethodMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Component
public class TgBot extends AbilityBot {

    public TgBot(@Value("${BOT_TOKEN}") String token) {
        super(token, "TutVPN");
    }

    @Override
    public long creatorId() {
        return 1;
    }

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info("Start command")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> sendMessage(ctx, "Hello!"))
                .build();
    }

    @SneakyThrows
    private void sendMessage(MessageContext messageContext, String text) {
        sender.execute(SendMessage.builder().chatId(messageContext.chatId()).text(text+messageContext.user()).build());
    }
}
