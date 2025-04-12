package org.amupoti.pandemic.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleAuthorizeUtil {
    public static Credential authorize() throws IOException, GeneralSecurityException {

//        InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream("/credentials.json");
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));
//
//        List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
//
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), clientSecrets, scopes).setDataStoreFactory(new MemoryDataStoreFactory())
//                .setAccessType("offline").build();

        //return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");


        return GoogleCredential.fromStream(GoogleAuthorizeUtil.class.getResourceAsStream("/credentials.json"))
                .createScoped(Collections.singleton(SheetsScopes.DRIVE));
    }
}