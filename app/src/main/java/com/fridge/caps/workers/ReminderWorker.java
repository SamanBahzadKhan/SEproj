package com.fridge.caps.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fridge.caps.controllers.NotificationController;
import com.fridge.caps.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Schedules a Firestore reminder notification ~24h before an appointment.
 */
public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    public static final String KEY_STUDENT_ID     = "studentId";
    public static final String KEY_COUNSELOR_NAME = "counselorName";
    public static final String KEY_DATE           = "date";
    public static final String KEY_TIME           = "time";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String studentId = getInputData().getString(KEY_STUDENT_ID);
        String counselorName = getInputData().getString(KEY_COUNSELOR_NAME);
        String date = getInputData().getString(KEY_DATE);
        String time = getInputData().getString(KEY_TIME);
        if (studentId == null || studentId.isEmpty()) {
            return Result.success();
        }
        String cn = counselorName != null ? counselorName : "your counsellor";
        String msg = "You have an appointment with Dr. " + cn
                + (time != null && !time.isEmpty() ? " at " + time : "") + ".";
        new NotificationController().sendReminder(studentId, cn, date, time, msg);
        return Result.success();
    }

    /**
     * If appointment is more than 24h away, enqueue a one-time work to fire ~24h before.
     */
    public static void scheduleIfFuture(Context context, String studentId, String counselorName,
                                        String dateYmd, String startTime) {
        if (dateYmd == null || dateYmd.isEmpty() || startTime == null || startTime.isEmpty()) {
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    DateUtils.STORAGE_DATE + " " + DateUtils.STORAGE_TIME, Locale.US);
            Date appt = sdf.parse(dateYmd + " " + startTime);
            if (appt == null) return;
            long delayMs = appt.getTime() - System.currentTimeMillis()
                    - TimeUnit.HOURS.toMillis(24);
            if (delayMs <= 0) {
                return;
            }
            Data data = new Data.Builder()
                    .putString(KEY_STUDENT_ID, studentId)
                    .putString(KEY_COUNSELOR_NAME, counselorName != null ? counselorName : "")
                    .putString(KEY_DATE, dateYmd)
                    .putString(KEY_TIME, startTime)
                    .build();
            OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();
            WorkManager.getInstance(context.getApplicationContext()).enqueue(req);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage() != null ? e.getMessage() : "parse");
        }
    }
}
