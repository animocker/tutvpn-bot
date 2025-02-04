package com.tutvpn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class RetryHelper {

    private final MessageSenderWrapper messageSenderWrapper;
    private final AlgoService algoService;

    public RetryHelper(MessageSenderWrapper messageSenderWrapper, AlgoService algoService) {
        this.messageSenderWrapper = messageSenderWrapper;
        this.algoService = algoService;
    }

    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 10000, multiplier = 3))
    public File getQRCodeWithRetry(UserEntity user) {
        log.info("Retry count: {}", RetrySynchronizationManager.getContext().getRetryCount());
        var qrCode = algoService.getQRCode(user);
        if (!qrCode.exists()) {
            var retryCount = RetrySynchronizationManager.getContext().getRetryCount();
            if (retryCount == 0) {
                messageSenderWrapper.sendMessage(user.getId(), "QR-код генерируется, пожалуйста подождите");
            } else if (retryCount == 3) {
                messageSenderWrapper.sendMessage(user.getId(), "QR-код не готов, попробуйте позже или напишите в поддержку");
            }
            throw new RuntimeException("QR code not found");
        }
        return qrCode;
    }
}
