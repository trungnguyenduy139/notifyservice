package trungnguyen.myapplication

import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.annotation.RequiresApi

class NotificationListenerExampleService : NotificationListenerService() {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")!!.toString()

        val intent = Intent("com.trungnguyen.myapplication.notificationlistenerexample")
        intent.putExtra("NOTIFY_TITLE", title)
        intent.putExtra("NOTIFY_TEXT", text)
        sendBroadcast(intent)

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

}
