package addon.fmod.features.signs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class SignLines {
    public enum Mode { Custom, Empty, Username, WasHere, Timestamp, Epoch, File, Brand, Base64, Hex, PrefixedHex, Rot13, UUID, RandomUUID, HashedUUID }
    public record Context(String username, java.util.UUID uuid, Instant timestamp, ZoneId zone, String brand) {}
    private SignLines() {}
    public static String format(Mode mode,String custom,String dateFormat,int fileLine,List<String> file,Context context){
        return switch(mode){
            case Custom->custom;
            case Empty->"";
            case Username->context.username;
            case WasHere->context.username+" was here";
            case Timestamp->DateTimeFormatter.ofPattern(dateFormat,Locale.ROOT).withZone(context.zone).format(context.timestamp);
            case Epoch->Long.toString(context.timestamp.getEpochSecond());
            case File->fileLine>=0&&fileLine<file.size()?file.get(fileLine):"";
            case Brand->context.brand;
            case Base64->Base64.getEncoder().encodeToString(custom.getBytes(StandardCharsets.UTF_8));
            case Hex->HexFormat.of().formatHex(custom.getBytes(StandardCharsets.UTF_8));
            case PrefixedHex->"0x"+HexFormat.of().formatHex(custom.getBytes(StandardCharsets.UTF_8));
            case Rot13->rot13(custom);
            case UUID->context.uuid.toString();
            case RandomUUID->java.util.UUID.randomUUID().toString();
            case HashedUUID->hash(context.uuid.toString());
        };
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException error){throw new IllegalStateException(error);}}
    private static String rot13(String value){StringBuilder result=new StringBuilder();for(char ch:value.toCharArray()){if(ch>='a'&&ch<='z')ch=(char)('a'+(ch-'a'+13)%26);else if(ch>='A'&&ch<='Z')ch=(char)('A'+(ch-'A'+13)%26);result.append(ch);}return result.toString();}
}
