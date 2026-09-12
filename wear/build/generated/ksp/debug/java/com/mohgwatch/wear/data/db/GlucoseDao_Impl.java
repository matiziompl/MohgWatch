package com.mohgwatch.wear.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GlucoseDao_Impl implements GlucoseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GlucoseReadingEntity> __insertionAdapterOfGlucoseReadingEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public GlucoseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGlucoseReadingEntity = new EntityInsertionAdapter<GlucoseReadingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `readings` (`id`,`value`,`trendArrow`,`measurementColor`,`timestamp`,`isHigh`,`isLow`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GlucoseReadingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindDouble(2, entity.getValue());
        statement.bindLong(3, entity.getTrendArrow());
        statement.bindLong(4, entity.getMeasurementColor());
        statement.bindLong(5, entity.getTimestamp());
        final int _tmp = entity.isHigh() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.isLow() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM readings WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<GlucoseReadingEntity> readings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGlucoseReadingEntity.insert(readings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final GlucoseReadingEntity reading,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGlucoseReadingEntity.insert(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatest(final Continuation<? super GlucoseReadingEntity> $completion) {
    final String _sql = "SELECT * FROM readings ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GlucoseReadingEntity>() {
      @Override
      @Nullable
      public GlucoseReadingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfTrendArrow = CursorUtil.getColumnIndexOrThrow(_cursor, "trendArrow");
          final int _cursorIndexOfMeasurementColor = CursorUtil.getColumnIndexOrThrow(_cursor, "measurementColor");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsHigh = CursorUtil.getColumnIndexOrThrow(_cursor, "isHigh");
          final int _cursorIndexOfIsLow = CursorUtil.getColumnIndexOrThrow(_cursor, "isLow");
          final GlucoseReadingEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final int _tmpTrendArrow;
            _tmpTrendArrow = _cursor.getInt(_cursorIndexOfTrendArrow);
            final int _tmpMeasurementColor;
            _tmpMeasurementColor = _cursor.getInt(_cursorIndexOfMeasurementColor);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsHigh;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsHigh);
            _tmpIsHigh = _tmp != 0;
            final boolean _tmpIsLow;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLow);
            _tmpIsLow = _tmp_1 != 0;
            _result = new GlucoseReadingEntity(_tmpId,_tmpValue,_tmpTrendArrow,_tmpMeasurementColor,_tmpTimestamp,_tmpIsHigh,_tmpIsLow);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<GlucoseReadingEntity> observeLatest() {
    final String _sql = "SELECT * FROM readings ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<GlucoseReadingEntity>() {
      @Override
      @Nullable
      public GlucoseReadingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfTrendArrow = CursorUtil.getColumnIndexOrThrow(_cursor, "trendArrow");
          final int _cursorIndexOfMeasurementColor = CursorUtil.getColumnIndexOrThrow(_cursor, "measurementColor");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsHigh = CursorUtil.getColumnIndexOrThrow(_cursor, "isHigh");
          final int _cursorIndexOfIsLow = CursorUtil.getColumnIndexOrThrow(_cursor, "isLow");
          final GlucoseReadingEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final int _tmpTrendArrow;
            _tmpTrendArrow = _cursor.getInt(_cursorIndexOfTrendArrow);
            final int _tmpMeasurementColor;
            _tmpMeasurementColor = _cursor.getInt(_cursorIndexOfMeasurementColor);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsHigh;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsHigh);
            _tmpIsHigh = _tmp != 0;
            final boolean _tmpIsLow;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLow);
            _tmpIsLow = _tmp_1 != 0;
            _result = new GlucoseReadingEntity(_tmpId,_tmpValue,_tmpTrendArrow,_tmpMeasurementColor,_tmpTimestamp,_tmpIsHigh,_tmpIsLow);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getReadingsSince(final long since,
      final Continuation<? super List<GlucoseReadingEntity>> $completion) {
    final String _sql = "SELECT * FROM readings WHERE timestamp > ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<GlucoseReadingEntity>>() {
      @Override
      @NonNull
      public List<GlucoseReadingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfTrendArrow = CursorUtil.getColumnIndexOrThrow(_cursor, "trendArrow");
          final int _cursorIndexOfMeasurementColor = CursorUtil.getColumnIndexOrThrow(_cursor, "measurementColor");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfIsHigh = CursorUtil.getColumnIndexOrThrow(_cursor, "isHigh");
          final int _cursorIndexOfIsLow = CursorUtil.getColumnIndexOrThrow(_cursor, "isLow");
          final List<GlucoseReadingEntity> _result = new ArrayList<GlucoseReadingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GlucoseReadingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final float _tmpValue;
            _tmpValue = _cursor.getFloat(_cursorIndexOfValue);
            final int _tmpTrendArrow;
            _tmpTrendArrow = _cursor.getInt(_cursorIndexOfTrendArrow);
            final int _tmpMeasurementColor;
            _tmpMeasurementColor = _cursor.getInt(_cursorIndexOfMeasurementColor);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsHigh;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsHigh);
            _tmpIsHigh = _tmp != 0;
            final boolean _tmpIsLow;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLow);
            _tmpIsLow = _tmp_1 != 0;
            _item = new GlucoseReadingEntity(_tmpId,_tmpValue,_tmpTrendArrow,_tmpMeasurementColor,_tmpTimestamp,_tmpIsHigh,_tmpIsLow);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM readings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
