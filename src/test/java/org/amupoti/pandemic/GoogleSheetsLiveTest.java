package org.amupoti.pandemic;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GoogleSheetsLiveTest {

    private static Sheets sheetsService;

    private static final String SPREADSHEET_ID = "1y3HicO9FcthZwH_DOC1J315JG3gcJrvUF4ssqZRyvhQ";

    @BeforeAll
    public static void setup() throws GeneralSecurityException, IOException {
        sheetsService = SheetsServiceUtil.getSheetsService();
    }

    @Test
    public void givenTheGoogleSheet_whenReading_thenInfoIsAccessible() throws IOException {

        List<String> ranges = Collections.singletonList("data");
        BatchGetValuesResponse readResult = sheetsService.spreadsheets().values().batchGet(SPREADSHEET_ID).setRanges(ranges).execute();

        ValueRange spreadSheetData = readResult.getValueRanges().get(0);
        assertThat(spreadSheetData.getValues()
                .get(0) //This is the first row
                .get(0)) //This is the first cell from the row
                .isNotNull();


    }

}