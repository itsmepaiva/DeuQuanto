package portifolio.deuquanto.configuration;

public class DataMasker {

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "***@" + domain;
        }

        String visiblePart = username.substring(0, 2);
        return visiblePart + "***@" + domain;
    }

    public static String maskPix(String pixKey) {
        if (pixKey == null || pixKey.isBlank()) {
            return "***";
        }

        if (pixKey.contains("@")) {
            return maskEmail(pixKey);
        }

        String cleanPix = pixKey.trim();
        int length = cleanPix.length();

        if (length <= 5) {
            return "***";
        }

        // Estratégia Genérica (CPF, Celular, CNPJ ou UUID Aleatório)
        // Exemplo Celular: 85999887766 -> 859***766
        // Exemplo CPF: 123.456.789-00 -> 123***-00
        // Exemplo UUID: 123e4567-e89b... -> 123***...

        String prefix = cleanPix.substring(0, 3);
        String suffix = cleanPix.substring(length - 3);

        return prefix + "***" + suffix;
    }
}