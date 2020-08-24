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

@Service
public class InfectionModelLoader {


    public List<InfectionCard> loadFromGooglesheet(String spreadsheetId) throws IOException, GeneralSecurityException {

        List<List<Object>> values = getSpreadsheetCells(spreadsheetId);

        return values.stream()
                .skip(1) //Ignore header
                .map(this::createInfectionCardFromRow)
                .filter(InfectionCard::isInTheGame)
                .collect(Collectors.toList());

    }

    private List<List<Object>> getSpreadsheetCells(String spreadsheetId) throws IOException, GeneralSecurityException {
        Sheets sheetsService = SheetsServiceUtil.getSheetsService();
        //Range name must be the same as spreadsheet tab name to get all the cells in the sheet
        List<String> ranges = Collections.singletonList("data");
        BatchGetValuesResponse readResult = sheetsService.spreadsheets().values().batchGet(spreadsheetId).setRanges(ranges).execute();

        return readResult.getValueRanges().get(0).getValues();
    }

    private InfectionCard createInfectionCardFromRow(List<Object> row) {
        return InfectionCardFactory.fromRow(row);
    }
}
