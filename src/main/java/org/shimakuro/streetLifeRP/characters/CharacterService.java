package org.shimakuro.streetLifeRP.characters;

import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.identity.IdentityService;

import java.util.UUID;

public final class CharacterService {
    private final PlayerDataRepository repo;
    private final IdentityService identity;
    private final double startingCash;
    private final double startingBank;

    public CharacterService(PlayerDataRepository repo, IdentityService identity, double startingCash, double startingBank) {
        this.repo = repo;
        this.identity = identity;
        this.startingCash = startingCash;
        this.startingBank = startingBank;
    }

    public boolean create(UUID uuid, String firstName, String lastName) {
        PlayerData data = repo.get(uuid);
        if (data.hasCharacter()) return false;

        data.setFirstName(firstName);
        data.setLastName(lastName);
        data.setIdNumber(identity.generateIdNumber(uuid));
        if (data.phoneNumber() == null || data.phoneNumber().isBlank()) {
            data.setPhoneNumber("06" + String.format("%08d", Math.floorMod(uuid.hashCode(), 100_000_000)));
        }
        data.setCash(startingCash);
        data.setBank(startingBank);
        repo.save(data);
        return true;
    }

    public PlayerData data(UUID uuid) {
        return repo.get(uuid);
    }

    public String rpNameOrNull(UUID uuid) {
        return repo.get(uuid).rpNameOrNull();
    }

    public boolean delete(UUID uuid) {
        PlayerData data = repo.get(uuid);
        if (!data.hasCharacter()) return false;

        data.setFirstName(null);
        data.setLastName(null);
        data.setIdNumber(null);
        data.setPhoneNumber(null);

        data.setCash(0.0);
        data.setBank(0.0);

        data.setJob(null);
        data.setLastWorkAtMillis(0L);

        data.setCuffed(false);

        data.setFineAmount(0.0);
        data.setFineIssuer(null);
        data.setFineReason(null);
        data.setFineIssuedAtMillis(0L);

        repo.save(data);
        return true;
    }
}
