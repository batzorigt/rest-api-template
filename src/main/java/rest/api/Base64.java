package rest.api;

public interface Base64 {

    public static String encode(byte[] data) {
        return java.util.Base64.getUrlEncoder().encodeToString(data);
    }

    public static byte[] decode(byte[] data) {
        return java.util.Base64.getUrlDecoder().decode(data);
    }

}