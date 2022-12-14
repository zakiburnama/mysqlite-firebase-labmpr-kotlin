package com.example.localdatabase

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.localdatabase.databinding.ActivityAddNewHomeworkBinding
import com.example.localdatabase.db.DatabaseContract
import com.example.localdatabase.db.HomeworkHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class AddNewHomeworkActivity : AppCompatActivity() {
    private var isEdit = false
    private var homework: Homework? = null
    private var position: Int = 0
    private lateinit var homeworkHelper: HomeworkHelper

    private lateinit var binding: ActivityAddNewHomeworkBinding

    private lateinit var database: DatabaseReference

    private var homeworkTitle: String? = null
    private var homeworkDescription: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewHomeworkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        homeworkHelper = HomeworkHelper.getInstance(applicationContext)
        homeworkHelper.open()

        // Cek apakah ada data homework
//        homework = intent.getParcelableExtra(EXTRA_HOMEWORK)
//        if (homework != null) {
//            position = intent.getIntExtra(EXTRA_POSITION, 0)
//            isEdit = true
//        } else {
//            homework = Homework()
//        }

        // CEK ada judul ato ga
        homeworkTitle = intent.getStringExtra(EXTRA_HOMEWORK_TITLE)
        if (homeworkTitle != null) {
            isEdit = true
            homeworkDescription = intent.getStringExtra(EXTRA_HOMEWORK_DESCRIPTION)
        }

        val actionBarTitle: String
        val btnTitle: String

        // Jika ada data pada homework bertati melakukan update
        if (isEdit) {
            actionBarTitle = "Ubah"
            btnTitle = "Update"

            binding.edtTitle.setText(homeworkTitle)
            binding.edtDescription.setText(homeworkDescription)
//            homework?.let {
//                binding.edtTitle.setText(it.title)
//                binding.edtDescription.setText(it.description)
//            }

        } else {
            actionBarTitle = "Tambah"
            btnTitle = "Simpan"
        }

        supportActionBar?.title = actionBarTitle
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSubmit.text = btnTitle

//        binding.btnSubmit.setOnClickListener { addNewHomework() }
        binding.btnSubmit.setOnClickListener { addNewHomeworkFirebase() }

//        binding.btnDelete.setOnClickListener { showAlertDialog(ALERT_DIALOG_DELETE) }
        binding.btnDelete.setOnClickListener { delete() }
    }

    fun delete() {
        database = Firebase.database.reference
        val title = binding.edtTitle.text.toString().trim()
        database.child("homework").child(title).removeValue()

        Toast.makeText(this, "Data firebase berhasil dihapus", Toast.LENGTH_SHORT).show()
        finish()

    }

    fun addNewHomeworkFirebase() {
        database = Firebase.database.reference

        val id = 0
        val title = binding.edtTitle.text.toString().trim()
        val description = binding.edtDescription.text.toString().trim()

        if (isEdit) {
            database.child("homework").child(title).child("description").setValue(description)
            Toast.makeText(this, "Data firebase berhasil diperbaharui", Toast.LENGTH_SHORT).show()
            finish()
        }
        else {
            val date = getCurrentDate()
            val homework = Homework(id, title, description, date)

            database.child("homework").child(title).setValue(homework)
            Toast.makeText(this, "Data firebase berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    fun addNewHomework() {
        val title = binding.edtTitle.text.toString().trim()
        val description = binding.edtDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.edtTitle.error = "Title tidak boleh kosong"
            return
        }

        homework?.title = title
        homework?.description = description

        val intent = Intent()
        intent.putExtra(EXTRA_HOMEWORK, homework)
        intent.putExtra(EXTRA_POSITION, position)

        val values = ContentValues()
        values.put(DatabaseContract.HomeworkColumns.TITLE, title)
        values.put(DatabaseContract.HomeworkColumns.DESCRIPTION, description)

        if (isEdit) {
            val result = homeworkHelper.update(homework?.id.toString(), values)
            if (result > 0) {
                setResult(RESULT_UPDATE, intent)
                finish()
            } else {
                Toast.makeText(this, "Gagal memperbaharui data", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            homework?.date = getCurrentDate()
            values.put(DatabaseContract.HomeworkColumns.DATE, getCurrentDate())
            val result = homeworkHelper.insert(values)

            if (result > 0) {
                homework?.id = result.toInt()
                setResult(RESULT_ADD, intent)
                finish()
            } else {
                Toast.makeText(this, "Gagal menambah data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val date = Date()

        return dateFormat.format(date)
    }

    override fun onBackPressed() {
        showAlertDialog(ALERT_DIALOG_CLOSE)
    }

    private fun showAlertDialog(type: Int) {
        val isDialogClose = type == ALERT_DIALOG_CLOSE
        val dialogTitle: String
        val dialogMessage: String

        if (isDialogClose) {
            dialogTitle = "Batal"
            dialogMessage = "Apakah anda ingin membatalkan memperbaharui data?"
        } else {
            dialogTitle = "Hapus Homework"
            dialogMessage = "Apakah anda yakin ingin menghapus item ini?"
        }

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (isDialogClose) {
                    finish()
                } else {
                    val result = homeworkHelper.deleteById(homework?.id.toString()).toLong()
                    if (result > 0) {
                        val intent = Intent()
                        intent.putExtra(EXTRA_POSITION, position)
                        setResult(RESULT_DELETE, intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal menghapus data", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    companion object {
        const val EXTRA_HOMEWORK = "extra_homework"
        const val EXTRA_POSITION = "extra_position"
        const val RESULT_ADD = 101
        const val RESULT_UPDATE = 201
        const val RESULT_DELETE = 301
        const val ALERT_DIALOG_CLOSE = 10
        const val ALERT_DIALOG_DELETE = 20

        const val EXTRA_HOMEWORK_TITLE = "extra_homework_title"
        const val EXTRA_HOMEWORK_DESCRIPTION = "extra_homework_title_description"
    }
}