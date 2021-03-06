/*
 * // Copyright 2018 Beam Development
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //    http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 */

package com.mw.beam.beamwallet.screens.send

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.InputFilter
import android.view.View
import com.google.zxing.integration.android.IntentIntegrator
import com.mw.beam.beamwallet.R
import com.mw.beam.beamwallet.base_screen.BaseActivity
import com.mw.beam.beamwallet.base_screen.BasePresenter
import com.mw.beam.beamwallet.base_screen.MvpRepository
import com.mw.beam.beamwallet.base_screen.MvpView
import com.mw.beam.beamwallet.core.helpers.PermissionStatus
import com.mw.beam.beamwallet.core.helpers.PermissionsHelper
import com.mw.beam.beamwallet.core.helpers.convertToBeam
import com.mw.beam.beamwallet.core.helpers.convertToBeamString
import com.mw.beam.beamwallet.core.watchers.AmountFilter
import com.mw.beam.beamwallet.core.watchers.TextWatcher
import com.mw.beam.beamwallet.screens.qr.ScanQrActivity
import kotlinx.android.synthetic.main.activity_send.*
import java.text.DecimalFormat


/**
 * Created by vain onnellinen on 11/13/18.
 */
class SendActivity : BaseActivity<SendPresenter>(), SendContract.View {
    private lateinit var presenter: SendPresenter
    private lateinit var tokenWatcher: TextWatcher
    private lateinit var amountWatcher: TextWatcher
    private lateinit var feeFocusListener: View.OnFocusChangeListener

    override fun onControllerGetContentLayoutId() = R.layout.activity_send
    override fun getToolbarTitle(): String? = getString(R.string.send_title)

    override fun getAmount(): Double = amount.text.toString().toDouble()
    override fun getToken(): String = token.text.toString()
    override fun getComment(): String? = comment.text.toString()
    override fun getFee(): Long {
        return try {
            fee.text.toString().toLong()
        } catch (exception: NumberFormatException) {
            0
        }
    }

    override fun init() {
        token.requestFocus()
    }

    override fun addListeners() {
        btnSend.setOnClickListener {
            presenter.onSend()
        }

        scanQR.setOnClickListener {
            presenter.onScanQrPressed()
        }

        tokenWatcher = object : TextWatcher {
            override fun afterTextChanged(token: Editable?) {
                presenter.onTokenChanged(token.toString())
            }
        }
        token.addTextChangedListener(tokenWatcher)

        amountWatcher = object : TextWatcher {
            override fun afterTextChanged(token: Editable?) {
                presenter.onAmountChanged()
            }
        }
        amount.addTextChangedListener(amountWatcher)
        amount.filters = Array<InputFilter>(1) { AmountFilter() }

        //TODO presenter
        feeFocusListener = View.OnFocusChangeListener { _, isFocused ->
            if (!isFocused) {
                if (fee.text.toString().isEmpty()) {
                    fee.setText(getString(R.string.send_common_fee))
                }
            }
        }
        fee.onFocusChangeListener = feeFocusListener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter.onScannedQR(IntentIntegrator.parseActivityResult(resultCode, data).contents)
    }

    override fun isPermissionGranted(): Boolean {
        return PermissionsHelper.requestPermissions(this, PermissionsHelper.PERMISSIONS_CAMERA, PermissionsHelper.REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        var isGranted = true

        for ((index, permission) in permissions.withIndex()) {
            if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                isGranted = false
                if (!shouldShowRequestPermissionRationale(permission)) {
                    presenter.onRequestPermissionsResult(PermissionStatus.NEVER_ASK_AGAIN)
                } else if (PermissionsHelper.PERMISSIONS_CAMERA == permission) {
                    presenter.onRequestPermissionsResult(PermissionStatus.DECLINED)
                }
            }
        }

        if (isGranted) {
            presenter.onRequestPermissionsResult(PermissionStatus.GRANTED)
        }
    }

    override fun showPermissionRequiredAlert() {
        showAlert(message = getString(R.string.send_permission_required_message),
                btnConfirmText = getString(R.string.send_permission_required_settings),
                onConfirm = { showAppDetailsPage() },
                title = getString(R.string.send_permission_required_title),
                btnCancelText = getString(R.string.common_cancel))

    }

    override fun showNotBeamAddressError() {
        showSnackBar(getString(R.string.send_error_not_beam_address))
    }

    override fun scanQR() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.captureActivity = ScanQrActivity::class.java
        integrator.setBeepEnabled(false)
        integrator.initiateScan()
    }

    override fun hasErrors(availableAmount: Long): Boolean {
        val feeAmount = try {
            fee.text.toString().toLong().convertToBeam()
        } catch (exception: NumberFormatException) {
            0.0
        }
        var hasErrors = false
        clearErrors()

        try {
            if (amount.text.toString().toDouble() + feeAmount > availableAmount.convertToBeam()) {
                configAmountError(String.format(getString(R.string.send_amount_overflow_error), DecimalFormat("#.########").format(availableAmount.convertToBeam() - feeAmount)))
                hasErrors = true
            }
        } catch (exception: NumberFormatException) {
            configAmountError(String.format(getString(R.string.send_amount_overflow_error), availableAmount.convertToBeamString()))
            hasErrors = true
        }

        if (amount.text.isNullOrBlank()) {
            configAmountError(getString(R.string.send_amount_empty_error))
            hasErrors = true
        }

        try {
            if (amount.text.toString().toDouble() == 0.0) {
                configAmountError(getString(R.string.send_amount_zero_error))
                hasErrors = true
            }
        } catch (exception: NumberFormatException) {
            configAmountError(getString(R.string.send_amount_empty_error))
            hasErrors = true
        }

        return hasErrors
    }

    override fun setAddress(address: String) {
        token.setText(address)
    }

    override fun showCantSendToExpiredError() {
        showAlert(getString(R.string.send_error_expired_address), getString(R.string.common_ok), {})
    }

    override fun setAddressError() {
        tokenError.visibility = View.VISIBLE
        tokenDescription.visibility = View.GONE
    }

    override fun clearAddressError() {
        tokenError.visibility = View.INVISIBLE
        tokenDescription.visibility = View.VISIBLE
    }

    override fun clearToken(clearedToken: String?) {
        token.setText(clearedToken)
        token.setSelection(token.text.length)
    }

    override fun clearErrors() {
        amountError.visibility = View.GONE
        amount.setTextColor(ContextCompat.getColor(this, R.color.sent_color))
        amount.isStateNormal = true
    }

    override fun updateUI(shouldShowParams: Boolean) {
        params.visibility = if (shouldShowParams) View.VISIBLE else View.GONE

        if (shouldShowParams) {
            //clear previous input before showing to user
            amount.text = null
            comment.text = null
            fee.setText(getString(R.string.send_common_fee))
        } else {
            //can't attach this view to the params because constraint group forbid to change visibility of it's children
            amountError.visibility = View.GONE
            tokenError.visibility = View.INVISIBLE
            tokenDescription.visibility = View.VISIBLE
        }
    }

    override fun close() {
        finish()
    }

    override fun clearListeners() {
        btnSend.setOnClickListener(null)
        token.removeTextChangedListener(tokenWatcher)
        amount.removeTextChangedListener(amountWatcher)
        amount.filters = emptyArray()
        fee.onFocusChangeListener = null
    }

    private fun configAmountError(errorString: String) {
        amountError.visibility = View.VISIBLE
        amountError.text = errorString
        amount.setTextColor(ContextCompat.getColorStateList(this, R.color.text_color_selector))
        amount.isStateError = true
    }

    private fun showAppDetailsPage() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }

    override fun initPresenter(): BasePresenter<out MvpView, out MvpRepository> {
        presenter = SendPresenter(this, SendRepository(), SendState())
        return presenter
    }
}
