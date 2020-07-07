package me.canyon.game.player.toggle;

public enum ToggleEnum { //Descriptions will start will "You'll" if on and "You won't" if off
    GANG_LOGIN_ALERT("gangLoginAlert", "Gang Login Alerts"),
    LEVEL_UP_ALERT("levelUpAlert", "Level Up Alerts"),
    XP_GAIN_ALERT("xpGainAlert", "XP Gain Alerts"),
    CENSOR_CHAT("censorChat", "Censor Bad Words"),
    COINFLIP("coinflip", "Coinflip Messages"),
    DEATH_MESSAGE("deathMessage", "Player Death Messages"),
    PRIVATE_MESSAGE("privateMessage", "Private Messages"),
    PUBLIC_CHAT("publicChat", "Public Messages"),
    GANG_INVITE("gangInvite", "Gang Invites"),
    JACKPOT_ALERT("jackpotAlert", "Jackpot Messages"),
    KILL_MESSAGE("killMessage", "Player Killed Message"),
    CLEAR_INVENTORY_WARNING("clearInventoryWarning", "Clear Inventory Warning"),
    EXTRACT_XP_WARNING("extractXPWarning", "Extract XP Confirmation"),
    PAY_CONFIRM("payConfirm", "Pay Confirmation"),
    TPA_REQUESTS("tpaRequests", "TPA Requests"),
    TPA_HERE_REQUESTS("tpahereRequests", "TPA Here Requests"),
    TRADE_REQUESTS("tradeRequest", "Trade Requests"),
    GANG_CHAT("gangChat", "Gang Chat"),
    FRIEND_REQUESTS("friendRequets", "Friend Requests"),
    FRIEND_LOGIN_ALERT("friendLoginAlert", "Friend Login Alerts");

    private String name, id;

    ToggleEnum(String id, String name) {
        this.name = name;
        this.id = id;
    }

    public String getName() { return this.name; }

    public String getID() { return this.id; }

    public static ToggleEnum getFromID(String id) {
        for (ToggleEnum toggleEnum : ToggleEnum.values()) {
            if (toggleEnum.getID().equals(id))
                return toggleEnum;
        }

        return null;
    }

    public static ToggleEnum getFromName(String name) {
        for (ToggleEnum toggleEnum : ToggleEnum.values())
            if (toggleEnum.getName().equals(name))
                return toggleEnum;

        return null;
    }

    public String toString() {
        return this.name;
    }
}
