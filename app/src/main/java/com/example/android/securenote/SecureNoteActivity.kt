package com.example.android.securenote

import java.io.InputStream
import java.io.OutputStream

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast

import com.example.android.securenote.crypto.PasswordEncryptor
import com.example.android.securenote.crypto.RSAHardwareEncryptor

class SecureNoteActivity : Activity(), OnClickListener, TextWatcher, GetPasswordDialog.OnPasswordListener {

    private var noteText: EditText? = null
    private var resultText: TextView? = null
    private var encryptionSelect: RadioGroup? = null
    private var saveButton: Button? = null

    private var passwordEncryptor: PasswordEncryptor? = null
    private var hardwareEncryptor: RSAHardwareEncryptor? = null

    private val isSecureNoteFilePresent: Boolean
        get() = getFileStreamPath(FILENAME).exists()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.secure_note)

        noteText = findViewById(R.id.note_text)
        resultText = findViewById(R.id.text_result)
        encryptionSelect = findViewById(R.id.type_select)
        saveButton = findViewById(R.id.save_button)

        findViewById<View>(R.id.load_button).setOnClickListener(this)
        saveButton!!.setOnClickListener(this)
        noteText!!.addTextChangedListener(this)
        noteText!!.text = null

        passwordEncryptor = PasswordEncryptor()
        hardwareEncryptor = RSAHardwareEncryptor(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.secure_note, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete_button).isEnabled = this.isSecureNoteFilePresent
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_button -> {
                AlertDialog.Builder(this)
                        .setMessage(R.string.delete_alert)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes
                        ) { dialog, id -> deleteSecureNote() }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getPassword(requestCode: Int, verifyPasswords: Boolean) {
        Log.d(TAG, "Getting password")
        val dialog = GetPasswordDialog.newInstance(requestCode,
                6, verifyPasswords)
        dialog.show(fragmentManager,
                GetPasswordDialog::class.java.simpleName)
    }

    override fun onPasswordValid(requestType: Int, password: String) {
        when (requestType) {
            GET_PASSWORD_FOR_LOAD -> this.loadSecureNote(password)
            GET_PASSWORD_FOR_SAVE -> this.saveSecureNote(password)
        }
    }

    override fun onPasswordCancel() {
        Log.d(TAG, "Canceled result. Ignoring.")
    }

    override fun onClick(v: View) {
        val encryptionType = encryptionSelect!!.checkedRadioButtonId
        when (v.id) {
            R.id.load_button -> if (encryptionType == R.id.type_password) {
                getPassword(GET_PASSWORD_FOR_LOAD, false)
            } else {
                loadSecureNote(null)
            }
            R.id.save_button -> if (encryptionType == R.id.type_password) {
                getPassword(GET_PASSWORD_FOR_SAVE, true)
            } else {
                saveSecureNote(null)
            }
            else -> throw IllegalArgumentException("Invalid Button")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        noteText!!.text.clear()
    }

    private fun deleteSecureNote() {
        Log.d(TAG, "Deleting note")
        if (super.getFileStreamPath(FILENAME).delete()) {
            toast(R.string.deleted_note)
            Log.d(TAG, "Deleted note")
        } else {
            toast(R.string.failed_to_delete)
            Log.e(TAG, "Failed to delete note")
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun saveSecureNote(passkey: String?) {
        Log.d(TAG, "Saving note")
        object : AsyncTask<String, Void, Boolean>() {
            override fun doInBackground(vararg strings: String): Boolean? {
                try {
                    val out = openFileOutput(FILENAME, Context.MODE_PRIVATE)
                    val noteData = strings[0].toByteArray()
                    if (passkey == null) {
                        hardwareEncryptor!!.encryptData(noteData, out)
                    } else {
                        passwordEncryptor!!.encryptData(passkey, noteData, out)
                    }
                    Log.d(TAG, "Saved note to $FILENAME")

                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save note to $FILENAME", e)
                    getFileStreamPath(FILENAME).delete()
                    return false
                }

            }

            override fun onPostExecute(result: Boolean?) {
                if (result!!) {
                    noteText!!.text.clear()
                    toast(R.string.saved_note)
                } else {
                    toast(R.string.failed_to_save)
                }
            }

        }.execute(noteText!!.text.toString())
    }

    @SuppressLint("StaticFieldLeak")
    private fun loadSecureNote(passkey: String?) {
        Log.d(TAG, "Loading note...")
        object : AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg params: Void): String? {
                try {
                    val `in` = openFileInput(FILENAME)
                    val decrypted: ByteArray
                    if (passkey == null) {
                        decrypted = hardwareEncryptor!!.decryptData(`in`)
                    } else {
                        decrypted = passwordEncryptor!!.decryptData(passkey, `in`)
                    }
                    Log.d(TAG, "Loaded note from $FILENAME")
                    return String(decrypted)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load note from $FILENAME", e)
                    return null
                }

            }

            override fun onPostExecute(result: String?) {
                if (result == null) {
                    toast(R.string.failed_to_load)
                } else {
                    resultText!!.text = result
                    toast(R.string.loaded_note)
                }
            }
        }.execute()
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
    }

    override fun afterTextChanged(s: Editable) {
        saveButton!!.isEnabled = s.length != 0
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    companion object {
        private val TAG = SecureNoteActivity::class.java.simpleName

        private val FILENAME = "secure.note"

        /* Password Activity Actions */
        private val GET_PASSWORD_FOR_LOAD = 1
        private val GET_PASSWORD_FOR_SAVE = 2
    }
}
