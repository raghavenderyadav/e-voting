package uk.dsxt.voting.common.utils;

public class MessageBuilder {
    
    public static String buildMessage(String... parts) {
        String[] escapedParts = new String[parts.length];
        for(int i = 0; i < parts.length; i++) {
            escapedParts[i] = parts[i] == null ? "`n" : parts[i].replaceAll("`", "`o").replaceAll("@", "`e");
        }
        return String.join("@", escapedParts);
    }

    public static String[] splitMessage(String message) {
        boolean findE = message.indexOf("`e") > 0;
        boolean findO = message.indexOf("`o") > 0;
        boolean findN = message.indexOf("`o") > 0;
        String[] parts =  message.split("@");
        if (findE || findO || findN) {
            for(int i = 0; i < parts.length; i++) {
                if (findN && parts[i].equals("`n"))
                    parts[i] = null;
                else {
                    if (findE)
                        parts[i] = parts[i].replaceAll("`e", "@");
                    if (findO)
                        parts[i] = parts[i].replaceAll("`o", "`");
                }
            }
        }
        return parts;
    }
}
