package org.sorz.lab.sms2smtp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A broadcast receiver on SMS_RECEIVED_ACTION.
 * Build Email subject & body then hand to SmtpService.
 *
 * Created by xierch on 2017/2/18.
 */

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    private static class Message {
        String sender;
        String body;
        long timestamp;

        Message(SmsMessage sms) {
            sender = sms.getDisplayOriginatingAddress();
            body = sms.getDisplayMessageBody();
            timestamp = sms.getTimestampMillis();
        }

        void concat(SmsMessage sms) {
            body += sms.getDisplayMessageBody();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(context.getString(R.string.pref_enable_smtp_forwarding_key), false)) {
            Log.d(TAG, "New SMS ignored according to the global switch.");
            return;
        }
        SmsMessage smses[] = getMessagesFromIntent(intent);
        Collection<Message> messages = retrieveSenderMessage(smses);
        Log.i(TAG, messages.size() + " SMS received, from " + smses.length + " parts");
        for (Message msg : messages ) {
            final Date date = new Date(msg.timestamp);
            final DateFormat dateFormat = android.text.format.DateFormat.getLongDateFormat(context);
            final DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);

            final Map<String, String> values = new HashMap<>();
            values.put("from", msg.sender);
            values.put("message", msg.body);
            values.put("date", dateFormat.format(date));
            values.put("time", timeFormat.format(date));
            final StrSubstitutor sub = new StrSubstitutor(values);

            final String subject = sub.replace(context.getString(R.string.email_subject_template));
            final String content = sub.replace(context.getString(R.string.email_content_template));
            Log.d(TAG, "Request sending email, subject: " + subject);
            SmtpService.startSendEmail(context, subject, content, date.getTime());
        }
    }

    /**
     * Merge messages with same sender (if any) to one message.
     * @param messages A array of SmsMessage parsing from a single PDUS.
     * @return A set of joined messages.
     */
    private static Collection<Message> retrieveSenderMessage(SmsMessage[] messages) {
        HashMap<String, Message> senderBody = new HashMap<>(messages.length);
        for (SmsMessage sms : messages) {
            String sender = sms.getOriginatingAddress();
            if (!senderBody.containsKey(sender)) {
                senderBody.put(sender, new Message(sms));
            } else {
                senderBody.get(sender).concat(sms);
            }
        }
        return senderBody.values();
    }

    private static SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages;
        messages = (Object[]) intent.getSerializableExtra("pdus");
        int pduCount = messages.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];

        for (int i = 0; i < pduCount; i++)
            msgs[i] = SmsMessage.createFromPdu((byte[]) messages[i]);
        return msgs;
    }
}
