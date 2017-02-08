package org.readium.sdk.android.biblemesh;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.math.BigInteger;

/**
 * Created by admin on 28/11/2016.
 */

public class DBHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "epubtitle.db";
	private static final int DATABASE_VERSION = 1;
	private static final String BOOKS_TABLE_CREATE =
			"CREATE TABLE books (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"bookID INTEGER, " +
					"fsize INTEGER, " +
					"lastUpdated BIGINT, " + //fix 64
					"author VARCHAR, " +
					"title VARCHAR, " +
					"coverHref VARCHAR, " +
					"rootURL VARCHAR, " +
					"downloadStatus INTEGER" +
					");";
	private static final String LOCATIONS_TABLE_CREATE =
			"CREATE TABLE locations (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"bookID INTEGER, " +
					"userID INTEGER, " +
					"lastUpdated BIGINT, " + //fix 64
					"elementCfi VARCHAR, " +
					"idref VARCHAR " +
					");";
	private static final String HIGHLIGHTS_TABLE_CREATE =
			"CREATE TABLE highlights (" +
					"id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"bookID INTEGER, " +
					"userID INTEGER, " +
					"lastUpdated BIGINT, " + //fix 64
					"color INTEGER, " +
					"cfi VARCHAR, " +
					"idref VARCHAR, " +
					"note VARCHAR, " +
					"annotationID INTEGER" +
					");";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public DBCursor getLocations(Integer userID) {
		String sql = "SELECT locations.*, books.title, books.author, books.coverHref, books.rootURL, books.downloadStatus, " +
				"books.fsize FROM locations left join books on locations.bookID = books.bookID " +
				"where locations.userID = " + userID.toString() + " order by locations.bookID asc";
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getLocation(Integer bookID) {
		String sql = "SELECT locations.* FROM locations where locations.bookID = "+bookID+" and locations.userID = " + LoginActivity.userID.toString();
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getHighlights(Integer bookID) {
		String sql = "SELECT highlights.* FROM highlights where highlights.userID = " + LoginActivity.userID.toString() + " and highlights.bookID = "+bookID.toString()+" order by highlights.cfi asc";
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getHighlight(Integer bookID, Integer annotationID) {
		String sql = "SELECT highlights.* FROM highlights where highlights.userID = " + LoginActivity.userID.toString() + " and highlights.bookID = "+bookID.toString()+" and highlights.annotationID = "+annotationID;
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getBook(Integer bookID) {
		String sql = "SELECT * FROM books where bookID = " + bookID.toString();
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public void removeHighlight(Integer id) {
		try {
			getWritableDatabase().execSQL("DELETE from highlights where id = ?", new Object[]{id});
		} catch (SQLException e) {
			Log.e("removing highlight", e.toString());
		}
	}

	/*public void removeHighlights(Integer bookID) {
		try {
			getWritableDatabase().execSQL("DELETE from highlights where bookID = ? and userID = ?", new Object[]{bookID, LoginActivity.userID});
		} catch (SQLException e) {
			Log.e("removing highlights", e.toString());
		}
	}*/

	public void insertHighlight(Integer bookID, String idref, String cfi, Integer color, String note, Long hupdated_at, Integer annotationID) {
		try {
			getWritableDatabase().execSQL("INSERT into highlights (id, bookID, userID, cfi, idref, color, note, lastUpdated, annotationID) values " +
					"(NULL, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {bookID, LoginActivity.userID, cfi, idref, color, note, hupdated_at, annotationID});
		} catch (SQLException e) {
			Log.e("inserting highlight", e.toString());
		}
	}

	public void updateHighlight(Integer highlightID, Integer bookID, Integer color, String note, Long hupdated_at, Integer annotationID) {
		try {
			getWritableDatabase().execSQL("UPDATE highlights set color = ?, note = ?, lastUpdated = ?, annotationID = ? where bookId = ? and userID = ? and id = ?",
					new Object[] {color, note, hupdated_at, annotationID, bookID, LoginActivity.userID, highlightID});
		} catch (SQLException e) {
			Log.e("updating highlight", e.toString());
		}
	}

	public void setLocation(Integer bookID, String idref, String elementCfi, Long updated_at) {
		try {
			getWritableDatabase().execSQL("UPDATE locations set idref = ?, elementCfi = ?, lastUpdated = ? where bookID = ? and userID = ?",
					new Object[] {idref, elementCfi, updated_at, bookID, LoginActivity.userID});
		} catch (SQLException e) {
			Log.e("Error setting location", e.toString());
		}
	}

	public void setDownloadStatus(Integer bookID, Integer status) {
		try {
			getWritableDatabase().execSQL("UPDATE books set downloadStatus = ? where bookID = ?", new Object[]{status, bookID});
		} catch (SQLException e) {
			Log.e("Error setting dstatus", e.toString());
		}
	}

	public void setDownloadFSize(Integer bookID, Integer fsize) {
		try {
			getWritableDatabase().execSQL("UPDATE books set fsize = ? where bookID = ?", new Object[] {fsize, bookID});
		} catch (SQLException e) {
			Log.e("Error setting dstatus", e.toString());
		}
	}

	public void insertLocation(Integer bookID, Integer userID) {
		try {
			getWritableDatabase().execSQL("Insert into locations (id, bookID, userID) values (NULL, ?,?)", new Object[]{bookID, userID});
		} catch (SQLException e) {
			Log.e("Error adding location", e.toString());
		}
	}

	public void insertBook(Integer bookID, String title, String author, String coverHref, String rootURL, String updatedAtStr) {
		try {
			getWritableDatabase().execSQL("Insert into books (id, bookID, title, author, coverHref, rootURL) values " +
					"(NULL, ?, ?, ?, ?, ?)", new Object[]{bookID, title, author, coverHref, rootURL});
		} catch (SQLException e) {
			Log.e("Error adding book", e.toString());
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v("db", "onCreate");
		try {
			db.execSQL(BOOKS_TABLE_CREATE);
			db.execSQL(LOCATIONS_TABLE_CREATE);
			db.execSQL(HIGHLIGHTS_TABLE_CREATE);
		} catch (SQLException e) {
			Log.v("db", e.toString());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v("db", "onUpgrade");
	}
}
