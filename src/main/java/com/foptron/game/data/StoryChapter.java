package com.foptron.game.data;

public record StoryChapter(
        String id,
        String title,
        int unlockLevel,
        String unlockOnDefeat,
        String text
) {
}
