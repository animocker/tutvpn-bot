package com.tutvpn;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.File;

@Component
public class MessageSenderWrapper {

    private final MessageSender sender;

    public MessageSenderWrapper(@Lazy MessageSender sender) {
        this.sender = sender;
    }

    @SneakyThrows
    public void sendMessage(Long chatId, String text) {
        var message = SendMessage.builder().chatId(chatId).text(text).build();
        sender.execute(message);
    }

    public void sendExpiredVpnMessage(Long chatId) {
        sendMessage(chatId, "Ваш VPN истек");
    }

    @SneakyThrows
    public void sendQrCode(Long userId, File qrCode) {
        sendMessage(userId, "Ваш QR-код для подключения к VPN");
        var sendPhoto = SendPhoto.builder()
                .chatId(userId)
                .photo(new InputFile(qrCode))
                .build();
        sender.sendPhoto(sendPhoto);
    }
}
