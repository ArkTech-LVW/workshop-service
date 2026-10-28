package br.com.arktech.workshop;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public final class UniqueTestData {

    private static final String LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ";
    private static final String VIN_CHARACTERS = LETTERS + "0123456789";

    private UniqueTestData() {}

    public static CPFCNPJ uniqueCPF() {
        return withCheckDigits("%09d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000L)));
    }

    public static CPFCNPJ uniqueCNPJ() {
        return withCheckDigits("%08d0001".formatted(ThreadLocalRandom.current().nextLong(100_000_000L)));
    }

    public static String uniquePlate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "" + randomOf(LETTERS) + randomOf(LETTERS) + randomOf(LETTERS) + random.nextInt(10)
                + randomOf(LETTERS) + "%02d".formatted(random.nextInt(100));
    }

    public static String withHyphen(String licensePlate) {
        return licensePlate.substring(0, 3) + "-" + licensePlate.substring(3);
    }

    public static String uniqueChassis() {
        String base = IntStream.range(0, 16)
                .mapToObj(index -> String.valueOf(randomOf(VIN_CHARACTERS)))
                .reduce("", String::concat);
        return "0123456789X".chars()
                .mapToObj(checkDigit -> base.substring(0, 8) + (char) checkDigit + base.substring(8))
                .filter(Chassis::isValid)
                .findFirst()
                .orElseThrow();
    }

    private static CPFCNPJ withCheckDigits(String base) {
        return IntStream.range(0, 100)
                .mapToObj(checkDigits -> base + "%02d".formatted(checkDigits))
                .filter(CPFCNPJ::isValid)
                .findFirst()
                .map(CPFCNPJ::of)
                .orElseThrow();
    }

    private static char randomOf(String characters) {
        return characters.charAt(ThreadLocalRandom.current().nextInt(characters.length()));
    }
}
