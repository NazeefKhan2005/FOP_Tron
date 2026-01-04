package com.foptron.game.persistence;

import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LeaderboardService {

    private final Path file = Path.of("leaderboard.csv");

    public synchronized List<LeaderboardEntry> top10() {
        List<LeaderboardEntry> all = readAll();
        all.sort(Comparator.comparingLong(LeaderboardEntry::totalScore).reversed());
        return all.stream().limit(10).toList();
    }

    public synchronized void record(String playerName, int highestLevel, long totalScore) {
        if (playerName == null || playerName.isBlank()) playerName = "Player";

        List<LeaderboardEntry> all = readAll();
        all.add(new LeaderboardEntry(playerName.trim(), highestLevel, totalScore, LocalDate.now()));
        all.sort(Comparator.comparingLong(LeaderboardEntry::totalScore).reversed());

        writeAll(all);
    }

    private List<LeaderboardEntry> readAll() {
        try {
            if (!Files.exists(file)) {
                return new ArrayList<>();
            }

            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<LeaderboardEntry> out = new ArrayList<>();
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("playerName,")) continue;
                String[] p = line.split(",");
                if (p.length < 4) continue;
                out.add(new LeaderboardEntry(
                        p[0],
                        Integer.parseInt(p[1]),
                        Long.parseLong(p[2]),
                        LocalDate.parse(p[3])
                ));
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void writeAll(List<LeaderboardEntry> entries) {
        try {
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("playerName,highestLevel,totalScore,date\n");
                for (LeaderboardEntry e : entries) {
                    w.write(escape(e.playerName()));
                    w.write(',');
                    w.write(Integer.toString(e.highestLevel()));
                    w.write(',');
                    w.write(Long.toString(e.totalScore()));
                    w.write(',');
                    w.write(e.date().toString());
                    w.write('\n');
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write leaderboard", e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(",", " ");
    }
}
