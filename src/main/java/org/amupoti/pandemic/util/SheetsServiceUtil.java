package org.amupoti.pandemic.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SheetsServiceUtil {
    private static final String APPLICATION_NAME = "Google Sheets Example";
    private static SheetsServiceUtil instance;
    private final Sheets sheetService;

    private SheetsServiceUtil() throws GeneralSecurityException, IOException {

        Credential credential = GoogleAuthorizeUtil.authorize();
        sheetService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        if (instance == null) {
            instance = new SheetsServiceUtil();
        }
        return instance.sheetService;
    }

    public static Boolean parseBoolean(Object string) {
        return string != null && (string.equals("1") || string.equals("sí") || string.equals("yes"));
    }
}