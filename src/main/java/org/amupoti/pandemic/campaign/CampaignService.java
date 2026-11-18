package org.amupoti.pandemic.campaign;

import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Loads the campaign, applies the actions taken during play and saves it after every change
 */
@Service
public class CampaignService {

    private final CampaignRepository repository;
    private final InfectionModelLoader sheetLoader;
    private final String spreadsheetId;

    public CampaignService(CampaignRepository repository, InfectionModelLoader sheetLoader,
                           @Value("${pandemic.spreadsheet-id}") String spreadsheetId) {
        this.repository = repository;
        this.sheetLoader = sheetLoader;
        this.spreadsheetId = spreadsheetId;
    }

    /**
     * Read from disk every time: the file is small and it can be edited by hand
     */
    public synchronized Optional<Campaign> current() {
        return repository.load();
    }

    /**
     * Replaces the campaign with the content of the Google sheet
     */
    public synchronized Campaign importFromSheet() throws IOException, GeneralSecurityException {
        List<InfectionCard> cards = sheetLoader.loadAllFromGooglesheet(spreadsheetId);
        Campaign imported = Campaign.fromInfectionCards(cards);
        repository.save(imported);
        return imported;
    }

    public void draw(int cardId) {
        change(campaign -> campaign.draw(cardId));
    }

    public void epidemic(int cardId) {
        change(campaign -> campaign.epidemic(cardId));
    }

    public void undo() {
        change(Campaign::undo);
    }

    public List<CampaignCard> addCards(String cityName, int count, String label, String color, CardState state) {
        List<List<CampaignCard>> added = new ArrayList<>();
        change(campaign -> added.add(campaign.addCards(cityName, count, label, color, state)));
        return added.get(0);
    }

    public void updateCard(int cardId, String cityName, String label, String color, CardState state) {
        change(campaign -> campaign.updateCard(cardId, cityName, label, color, state));
    }

    public void changeState(int cardId, CardState state) {
        change(campaign -> campaign.changeState(cardId, state));
    }

    public void deleteCard(int cardId) {
        change(campaign -> campaign.deleteCard(cardId));
    }

    public void newGame() {
        change(Campaign::newGame);
    }

    public void correct(int periods, Map<Integer, ? extends Collection<Integer>> drawn) {
        change(campaign -> campaign.correct(periods, drawn));
    }

    /**
     * Only saves when the action succeeds, so a rejected action never leaves the campaign half changed
     */
    private synchronized void change(Consumer<Campaign> action) {
        Campaign campaign = current()
                .orElseThrow(() -> new IllegalStateException("Encara no hi ha cap campanya. Comença-la des de la pàgina inicial."));
        action.accept(campaign);
        repository.save(campaign);
    }

    /**
     * Starts a campaign without any card, to enter the cards by hand instead of importing them
     */
    public synchronized void startEmpty() {
        if (current().isPresent()) throw new IllegalStateException("Ja hi ha una campanya.");
        repository.save(new Campaign());
    }

    /**
     * The Google sheet can only be read with the credentials of a service account. Without them the app works the same,
     * starting from an empty campaign or from a file.
     */
    public boolean isSheetConfigured() {
        return getClass().getResource("/credentials.json") != null;
    }

    public byte[] export() {
        return repository.export(current().orElseThrow(() -> new IllegalStateException("Encara no hi ha cap campanya.")));
    }

    /**
     * Replaces the campaign with one exported before, for instance a backup
     */
    public synchronized void restore(InputStream content) {
        repository.save(repository.read(content));
    }

    public List<Path> listBackups() {
        return repository.listBackups();
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public String getCampaignFile() {
        return repository.getFile().toString();
    }
}
