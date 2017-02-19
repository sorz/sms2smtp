package org.sorz.lab.sms2smtp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Pair;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * An AsyncTask to send email.
 *
 * Created by xierch on 2017/2/18.
 */

public class SendEmailAsyncTask extends AsyncTask<Pair<String, String>, Void, Integer> {
    final private String addressFrom;
    final private String addressRecipient;
    final private String smtpServer;
    final private int smtpPort;


    public SendEmailAsyncTask(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        addressFrom = preferences.getString(context.getString(R.string.pref_email_from_key), "");
        addressRecipient =
                preferences.getString(context.getString(R.string.pref_email_recipient_key), "");
        smtpServer = preferences.getString(context.getString(R.string.pref_smtp_server_key), "");
        int port = 25;
        try {
            port = Integer.parseInt(
                    preferences.getString(context.getString(R.string.pref_smtp_port_key), "25"));
    } catch (NumberFormatException e) {
        // TODO: tell user.
    }
        smtpPort = port;
    }

    @Override
    protected Integer doInBackground(Pair<String, String>... params) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", smtpPort);
        Session session = Session.getInstance(props, null);
        int sendCount = 0;
        for (Pair<String, String> param : params) {
            String subject = param.first;
            String body = param.second;
            MimeMessage msg = new MimeMessage(session);
            try {
                msg.setFrom(new InternetAddress(addressFrom));
                msg.setRecipients(Message.RecipientType.TO, addressRecipient);
                msg.setSubject(subject);
                msg.setSentDate(new Date());
                msg.setText(body);
                Transport.send(msg);
            } catch (MessagingException e) {
                e.printStackTrace();
                continue;
            }
            ++ sendCount;
        }

        return sendCount;
    }
}
