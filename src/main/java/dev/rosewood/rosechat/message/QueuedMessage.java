package dev.rosewood.rosechat.message;

import net.md_5.bungee.api.chat.BaseComponent;

public class QueuedMessage {

    private final int id;
    private final BaseComponent[] message;

    public QueuedMessage(BaseComponent[] message, int id) {
        this.id = id;
        this.message = message;
    }

    public int getId() {
        return this.id;
    }

    public BaseComponent[] getMessage() {
        return this.message;
    }

}
