package com.example.peclient;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

/**
 * @author bber9
 * @description object creation of email message with all components
 */
public class FormattedMessage {

    private String draftId;
    private final Message message;
    private String messageId;
    private String threadId;
    private String to;
    private String toEmailId;
    private String from;
    private String fromEmailId = null;
    private String subject;
    private String bodyText = null;
    private String date;
    private String historyId;
    private int isUnread;
    private String toProfilePicString = "?";
    private String fromProfilePicString = "?";

    /**
     * @description constructor which runs helper method
     * @param message text content of message
     */
    public FormattedMessage(Message message){
        this.message = message;
        setMessageFields();
    }

    /**
     * @description grabs details from header of message content and parses for message descriptors
     */
    public void setMessageFields(){
        for(MessagePartHeader header : message.getPayload().getHeaders()) {
            String temp = null;
            int retValue;
            switch (header.getName()) {
                case "To":
                    temp = header.getValue();
                    retValue = temp.indexOf('<');
                    if(retValue != -1) {
                        to = temp.substring(0, retValue);
                        toEmailId = temp.substring(retValue+1,temp.lastIndexOf('>'));
                    }
                    else {
                        to = toEmailId = temp;
                    }
                    for(int i=0; i<to.length(); i++){
                        if(Character.isLetter(to.charAt(i))){
                            toProfilePicString = Character.toString(to.charAt(i));
                            break;
                        }
                    }
                    break;
                case "From":
                    temp = header.getValue();
                    retValue = temp.indexOf('<');
                    if(retValue != -1) {
                        from = temp.substring(0, retValue);
                        fromEmailId = temp.substring(retValue+1,temp.lastIndexOf('>'));
                    }
                    else {
                        from = fromEmailId = temp;
                    }
                    for(int i=0; i<from.length(); i++){
                        if(Character.isLetter(from.charAt(i))){
                            fromProfilePicString = Character.toString(from.charAt(i));
                            break;
                        }
                    }
                    break;
                case "Date":
                    date = header.getValue();
                    break;
                case "Subject":
                    subject = /*CharacterEncoder.toValid3ByteUTF8String(*/header.getValue();//);
                    break;
            }
        }
        historyId = message.getHistoryId().toString();
        messageId = message.getId();
        threadId = message.getThreadId();
        if(message.getLabelIds().contains("UNREAD"))
            isUnread = 1;
        else isUnread = 0;

    }

    public String getMessageId() {
        return messageId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getTo() {
        return to;
    }

    public String getToEmailId() { return toEmailId;}

    public String getFrom() {
        return from;
    }

    public String getFromEmailId() { return fromEmailId;}

    public String getSubject() {
        return subject;
    }

    public String getBodyText() { return  bodyText;}

    public String getDate() {
        return date;
    }

    public String getHistoryId() {
        return historyId;
    }

    public int getIsUnread() { return isUnread; }

    public String getDraftId() {  return draftId;}

    public String getToProfilePicString() { return toProfilePicString;}

    public String getFromProfilePicString() { return fromProfilePicString;}

    public void setDraftId(String id){ draftId = id;}

    public void setBodyText(String text){
        bodyText = text;
    }
}
