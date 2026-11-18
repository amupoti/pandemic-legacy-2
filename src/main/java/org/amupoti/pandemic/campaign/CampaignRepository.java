package org.amupoti.pandemic.campaign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps the campaign in a JSON file, with one backup per day in a folder next to it
 */
@Repository
public class CampaignRepository {

    static final int BACKUPS_KEPT = 30;

    private final Path file;
    private final Path backups;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Autowired
    public CampaignRepository(@Value("${pandemic.campaign-file:campaign.json}") String file) {
        this(file, Clock.systemDefaultZone());
    }

    CampaignRepository(String file, Clock clock) {
        this.file = Paths.get(file).toAbsolutePath();
        this.backups = this.file.resolveSibling("campaign-backups");
        this.clock = clock;
    }

    public Path getFile() {
        return file;
    }

    public Path getBackupFolder() {
        return backups;
    }

    public Optional<Campaign> load() {
        if (!Files.exists(file)) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), Campaign.class));
        } catch (IOException e) {
            throw new UncheckedIOException("No es pot llegir " + file, e);
        }
    }

    /**
     * Writes to a temporary file first, so a failure never leaves a half-written campaign. Then keeps a copy as the
     * backup of the day, replacing the one saved earlier that day.
     */
    public void save(Campaign campaign) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            objectMapper.writeValue(temporary.toFile(), campaign);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            backUp();
        } catch (IOException e) {
            throw new UncheckedIOException("No es pot desar " + file, e);
        }
    }

    private void backUp() throws IOException {
        Files.createDirectories(backups);
        Files.copy(file, backups.resolve("campaign-" + LocalDate.now(clock) + ".json"), StandardCopyOption.REPLACE_EXISTING);
        List<Path> old = listBackups();
        for (Path backup : old.subList(Math.min(BACKUPS_KEPT, old.size()), old.size())) {
            Files.deleteIfExists(backup);
        }
    }

    /**
     * @return the backups, newest first
     */
    public List<Path> listBackups() {
        if (!Files.isDirectory(backups)) return Collections.emptyList();
        try (Stream<Path> files = Files.list(backups)) {
            return files.filter(path -> path.getFileName().toString().matches("campaign-\\d{4}-\\d{2}-\\d{2}\\.json"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("No es poden llistar les còpies de " + backups, e);
        }
    }

    public byte[] export(Campaign campaign) {
        try {
            return objectMapper.writeValueAsBytes(campaign);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a campaign exported before. Fails if the content is not a campaign, so a wrong file never replaces it
     */
    public Campaign read(InputStream content) {
        try {
            Campaign campaign = objectMapper.readValue(content, Campaign.class);
            if (campaign.getCards() == null || campaign.getCards().isEmpty()) {
                throw new IllegalArgumentException("El fitxer no té cap carta");
            }
            return campaign;
        } catch (IOException e) {
            throw new IllegalArgumentException("El fitxer no és una campanya vàlida: " + e.getMessage(), e);
        }
    }
}
