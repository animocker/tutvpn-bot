package com.tutvpn;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
public class TgBot extends AbilityBot {
    private static final String ACTIVATE_TRIAL = "Активировать пробный период";

    private final UserRepository userRepository;

    public TgBot(@Value("${BOT_TOKEN}") String token, UserRepository userRepository) {
        super(token, "TutVPN");
        this.userRepository = userRepository;
    }

    @Override
    public long creatorId() {
        return 256007482;
    }

    public Ability startAbility() {
        return Ability
                .builder()
                .name("start")
                .info("Start command")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> sendMessage(ctx.chatId(), "Hello!"))
                .build();
    }

    public Ability getVpnAbility() {
        return Ability
                .builder()
                .name("vpn")
                .info("Получить VPN")
                .locality(USER)
                .privacy(PUBLIC)
                .action(this::getVpn)
                .reply(replyToTrial())
                .build();
    }

    private void getVpn(MessageContext messageContext) {
        var userId = messageContext.user().getId();
        var user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.info("start trial");
            getTrial(messageContext);
        }
    }

    @SneakyThrows
    private void getTrial(MessageContext messageContext) {
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Активировать пробный период"));
        row.add(new KeyboardButton("Отмена"));


        // Create a list of keyboard rows
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        log.info("User {} has no subscription", messageContext.user().getId());
        var sendMessage = SendMessage.builder()
                .chatId(messageContext.chatId())
                .text("Доступен пробный период на 2 дня. Чтобы активировать нажмите кнопку ниже.")
                .replyMarkup(replyKeyboardMarkup)
                .build();

        sender.execute(sendMessage);
    }

    private Reply replyToTrial() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> trialReply(upd);
        return Reply.of(action, Flag.MESSAGE, upd -> !upd.getMessage().getText().startsWith("/"));
    }

    private void trialReply(Update update) {
        var message = update.getMessage();
        if (ACTIVATE_TRIAL.equals(message.getText())) {
            if (isTrialAvailable(message.getChatId())) {
                sendMessage(message.getChatId(), "Пробный период активирован");
                saveNewUser(message.getFrom());
            } else {
                sendMessage(message.getChatId(), "Пробный период уже активирован");
            }
        }
    }

    private boolean isTrialAvailable(Long userId) {
        return userRepository.findById(userId).isEmpty();
    }

    private UserEntity saveNewUser(User user) {
        var newUser = new UserEntity();
        newUser.setId(user.getId());
        newUser.setUsername(user.getUserName());
        newUser.setFullName(user.getFirstName() + " " + user.getLastName());
        newUser.setExpireDate(LocalDate.now().plusDays(2));
        newUser.setUserData(user.toString());
        return userRepository.save(newUser);
    }

    @SneakyThrows
    private void sendMessage(Long chatId, String text) {
        var message = SendMessage.builder().chatId(chatId).text(text).build();
        sender.execute(message);
    }
}
