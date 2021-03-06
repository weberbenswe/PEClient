package com.example.peclient;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.model.*;
import javafx.application.Platform;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GmailOperations {

    public static List<FormattedMessage> getInboxMessages(int maxResults) throws IOException {
        List<FormattedMessage> messages = new ArrayList<>();
        int max = maxResults + GmailMessages.inboxLastIndex;
        int count = 0;
        //TODO: Dynamically acquire url
        GenericUrl url = new GenericUrl("https://www.googleapis.com/batch/gmail/v1");
        BatchRequest batch = GoogleAuthorizationLogin.service.batch();
        JsonBatchCallback<Message> bc = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Internet Connection Error");
            }

            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                messages.add(new FormattedMessage(message));
            }
        };
        Message m;
        while (GmailMessages.inboxLastIndex < max && GmailMessages.inboxLastIndex < GmailMessages.inboxMaxSize) {
            m = GmailMessages.inboxList.get(GmailMessages.inboxLastIndex);
            GoogleAuthorizationLogin.service.users().messages().get("me", m.getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).queue(batch, bc);
            GmailMessages.inboxLastIndex++;
            count++;
        }
        try {
            if (count != 0) {
                batch.setBatchUrl(url);
                batch.execute();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return messages;
    }

    public static void loadInbox() throws IOException, SQLException, ClassNotFoundException {
        GmailMessages.inboxList = getAllMessagesListInLabelId(List.of("INBOX"));
        GmailMessages.inboxMaxSize = GmailMessages.inboxList.size();
        List<FormattedMessage> temp = getInboxMessages(50);
        GmailMessages.inboxMessages.addAll(temp);
        if (temp.size() != 0)
            GmailMessages.inboxStartHistoryId = temp.get(0).getHistoryId();
        // TODO:Save messages to local db
        //SaveMessages.saveInbox(temp);
        System.out.println("Inbox Loaded");
    }

    public static void getNewInboxMails() throws IOException {
        if (GmailMessages.inboxStartHistoryId != null) {
            List<History> historyList = getHistoryMessages("INBOX", new BigInteger(GmailMessages.inboxStartHistoryId));
            for (History history : historyList) {
                List<HistoryMessageAdded> messageAddedList = history.getMessagesAdded();
                if (messageAddedList != null) {
                    for (HistoryMessageAdded messageAdded : messageAddedList) {
                        Platform.runLater(() -> {
                            try {
                                GmailMessages.inboxMessages.add(0, new FormattedMessage(GoogleAuthorizationLogin.service.users().messages().get("me",
                                        messageAdded.getMessage().getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).execute()));
                                GmailMessages.inboxStartHistoryId = GmailMessages.inboxMessages.get(0).getHistoryId();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    Platform.runLater(() -> NotificationBuilder.getNotification("New Mails", ""+messageAddedList.size()+ " new Mails arrived"));
                }
            }

        } else {
            Platform.runLater(() -> {
                try {
                    loadInbox();
                } catch (IOException | SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    //***********************************************************************************
    //Sent folder specific operations

    public static List<FormattedMessage> getSentMessages(int maxResults) throws IOException {
        List<FormattedMessage> messages = new ArrayList<>();
        int count = 0;
        int max = GmailMessages.sentLastIndex + maxResults;
        //TODO: Dynamically acquire url
        GenericUrl url = new GenericUrl("https://www.googleapis.com/batch/gmail/v1");
        BatchRequest batch = GoogleAuthorizationLogin.service.batch();
        JsonBatchCallback<Message> bc = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Internet Connection Error");
            }

            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                messages.add(new FormattedMessage(message));
            }
        };
        Message m;
        while (GmailMessages.sentLastIndex < max && GmailMessages.sentLastIndex < GmailMessages.sentMaxSize) {
            m = GmailMessages.sentList.get(GmailMessages.sentLastIndex);
            GoogleAuthorizationLogin.service.users().messages().get("me", m.getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).queue(batch, bc);
            GmailMessages.sentLastIndex++;
            count++;
        }
        if (count != 0)
            batch.setBatchUrl(url);
            batch.execute();
        return messages;
    }

    public static void loadSent() throws IOException {
        GmailMessages.sentList = getAllMessagesListInLabelId(List.of("SENT"));
        GmailMessages.sentMaxSize = GmailMessages.sentList.size();
        List<FormattedMessage> temp = getSentMessages(50);
        GmailMessages.sentMessages.addAll(temp);
        if (temp.size() != 0)
            GmailMessages.sentStartHistoryId = temp.get(0).getHistoryId();
        //SaveMessages.saveSent(temp);
        System.out.println("sent Loaded");
    }

    public static void getNewSentMails() throws IOException {
        if (GmailMessages.sentStartHistoryId != null) {
            List<History> historyList = getHistoryMessages("SENT", new BigInteger(GmailMessages.sentStartHistoryId));
            for (History history : historyList) {
                List<HistoryMessageAdded> messageAddedList = history.getMessagesAdded();
                if (messageAddedList != null) {
                    for (HistoryMessageAdded messageAdded : messageAddedList) {
                        Platform.runLater(() -> {
                            try {
                                GmailMessages.sentMessages.add(0, new FormattedMessage(GoogleAuthorizationLogin.service.users().messages().get("me", messageAdded.getMessage()
                                        .getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).execute()));
                                GmailMessages.sentStartHistoryId = GmailMessages.sentMessages.get(0).getHistoryId();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }

        } else {
            Platform.runLater(() -> {
                try {
                    loadSent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }


    //***********************************************************************************
    //Drafts specific operations
    public static List<Draft> getAllDraftsList() throws IOException {
        ListDraftsResponse response = GoogleAuthorizationLogin.service.users().drafts().list("me").execute();
        List<Draft> drafts = new ArrayList<>();
        while (response.getDrafts() != null) {
            drafts.addAll(response.getDrafts());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = GoogleAuthorizationLogin.service.users().drafts().list("me").setPageToken(pageToken).execute();
            } else
                break;
        }
        // messagesList = messages;
        return drafts;
    }

    public static List<FormattedMessage> getDraftMessages(int maxResults) throws IOException {
        List<FormattedMessage> messages = new ArrayList<>();
        int count = 0;
        int max = maxResults + GmailMessages.draftLastIndex;
        //TODO: Dynamically acquire url
        GenericUrl url = new GenericUrl("https://www.googleapis.com/batch/gmail/v1");
        BatchRequest batch = GoogleAuthorizationLogin.service.batch();
        JsonBatchCallback<Draft> bc = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Internet Connection Error");
            }

            @Override
            public void onSuccess(Draft draft, HttpHeaders responseHeaders) {
                Message message = draft.getMessage();
                FormattedMessage formattedMessage = new FormattedMessage(message);
                formattedMessage.setDraftId(draft.getId());
                //formattedMessage.setBodyText(GmailOperations.getMessageBody(message));
                messages.add(formattedMessage);
            }
        };
        Draft d;
        while (GmailMessages.draftLastIndex < max && GmailMessages.draftLastIndex < GmailMessages.draftMaxSize) {
            d = GmailMessages.draftList.get(GmailMessages.draftLastIndex);
            GoogleAuthorizationLogin.service.users().drafts().get("me", d.getId()).queue(batch, bc);
            GmailMessages.draftLastIndex++;
            count++;
        }
        if (count != 0)
            batch.setBatchUrl(url);
        batch.execute();
        return messages;
    }

    public static void loadDraft() throws IOException {
        //GmailMessages.draftList = getAllMessagesListInLabelId(Arrays.asList("DRAFT"));
        GmailMessages.draftList = getAllDraftsList();
        GmailMessages.draftMaxSize = GmailMessages.draftList.size();
        List<FormattedMessage> temp = getDraftMessages(50);
        GmailMessages.draftMessages.setAll(temp);
        if (temp.size() != 0)
            GmailMessages.draftStartHistoryId = temp.get(0).getHistoryId();
        //SaveMessages.saveDraft(temp);
        System.out.println("draft Loaded");
    }

    public static void getNewDraftMails() throws IOException {
        List<History> historyList = getHistoryMessages("DRAFT", new BigInteger(GmailMessages.draftStartHistoryId));
        if (!historyList.isEmpty()) {
            Platform.runLater(() -> {
                try {
                    GmailMessages.draftLastIndex = 0;
                    loadDraft();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            });
        }
    }


    //***********************************************************************************
    //Trash related operations
    public static List<FormattedMessage> getTrashMessages(int maxResults) throws IOException {
        List<FormattedMessage> messages = new ArrayList<>();
        //TODO: Dynamically acquire url
        GenericUrl url = new GenericUrl("https://www.googleapis.com/batch/gmail/v1");
        BatchRequest batch = GoogleAuthorizationLogin.service.batch();
        JsonBatchCallback<Message> bc = new JsonBatchCallback<>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Internet Connection Error");
            }

            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                messages.add(new FormattedMessage(message));
            }
        };
        Message m;
        while (GmailMessages.trashLastIndex < maxResults && GmailMessages.trashLastIndex < GmailMessages.trashMaxSize) {
            m = GmailMessages.trashList.get(GmailMessages.trashLastIndex);
            GoogleAuthorizationLogin.service.users().messages().get("me", m.getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).queue(batch, bc);
            GmailMessages.trashLastIndex++;
        }
        if (Math.abs(GmailMessages.trashLastIndex - maxResults) != maxResults)
            batch.setBatchUrl(url);
            batch.execute();
        return messages;
    }

    public static void loadTrash() throws IOException {
        GmailMessages.trashList = getAllMessagesListInLabelId(List.of("TRASH"));
        GmailMessages.trashMaxSize = GmailMessages.trashList.size();
        GmailMessages.trashLastIndex = 0;
        List<FormattedMessage> temp = getTrashMessages(50);
        GmailMessages.trashMessages.setAll(temp);
        if (temp.size() != 0)
            GmailMessages.trashStartHistoryId = temp.get(0).getHistoryId();
        //SaveMessages.saveTrash(temp);
        System.out.println("trash Loaded");
    }

    public static void getNewTrashMails() throws IOException {
        if (GmailMessages.trashStartHistoryId != null) {
            List<History> historyList = getHistoryMessages("TRASH", new BigInteger(GmailMessages.trashStartHistoryId));
            for (History history : historyList) {
                List<HistoryMessageAdded> messageAddedList = history.getMessagesAdded();
                if (messageAddedList != null) {
                    for (HistoryMessageAdded messageAdded : messageAddedList) {
                        Platform.runLater(() -> {
                            try {
                                GmailMessages.trashMessages.add(0, new FormattedMessage(GoogleAuthorizationLogin.service.users().messages().get("me", messageAdded.getMessage()
                                        .getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).execute()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        } else {
            Platform.runLater(() -> {
                try {
                    loadTrash();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public static void trashMessage(String id) throws IOException {
        GoogleAuthorizationLogin.service.users().messages().trash("me", id).execute();
    }

    public static void untrashMessage(String id) throws IOException {
        GoogleAuthorizationLogin.service.users().messages().untrash("me", id).execute();
        loadTrash();
    }









    //***********************************************************************************
    //common messages operation

    public static Message getMessage(String id) throws IOException {
        return GoogleAuthorizationLogin.service.users().messages().get("me", id).execute();
    }

    //****Retrieve message body ****
    public static String getMessageBody(Message m) throws IOException {
        String string;
        if (m.getPayload().getParts() == null)
            string = m.getPayload().getBody().getData();
        else {
            string = getHtmlParts(m.getPayload().getParts());
        }
        return new String(Base64.decodeBase64(string.getBytes()));
    }

    //****Retrieve multipart html content****
    public static String getHtmlParts(List<MessagePart> m) {
        StringBuilder stringBuilder = new StringBuilder();
        for (MessagePart mp : m) {
            if (mp.getParts() == null) {
                if (mp.getMimeType().equals("text/html"))
                    stringBuilder.append(mp.getBody().getData());
            } else {
                stringBuilder.append(getHtmlParts(mp.getParts()));
            }
        }
        return new String(stringBuilder);
    }

    //****this method will return messages with messageid and threadids****
    public static List<Message> getAllMessagesListInLabelId(List<String> labelIds) throws IOException {
        ListMessagesResponse response = GoogleAuthorizationLogin.service.users().messages().list("me").setLabelIds(labelIds).execute();
        List<Message> messages = new ArrayList<>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = GoogleAuthorizationLogin.service.users().messages().list("me").setLabelIds(labelIds).setPageToken(pageToken).execute();
            } else
                break;
        }
        // messagesList = messages;
        return messages;
    }

    public static void sendMessage(String to, String from, String sub, String bodyText, boolean isHtml, List<File> attachments) throws IOException, MessagingException {
        Message message = createMessage(to, from, sub, bodyText, isHtml, attachments);
        message = GoogleAuthorizationLogin.service.users().messages().send("me", message).execute();
    }

    public static void createDraft(String to, String from, String sub, String bodyText, boolean isHtml, List<File> attachments) throws IOException, MessagingException {
        Draft createdDraft = new Draft();
        createdDraft.setMessage(createMessage(to, from, sub, bodyText, isHtml, attachments));
        createdDraft = GoogleAuthorizationLogin.service.users().drafts().create("me", createdDraft).execute();
    }

    public static void updateDraft(String draftId, String to, String from, String sub, String bodyText, boolean isHtml, List<File> attachments) throws IOException, MessagingException {
        Draft updatedDraft = new Draft();
        updatedDraft.setMessage(createMessage(to, from, sub, bodyText, isHtml, attachments));
        updatedDraft = GoogleAuthorizationLogin.service.users().drafts().update("me", draftId, updatedDraft).execute();
    }


    public static Message createMessage(String to, String from, String sub, String bodyText, boolean isHtml, List<File> attachments) throws MessagingException, IOException {
        if (to == null || from == null) {
            System.out.println("Fields cannot be empty");
            return null;
        }
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        if (!from.equals(""))
            email.setFrom(new InternetAddress(from));
        if (!to.equals(""))
            email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        //if (!sub.equals(""))
        email.setSubject(sub);

        if (attachments == null || attachments.isEmpty()) {
            if (isHtml)
                email.setContent(bodyText, "text/html");
            else
                email.setText(bodyText);
        } else {
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            if (isHtml)
                mimeBodyPart.setContent(bodyText, "text/plain");
            else
                mimeBodyPart.setContent(bodyText, "text/plain");

            for (File attachmentFile : attachments) {
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);

                mimeBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attachmentFile);

                mimeBodyPart.setDataHandler(new DataHandler(source));
                mimeBodyPart.setFileName(attachmentFile.getName());

                multipart.addBodyPart(mimeBodyPart);
                email.setContent(multipart);
            }
        }
        Message message = createFromMimeMessage(email);
        return message;
    }

    private static Message createFromMimeMessage(MimeMessage email) throws IOException, MessagingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        email.writeTo(baos);
        String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static List<History> getHistoryMessages(String labelId, BigInteger startHistoryId) throws IOException {
        List<History> histories = new ArrayList<>();
        ListHistoryResponse response = GoogleAuthorizationLogin.service.users().history().list("me").setHistoryTypes(Arrays.asList("messageAdded"))
                .setLabelId(labelId).setStartHistoryId(startHistoryId).execute();
        while (response.getHistory() != null) {
            histories.addAll(response.getHistory());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = GoogleAuthorizationLogin.service.users().history().list("me").setHistoryTypes(List.of("messageAdded"))
                        .setLabelId(labelId).setPageToken(pageToken).setStartHistoryId(startHistoryId).execute();
            } else {
                break;
            }
        }
        return histories;
    }

    public static void listSearchMessages(String query) throws IOException {
        GmailMessages.searchLastIndex = 0;
        ListMessagesResponse response = GoogleAuthorizationLogin.service.users().messages().list("me")
                .setQ(query).execute();
        List<Message> messages = new ArrayList<Message>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = GoogleAuthorizationLogin.service.users().messages().list("me").setQ(query)
                        .setPageToken(pageToken).execute();
            } else
                break;
        }
        GmailMessages.searchList = messages;
        GmailMessages.searchMaxSize = messages.size();
    }

    public static void getSearchMessages(String query) throws IOException {
        int max = GmailMessages.searchLastIndex + 20;
        int count = 0;

        //TODO: Dynamically acquire url
        GenericUrl url = new GenericUrl("https://www.googleapis.com/batch/gmail/v1");
        BatchRequest batch = GoogleAuthorizationLogin.service.batch();
        JsonBatchCallback<Message> bc = new JsonBatchCallback<Message>() {
            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Internet Connection Error");
            }

            @Override
            public void onSuccess(Message message, HttpHeaders responseHeaders) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        GmailMessages.searchMessages.add(new FormattedMessage(message));
                    }
                });

            }
        };

        Message m = null;
        while (GmailMessages.searchLastIndex < max && GmailMessages.searchLastIndex < GmailMessages.searchMaxSize) {
            m = GmailMessages.searchList.get(GmailMessages.searchLastIndex);
            GoogleAuthorizationLogin.service.users().messages().get("me", m.getId()).setFormat("metadata").setMetadataHeaders(Arrays.asList("To", "From", "Date", "Subject")).queue(batch, bc);
            GmailMessages.searchLastIndex++;
            count++;
        }
        if (count != 0)
            batch.setBatchUrl(url);
            batch.execute();
    }

    //***********************************************************************************
    //attachments related operations

    public static List<File> downloadAttachments(Message message, String downloadLocation) throws IOException {
        List<File> attachmentsList = new ArrayList<>();
        List<MessagePart> parts = message.getPayload().getParts();
        if(parts !=null){
            for(MessagePart part: parts){
                if(part.getFilename() != null && part.getFilename().length() >0){
                    String filename = part.getFilename();
                    String attId = part.getBody().getAttachmentId();
                    MessagePartBody attachPart = GoogleAuthorizationLogin.service.users().messages().attachments()
                            .get("me", message.getId(), attId).execute();

                    Base64 base64Url = new Base64(true);
                    byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
                    File temp = new File(downloadLocation);
                    if(!temp.exists())
                        temp.mkdir();
                    FileOutputStream fileOutputStream = new FileOutputStream(temp+"/"+filename);
                    fileOutputStream.write(fileByteArray);
                    fileOutputStream.close();
                    attachmentsList.add(new File(temp+"/"+filename));
                }
            }
        }
        return attachmentsList;
    }

    public static void loadMailBox() throws IOException, SQLException, ClassNotFoundException {
        GmailMessages.USERS_EMAIL_ADDRESS = GoogleAuthorizationLogin.service.users().getProfile("me").execute().getEmailAddress();
        loadInbox();
        loadSent();
        loadDraft();
        loadTrash();
    }
}
