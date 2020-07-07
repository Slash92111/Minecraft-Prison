package me.canyon.game.command;

public class CommandManager {

    public void registerCommand(BaseCommand command) {
        command.register();
    }
}
