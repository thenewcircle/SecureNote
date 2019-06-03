package com.example.android.securenote

import android.app.Activity
import android.app.DialogFragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText

class GetPasswordDialog : DialogFragment(), OnClickListener, TextWatcher {

    private var mPassword: EditText? = null
    private var mPasswordVerification: EditText? = null
    private var mOkButton: Button? = null
    private var mMinPasswordLength: Int = 0

    private var mPasswordListener: OnPasswordListener? = null

    interface OnPasswordListener {
        fun onPasswordValid(requestType: Int, password: String)

        fun onPasswordCancel()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            mPasswordListener = activity as OnPasswordListener
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                    activity.javaClass.simpleName + " shoud implement OnPasswordListener")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        dialog.setTitle(R.string.get_password_label)
        val content = inflater.inflate(R.layout.get_password, container, false)

        mPassword = content.findViewById(R.id.password_text)
        mPasswordVerification = content.findViewById(R.id.password_verification_text)
        mOkButton = content.findViewById(R.id.ok_button)

        content.findViewById<View>(R.id.cancel_button).setOnClickListener(this)
        mOkButton!!.setOnClickListener(this)


        return content
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val args = arguments

        val verifyPassword = args.getBoolean(VERIFY_PASSWORD_REQUEST_PARAM, true)
        if (verifyPassword) {
            mOkButton!!.isEnabled = false
            mMinPasswordLength = args.getInt(MIN_PASSWORD_LENGTH_REQUEST_PARAM, 0)
            if (mMinPasswordLength > 0) {
                mPassword!!.hint = super.getString(R.string.password_hint_min_length,
                        mMinPasswordLength)
            }
            mPassword!!.addTextChangedListener(this)
            mPasswordVerification!!.addTextChangedListener(this)
        } else {
            mPasswordVerification!!.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        mPassword!!.text.clear()
        mPasswordVerification!!.text.clear()
        Log.d(TAG, "Cleared password fields")
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ok_button -> {
                val requestType = arguments.getInt(REQUEST_PARAM)
                val password = mPassword!!.text.toString()
                mPasswordListener!!.onPasswordValid(requestType, password)
            }
            R.id.cancel_button -> mPasswordListener!!.onPasswordCancel()
            else -> throw IllegalArgumentException("Invalid Button")
        }
        // the passwords will be cleared during onPause()
        dismiss()
    }

    override fun afterTextChanged(s: Editable) {
        if (mPassword!!.length() < mMinPasswordLength) {
            Log.d(TAG, "Password too short")
            mOkButton!!.isEnabled = false
        } else if (mPassword!!.length() != mPasswordVerification!!.length()) {
            Log.d(TAG, "Passwords' length differs")
            mOkButton!!.isEnabled = false
        } else {
            for (i in 0 until mPassword!!.text.length) {
                if (mPassword!!.text[i] != mPasswordVerification!!.text[i]) {
                    Log.d(TAG, "Passwords differ")
                    mOkButton!!.isEnabled = false
                    return
                }
            }
            Log.d(TAG, "Passwords are the same")
            mOkButton!!.isEnabled = true
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

        val VERIFY_PASSWORD_REQUEST_PARAM = "verifyPassword"
        val MIN_PASSWORD_LENGTH_REQUEST_PARAM = "minPasswordLength"
        val REQUEST_PARAM = "requestType"

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
