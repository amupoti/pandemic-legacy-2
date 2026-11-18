package org.amupoti.pandemic.campaign;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void thereIsNoCampaignUntilOneIsSaved() {
        CampaignRepository repository = new CampaignRepository(folder.resolve("campaign.json").toString());

        assertThat(repository.load()).isEmpty();
    }

    @Test
    void savesAndLoadsTheWholeCampaign() {
        CampaignRepository repository = new CampaignRepository(folder.resolve("data/campaign.json").toString());
        Campaign campaign = new Campaign();
        campaign.addCard("Lagos 1", "Refugi", "#E9B824");
        campaign.addCard("Madrid", "", "#2FA4CF");
        campaign.draw(0);
        campaign.epidemic(1);

        repository.save(campaign);
        Campaign loaded = repository.load().orElseThrow(AssertionError::new);

        assertThat(loaded).isEqualTo(campaign);
        assertThat(loaded.getLastEvent()).contains(new CampaignEvent(CampaignEvent.Type.EPIDEMIC, 1));
        assertThat(Files.exists(folder.resolve("data/campaign.json.tmp"))).isFalse();
    }

    @Test
    void aBrokenFileGivesAClearError() throws Exception {
        Path file = folder.resolve("campaign.json");
        Files.write(file, "{ not json".getBytes());

        assertThatThrownBy(() -> new CampaignRepository(file.toString()).load())
                .hasMessageContaining("No es pot llegir");
    }

    @Test
    void keepsOneBackupPerDay() throws Exception {
        Path file = folder.resolve("campaign.json");
        Campaign campaign = new Campaign();
        campaign.addCard("Lagos 1", "", "#E9B824");

        new CampaignRepository(file.toString(), day("2026-11-17")).save(campaign);
        campaign.draw(0);
        new CampaignRepository(file.toString(), day("2026-11-18")).save(campaign);
        new CampaignRepository(file.toString(), day("2026-11-18")).save(campaign);

        CampaignRepository repository = new CampaignRepository(file.toString(), day("2026-11-18"));
        assertThat(repository.listBackups()).extracting(path -> path.getFileName().toString())
                .containsExactly("campaign-2026-11-18.json", "campaign-2026-11-17.json");
        Campaign yesterday = repository.read(Files.newInputStream(repository.listBackups().get(1)));
        assertThat(yesterday.getHistory()).isEmpty();
    }

    @Test
    void removesTheOldestBackups() throws Exception {
        Path file = folder.resolve("campaign.json");
        Campaign campaign = new Campaign();
        campaign.addCard("Lagos 1", "", "#E9B824");
        LocalDate start = LocalDate.parse("2026-01-01");

        for (int i = 0; i < CampaignRepository.BACKUPS_KEPT + 5; i++) {
            new CampaignRepository(file.toString(), day(start.plusDays(i).toString())).save(campaign);
        }

        List<Path> backups = new CampaignRepository(file.toString()).listBackups();
        assertThat(backups).hasSize(CampaignRepository.BACKUPS_KEPT);
        assertThat(backups.get(backups.size() - 1).getFileName().toString()).isEqualTo("campaign-2026-01-06.json");
    }

    @Test
    void anExportedCampaignCanBeReadBack() {
        CampaignRepository repository = new CampaignRepository(folder.resolve("campaign.json").toString());
        Campaign campaign = new Campaign();
        campaign.addCard("Lagos 1", "Refugi", "#E9B824");
        campaign.draw(0);

        Campaign read = repository.read(new ByteArrayInputStream(repository.export(campaign)));

        assertThat(read).isEqualTo(campaign);
    }

    @Test
    void aFileThatIsNotACampaignIsRejected() {
        CampaignRepository repository = new CampaignRepository(folder.resolve("campaign.json").toString());

        assertThatThrownBy(() -> repository.read(new ByteArrayInputStream("[1, 2]".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.read(new ByteArrayInputStream("{}".getBytes())))
                .hasMessage("El fitxer no té cap carta");
    }

    private static Clock day(String date) {
        return Clock.fixed(LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }
}
