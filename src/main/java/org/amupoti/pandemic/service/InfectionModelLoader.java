package org.amupoti.pandemic.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.util.SheetsServiceUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class InfectionModelLoader {

    /**
     * @return the cards that are currently in the game
     */
    public List<InfectionCard> loadFromGooglesheet(String spreadsheetId) throws IOException, GeneralSecurityException {
        return loadAllFromGooglesheet(spreadsheetId).stream()
                .filter(InfectionCard::isInTheGame)
                .collect(Collectors.toList());
    }

    /**
     * @return every card in the sheet, including destroyed cards and cards in box 6. The id of each card is its row
     * position, starting at 0 after the header
     */
    public List<InfectionCard> loadAllFromGooglesheet(String spreadsheetId) throws IOException, GeneralSecurityException {
        return loadAllFromRows(getSpreadsheetCells(spreadsheetId));
    }

    /**
     * Same as {@link #loadAllFromGooglesheet(String)}, from rows already read from the sheet, header included
     */
    public List<InfectionCard> loadAllFromRows(List<List<Object>> rows) {
        List<List<Object>> cardRows = rows.subList(1, rows.size()); //Ignore header

        return IntStream.range(0, cardRows.size())
                .mapToObj(index -> InfectionCardFactory.fromRow(index, cardRows.get(index)))
                .collect(Collectors.toList());
    }

    private List<List<Object>> getSpreadsheetCells(String spreadsheetId) throws IOException, GeneralSecurityException {
        Sheets sheetsService = SheetsServiceUtil.getSheetsService();
        //Range name must be the same as spreadsheet tab name to get all the cells in the sheet
        List<String> ranges = Collections.singletonList("data");
        BatchGetValuesResponse readResult = sheetsService.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges).execute();

        return readResult.getValueRanges().get(0).getValues();
    }
}
