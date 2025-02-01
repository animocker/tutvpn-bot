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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

@Slf4j
@Component
public class TgBot extends AbilityBot {
    private static final String ACTIVATE_TRIAL = "Активировать";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserRepository userRepository;
    private final AlgoService algoService;

    public TgBot(@Value("${BOT_TOKEN}") String token, UserRepository userRepository, AlgoService algoService) {
        super(token, "TutVPN");
        this.userRepository = userRepository;
        this.algoService = algoService;
    }

    @Override
    public long creatorId() {
        return 256007482;
    }


    public Ability algoTest() {
        return Ability
                .builder()
                .name("algo")
                .info("Тестирование Algo")
                .locality(USER)
                .privacy(PUBLIC)
                .action(ctx -> algoService.updateAlgoUsers())
                .build();
    }

    public Ability statusAbility() {
        return Ability
                .builder()
                .name("status")
                .info("Время действия VPN")
                .locality(USER)
                .privacy(PUBLIC)
                .action(this::statusCheck)
                .build();
    }

    private void statusCheck(MessageContext ctx) {
        var userId = ctx.user().getId();
        userRepository.findById(userId).ifPresentOrElse(
                user -> {
                    if (user.getExpireDate().isBefore(LocalDate.now())) {
                        sendMessage(ctx.chatId(), "Ваш VPN истек");
                    } else {
                        sendMessage(ctx.chatId(), "Последний день использования VPN %s".formatted(user.getExpireDate().format(DATE_FORMATTER)));
                    }
                },
                () -> sendTrialMessage(ctx)
        );
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
                .build();
    }

    private void getVpn(MessageContext messageContext) {
        var userId = messageContext.user().getId();
        var user = userRepository.findById(userId);
        if (user.isEmpty()) {
            sendTrialMessage(messageContext);
        }
    }

    private ReplyKeyboardMarkup startTrialKeyboard() {
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
        return replyKeyboardMarkup;
    }

    @SneakyThrows
    private void sendTrialMessage(MessageContext messageContext) {
        var sendMessage = SendMessage.builder()
                .chatId(messageContext.chatId())
                .text("Доступен пробный период на 2 дня. Чтобы активировать нажмите кнопку ниже.")
                .replyMarkup(startTrialKeyboard())
                .build();

        sender.execute(sendMessage);
    }

    public Reply replyToTrial() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> trialReply(upd);
        return Reply.of(action, Flag.MESSAGE, upd -> !upd.getMessage().getText().startsWith("/"));
    }

    private void trialReply(Update update) {
        var message = update.getMessage();
        if (ACTIVATE_TRIAL.equals(message.getText())) {
            if (isTrialAvailable(message.getChatId())) {
                sendMessage(message.getChatId(), "Пробный период активирован");
                var newUser = saveNewUser(message.getFrom());
                algoService.addUser(newUser);
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
