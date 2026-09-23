package app.cike.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Notification;
import android.app.NotificationManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, "cike_reminders") : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle("此刻 · 任务提醒").setContentText(title == null ? "你安排的任务时间到了。" : title).setAutoCancel(true);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify((intent.getStringExtra("id") == null ? 1 : intent.getStringExtra("id").hashCode()), builder.build());
    }
}
