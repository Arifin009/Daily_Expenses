package com.ambufast.dailyexpenses

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "ExpenseDB", null, 1) {

    companion object {
        private const val TABLE_NAME = "transactions"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_DESCRIPTION = "description"
        private const val COL_AMOUNT = "amount"
        private const val COL_DATE = "date"
        private const val COL_IS_DEPOSIT = "isDeposit"
        private const val COL_MONTH_YEAR = "monthYear"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT,
                $COL_DESCRIPTION TEXT,
                $COL_AMOUNT TEXT,
                $COL_DATE TEXT,
                $COL_IS_DEPOSIT INTEGER,
                $COL_MONTH_YEAR TEXT
            );
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertTransaction(transaction: Transaction): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, transaction.category)
            put(COL_DESCRIPTION, transaction.description)
            put(COL_AMOUNT, transaction.amount)
            put(COL_DATE, transaction.date)
            put(COL_IS_DEPOSIT, if (transaction.isDeposit) 1 else 0)
            put(COL_MONTH_YEAR, transaction.monthYear)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun updateDeposit(monthYear: String, newAmount: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_AMOUNT, newAmount)
        }
        return db.update(
            TABLE_NAME,
            values,
            "$COL_IS_DEPOSIT = ? AND $COL_MONTH_YEAR = ?",
            arrayOf("1", monthYear)
        )
    }
    fun getTotalExpensesForDate(date: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(CAST($COL_AMOUNT AS REAL)) FROM $TABLE_NAME WHERE $COL_IS_DEPOSIT = 0 AND $COL_DATE = ?",
            arrayOf(date)
        )

        val total = if (cursor.moveToFirst()) {
            cursor.getDouble(0)
        } else {
            0.0
        }

        cursor.close()
        db.close()
        return total
    }
    fun resetAllData() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
        db.close()
    }

    fun getDepositForMonth(month: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT amount FROM transactions WHERE isDeposit = 1 AND monthYear = ? LIMIT 1",
            arrayOf(month)
        )

        val amount = if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null
        }

        cursor.close()
        db.close()
        return amount
    }

    fun getTotalExpensesForMonth(month: String): String {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM transactions WHERE isDeposit = 0 AND monthYear = ?",
            arrayOf(month)
        )

        val total = if (cursor.moveToFirst()) {
            cursor.getDouble(0)
        } else {
            0.0
        }

        cursor.close()
        db.close()
        return total.toString()
    }

    fun getAllTransactions(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COL_ID DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                val transaction = Transaction(
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
                    amount = cursor.getString(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                    isDeposit = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DEPOSIT)) == 1,
                    monthYear = cursor.getString(cursor.getColumnIndexOrThrow(COL_MONTH_YEAR))
                )
                list.add(transaction)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return list
    }
}
