package org.sorz.lab.sms2smtp;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SmtpService extends IntentService {
    private static final String TAG = "SmtpService";

    private static final String ACTION_SEND_EMAIL = "org.sorz.lab.sms2smtp.action.SEND_EMAIL";

    private static final String EXTRA_SUBJECT = "org.sorz.lab.sms2smtp.extra.SUBJECT";
    private static final String EXTRA_CONTENT = "org.sorz.lab.sms2smtp.extra.CONTENT";
    private static final String EXTRA_TIMESTAMP = "org.sorz.lab.sms2smtp.extra.TIMESTAMP";


    public SmtpService() {
        super("SmtpService");
    }

    /**
     * Starts this service to send SMS via SMTP.
     */
    public static void startSendEmail(Context context, String subject, String content, long timestamp) {
        Intent intent = new Intent(context, SmtpService.class);
        intent.setAction(ACTION_SEND_EMAIL);
        intent.putExtra(EXTRA_SUBJECT, subject);
        intent.putExtra(EXTRA_CONTENT, content);
        intent.putExtra(EXTRA_TIMESTAMP, timestamp);
        context.startService(intent);
    }

    public static void startSendEmail(Context context, String subject, String content) {
        startSendEmail(context, subject, content, new Date().getTime());

    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SEND_EMAIL.equals(action)) {
                final String subject = intent.getStringExtra(EXTRA_SUBJECT);
                final String content = intent.getStringExtra(EXTRA_CONTENT);
                final long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, new Date().getTime());
                handleSendEmail(subject, content, timestamp);
            }
        }
    }

    /**
     * Send email in the provided background thread with the provided
     * subject and content.
     */
    private void handleSendEmail(String subject, String content, long timestamp) {
        final Context context = getBaseContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String from = preferences.getString(context.getString(R.string.pref_email_from_key), "");
        final String recipient =
                preferences.getString(context.getString(R.string.pref_email_recipient_key), "");
        final String host = preferences.getString(context.getString(R.string.pref_smtp_server_key), "");
        int port = 25;
        try {
            port = Integer.parseInt(
                    preferences.getString(context.getString(R.string.pref_smtp_port_key), "25"));
        } catch (NumberFormatException e) {
            // TODO: tell user.
            Log.w(TAG, "Invalid SMTP port number, use default value (25).", e);
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        Session session = Session.getInstance(props, null);
        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, recipient);
            msg.setSubject(subject);
            msg.setSentDate(new Date(timestamp));
            msg.setText(content);
            Transport.send(msg);
            Log.i(TAG, "Email sent.");
        } catch (MessagingException e) {
            // TODO: tell user.
            Log.e(TAG, "Failed to send email.", e);
        }
    }

}
