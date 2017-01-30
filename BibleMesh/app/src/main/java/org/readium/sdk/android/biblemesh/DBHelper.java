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
					"note VARCHAR " +
					");";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// TODO Auto-generated constructor stub
	}

	public DBCursor getLocations(Integer userID) {
		String sql = "SELECT locations.*, books.title, books.author, books.coverHref, books.rootURL, books.downloadStatus FROM locations left join books on locations.bookID = books.bookID where locations.userID = " + userID.toString() + " order by locations.bookID asc";// + sortBy.toString();
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getLocation(Integer userID, Integer bookID) {
		String sql = "SELECT locations.* FROM locations where locations.bookID = "+bookID+" and locations.userID = " + userID.toString();// + sortBy.toString();
		SQLiteDatabase d = getReadableDatabase();
		DBCursor c = (DBCursor) d.rawQueryWithFactory(
				new DBCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DBCursor getHighlights(Integer userID, Integer bookID) {
		String sql = "SELECT highlights.* FROM highlights where highlights.userID = " + userID.toString() + " and highlights.bookID = "+bookID.toString()+" order by highlights.cfi asc";
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

	public void removeHighlights(Integer bookID) {
		String sql = "DELETE from highlights where bookID = "+bookID.toString()+" and userID = "+LoginActivity.userID.toString();
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("removing highlights", e.toString());
		}
	}

	public void insertHighlight(Integer bookID, String idref, String cfi, Integer color, String note, Long hupdated_at) {
		String sql = "INSERT into highlights (id, bookID, userID, cfi, idref, color, note, lastUpdated) values " +
				"(NULL, "+bookID.toString()+", "+LoginActivity.userID.toString()+", '"+cfi+"', '"+idref+"', "+color.toString()+", '"+note+"', "+hupdated_at.toString()+")";
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("inserting highlight", e.toString());
		}
	}

	public void setLocation(Integer bookID, String idref, String elementCfi, Long updated_at) {
		String sql = "UPDATE locations set idref = '" +idref+"', elementCfi = '"+elementCfi+"', lastUpdated = "+updated_at+" where bookID = " + bookID.toString()+" and userID = "+LoginActivity.userID;
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("Error setting location", e.toString());
		}
	}

	public void setDownloadStatus(Integer bookID, Integer status) {
		String sql = "UPDATE books set downloadStatus = " +status+" where bookID = " + bookID.toString();
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("Error setting dstatus", e.toString());
		}
	}

	public void insertLocation(Integer bookID, Integer userID) {
		String sql = "Insert into locations (id, bookID, userID) values (NULL, " + bookID.toString() + "," + userID.toString() + ")";
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("Error adding location", e.toString());
		}
	}

	public void insertBook(Integer bookID, String title, String author, String coverHref, String rootURL, String updatedAtStr) {
		String sql = "Insert into books (id, bookID, title, author, coverHref, rootURL) values " +
				"(NULL, " + bookID.toString() + ",'" + title + "', '" + author + "', '" + coverHref + "', '" + rootURL + "')";
		//fixme test with titles with apostrophes
		try {
			getWritableDatabase().execSQL(sql);
		} catch (SQLException e) {
			Log.e("Error adding book", e.toString());
		}
	}

	/*public DeliveriesCursor getDeliveries(){
		String sql = "SELECT * FROM deliveries order by id asc";// + sortBy.toString();
		SQLiteDatabase d = getReadableDatabase();
		DeliveriesCursor c = (DeliveriesCursor) d.rawQueryWithFactory(
				new DeliveriesCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DeliveriesCursor setScanned(Integer deliveryID){
		String sql = "UPDATE deliveries set scanned = 1 where delivery_id = "+deliveryID;// + sortBy.toString();
		SQLiteDatabase d = getWritableDatabase();
		DeliveriesCursor c = (DeliveriesCursor) d.rawQueryWithFactory(
				new DeliveriesCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}

	public DeliveriesCursor setDelivered(Integer deliveryID, Integer stage){
		String sql = "UPDATE deliveries set delivered = "+stage+" where delivery_id = "+deliveryID;// + sortBy.toString();
		SQLiteDatabase d = getWritableDatabase();
		DeliveriesCursor c = (DeliveriesCursor) d.rawQueryWithFactory(
				new DeliveriesCursor.Factory(),
				sql,
				null,
				null);
		c.moveToFirst();
		return c;
	}*/

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		Log.v("db", "onUpgrade");
	}
}
