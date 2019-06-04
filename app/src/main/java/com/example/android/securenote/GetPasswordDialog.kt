package com.example.android.securenote

import kotlinx.android.synthetic.main.get_password.*
import kotlinx.android.synthetic.main.get_password.view.*

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup

class GetPasswordDialog : androidx.fragment.app.DialogFragment(), OnClickListener, TextWatcher {
    private var minPasswordLength: Int = 0
    private var passwordListener: OnPasswordListener? = null

    interface OnPasswordListener {
        fun onPasswordValid(requestType: Int, password: String)

        fun onPasswordCancel()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            passwordListener = context as OnPasswordListener
        } catch (e: ClassCastException) {
            throw IllegalArgumentException("Must implement OnPasswordListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.setTitle(R.string.get_password_label)
        val content = inflater.inflate(R.layout.get_password, container, false)

        content.cancel_button.setOnClickListener(this)
        content.ok_button.setOnClickListener(this)

        return content
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = arguments ?: return

        val verifyPassword = args.getBoolean(VERIFY_PASSWORD_REQUEST_PARAM, true)
        if (verifyPassword) {
            ok_button!!.isEnabled = false
            minPasswordLength = args.getInt(MIN_PASSWORD_LENGTH_REQUEST_PARAM, 0)
            if (minPasswordLength > 0) {
                password_text.hint = super.getString(R.string.password_hint_min_length,
                        minPasswordLength)
            }
            password_text.addTextChangedListener(this)
            password_verification_text!!.addTextChangedListener(this)
        } else {
            password_verification_text!!.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        password_text.text.clear()
        password_verification_text!!.text.clear()
        Log.d(TAG, "Cleared password fields")
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ok_button -> {
                val requestType = arguments!!.getInt(REQUEST_PARAM)
                val password = password_text.text.toString()
                passwordListener!!.onPasswordValid(requestType, password)
            }
            R.id.cancel_button -> passwordListener!!.onPasswordCancel()
            else -> throw IllegalArgumentException("Invalid Button")
        }
        // the passwords will be cleared during onPause()
        dismiss()
    }

    override fun afterTextChanged(s: Editable) {
        when {
            password_text.length() < minPasswordLength -> {
                Log.d(TAG, "Password too short")
                ok_button!!.isEnabled = false
            }
            password_text.length() != password_verification_text!!.length() -> {
                Log.d(TAG, "Passwords' length differs")
                ok_button!!.isEnabled = false
            }
            else -> {
                for (i in 0 until password_text.text.length) {
                    if (password_text.text[i] != password_verification_text!!.text[i]) {
                        Log.d(TAG, "Passwords differ")
                        ok_button!!.isEnabled = false
                        return
                    }
                }
                Log.d(TAG, "Passwords are the same")
                ok_button!!.isEnabled = true
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // ignored
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // ignored
    }

    companion object {
        private val TAG = GetPasswordDialog::class.java.simpleName

        const val VERIFY_PASSWORD_REQUEST_PARAM = "verifyPassword"
        const val MIN_PASSWORD_LENGTH_REQUEST_PARAM = "minPasswordLength"
        const val REQUEST_PARAM = "requestType"

        fun newInstance(requestType: Int,
                        minPasswordLength: Int,
                        verifyPassword: Boolean): GetPasswordDialog {
            val dialog = GetPasswordDialog()
            val args = Bundle()
            args.putBoolean(VERIFY_PASSWORD_REQUEST_PARAM, verifyPassword)
            args.putInt(MIN_PASSWORD_LENGTH_REQUEST_PARAM, minPasswordLength)
            args.putInt(REQUEST_PARAM, requestType)
            dialog.arguments = args

            return dialog
        }
    }
}
