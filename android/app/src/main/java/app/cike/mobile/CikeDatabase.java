package app.cike.mobile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class CikeDatabase extends SQLiteOpenHelper {
    CikeDatabase(Context context) { super(context, "cike_local.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE app_state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL, updated_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    String readState() {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT payload FROM app_state WHERE id=1", null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        }
    }
    boolean writeState(String payload) {
        ContentValues values = new ContentValues(); values.put("id", 1); values.put("payload", payload); values.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().insertWithOnConflict("app_state", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }
}
