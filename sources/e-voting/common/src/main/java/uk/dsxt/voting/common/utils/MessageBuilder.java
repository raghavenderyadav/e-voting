package uk.dsxt.voting.common.utils;

public class MessageBuilder {
    
    public static String buildMessage(String... parts) {
        String[] escapedParts = new String[parts.length];
        for(int i = 0; i < parts.length; i++) {
            escapedParts[i] = parts[i].replaceAll("/", "/o").replaceAll("@", "/e");
        }
        return String.join("@", escapedParts);
    }

    public static String[] splitMessage(String message) {
        String[] parts =  message.split("@");
        for(int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].replaceAll("/e", "@").replaceAll("/o", "/");
        }
        return parts;
    }
}
