package org.readium.sdk.android.biblemesh;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;

/**
 * Created by admin on 28/11/2016.
 */

public class DBCursor extends SQLiteCursor {

	private DBCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
	                 String editTable, SQLiteQuery query) {
		super(db, driver, editTable, query);
	}

	static class Factory implements SQLiteDatabase.CursorFactory {
		@Override
		public Cursor newCursor(SQLiteDatabase db,
		                        SQLiteCursorDriver driver, String editTable,
		                        SQLiteQuery query) {
			return new DBCursor(db, driver, editTable, query);
		}
	}

	public Integer getColID() {
		return getInt(getColumnIndexOrThrow("id"));
	}
	public Integer getColDownloadStatus() {
		return getInt(getColumnIndexOrThrow("downloadStatus"));
	}
	public Integer getColBookID() {
		return getInt(getColumnIndexOrThrow("bookID"));
	}
	public Integer getColFSize() {
		return getInt(getColumnIndexOrThrow("fsize"));
	}
	public String getColTitle() {
		return getString(getColumnIndexOrThrow("title"));
	}
	public String getColCoverHref() {
		return getString(getColumnIndexOrThrow("coverHref"));
	}
	public String getColElementCFI() {
		return getString(getColumnIndexOrThrow("elementCfi"));
	}
	public String getColCFI() {
		return getString(getColumnIndexOrThrow("cfi"));
	}
	public String getColIDRef() {
		return getString(getColumnIndexOrThrow("idref"));
	}
	public Long getColLastUpdated() {
		return getLong(getColumnIndexOrThrow("lastUpdated"));
	}
	public String getColAuthor() {
		return getString(getColumnIndexOrThrow("author"));
	}
	public String getColNote() {
		return getString(getColumnIndexOrThrow("note"));
	}
	public Integer getColColor() {
		return getInt(getColumnIndexOrThrow("color"));
	}

	/*public String getColTrackingno() {
		return getString(getColumnIndexOrThrow("trackingno"));
	}
	public String getColOrderno() {
		return getString(getColumnIndexOrThrow("orderno"));
	}

	public String getColFirstname() {
		return getString(getColumnIndexOrThrow("recipientfirstname"));
	}

	public String getColLastname() {
		return getString(getColumnIndexOrThrow("recipientlastname"));
	}

	public String getColAddressPC() {
		return getString(getColumnIndexOrThrow("addresspc"));
	}

	public String getColAddressLine1() {
		return getString(getColumnIndexOrThrow("addressline1"));
	}

	public String getColAddressLine2() {
		return getString(getColumnIndexOrThrow("addressline2"));
	}

	public String getColAddressLine3() {
		return getString(getColumnIndexOrThrow("addressline3"));
	}

	public Integer getColScanned() {
		return getInt(getColumnIndexOrThrow("scanned"));
	}
	public Integer getColSigreq() {
		return getInt(getColumnIndexOrThrow("sigreq"));
	}

	public Integer getColDeliveryID() {
		return getInt(getColumnIndexOrThrow("delivery_id"));
	}

	public Integer getColETA() {
		return getInt(getColumnIndexOrThrow("eta"));
	}
	public Integer getColSlotStart() {
		return getInt(getColumnIndexOrThrow("slotstart"));
	}
	public Integer getColSlotEnd() {
		return getInt(getColumnIndexOrThrow("slotend"));
	}
	public Integer getColDelivered() {
		return getInt(getColumnIndexOrThrow("delivered"));
	}
	public Integer getColCollection() {
		return getInt(getColumnIndexOrThrow("collection"));
	}*/
}
