package me.canyon.game.staff;

public enum Punishment {
    MUTE("mute", "Mute"),
    BAN("ban", "Permanently Ban"),
    TEMP_BAN("tempBan", "Temporary Ban"),
    WARNING("warning", "Warning"),
    SKIN_CHANGE("skinChange", "Change Skin");

    private String id, name;

    Punishment(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getID() { return this.id; }
    public String getName() { return this.name; }

    public static Punishment getFromID(String id) {
        for (Punishment punishment : Punishment.values()) {
            if (punishment.getID().equals(id))
                return punishment;
        }

        return null;
    }

    public static Punishment getFromName(String name) {
        for (Punishment punishment : Punishment.values())
            if (punishment.getName().equals(name))
                return punishment;

        return null;
    }


    public enum Type {
        RACISM("racism", "Racism"),
        SPAMMING("spamming", "Spamming"),
        SEXUAL_HARASSMENT("sexualHarassment", "Sexual Harassment"),
        BULLYING("bullying", "Bullying"),
        VERBAL_ABUSE("verbalAbuse", "Verbal Abuse"),
        NEGATIVE_BEHAVIOUR("negativeBehaviour", "Negative Behaviour"),
        HACKING("hacking", "Hacking"),
        BAN_EVASION("banEvasion", "Ban Evasion"),
        INAPPROPRIATE_SKIN("inappropriateSkin", "Inappropriate Skin");

        private String id, name;

        Type(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getID() { return this.id; }
        public String getName() { return this.name; }

        public static Type getFromID(String id) {
            for (Type type : Type.values()) {
                if (type.getID().equals(id))
                    return type;
            }

            return null;
        }

        public static Type getFromName(String name) {
            for (Type type : Type.values())
                if (type.getName().equals(name))
                    return type;

            return null;
        }
    }
}