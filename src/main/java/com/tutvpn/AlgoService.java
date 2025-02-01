package com.tutvpn;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlgoService {

    private final File algoDir = new File("/home/evgen/REPOS/algo");
    private final File configFile = new File(algoDir, "config.cfg");
    private final UserRepository userRepository;

    public AlgoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        if (!algoDir.exists()) {
            throw new RuntimeException("Algo directory not found");
        }
        if (!configFile.exists()) {
            throw new RuntimeException("Config file not found");
        }
    }

    @SneakyThrows
    public void addUser(UserEntity user) {
        List<String> lines = Files.readAllLines(configFile.toPath());
        List<String> updatedLines = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().equals("users:")) {
                updatedLines.add(line);
                updatedLines.add("  - \"%s\" ".formatted(user.getId()));
            } else {
                updatedLines.add(line);
            }
        }

        Files.write(configFile.toPath(), updatedLines);
    }

    public BufferedImage getQRCode(UserEntity user) {
        return null;
    }

    //every day
    @Scheduled(cron = "0 0 0 * * *")
    public void updateAlgoUsers() {
        var validUsers = userRepository.findAllIdByExpireDateBefore(LocalDate.now().plusDays(1));
        updateUsersList(validUsers);
    }

    @SneakyThrows
    public void updateUsersList(List<Long> users) {
        List<String> lines = Files.readAllLines(configFile.toPath());
        List<String> updatedLines = new ArrayList<>();
        boolean usersSection = false;

        for (String line : lines) {
            if (line.trim().equals("users:")) {
                usersSection = true;
                updatedLines.add(line);
                for (Long user : users) {
                    updatedLines.add("  - \"%s\" ".formatted(user));
                }
            } else if (usersSection && !line.trim().startsWith("-")) {
                usersSection = false;
                updatedLines.add(line);
            } else if (!usersSection) {
                updatedLines.add(line);
            }
        }

        Files.write(configFile.toPath(), updatedLines);
    }
}
