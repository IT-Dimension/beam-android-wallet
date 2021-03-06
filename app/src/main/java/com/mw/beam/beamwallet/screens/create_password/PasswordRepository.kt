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

package com.mw.beam.beamwallet.screens.create_password

import com.mw.beam.beamwallet.base_screen.BaseRepository
import com.mw.beam.beamwallet.core.Api
import com.mw.beam.beamwallet.core.App
import com.mw.beam.beamwallet.core.AppConfig
import com.mw.beam.beamwallet.core.helpers.Status
import com.mw.beam.beamwallet.core.helpers.methodName
import com.mw.beam.beamwallet.core.helpers.removeDatabase
import com.mw.beam.beamwallet.core.utils.LogUtils

/**
 * Created by vain onnellinen on 10/23/18.
 */
class PasswordRepository : BaseRepository(), PasswordContract.Repository {

    override fun createWallet(pass: String?, phrases: String?): Status {
        var result = Status.STATUS_ERROR

        if (!pass.isNullOrBlank() && phrases != null) {
            if (Api.isWalletInitialized(AppConfig.DB_PATH)) {
                removeDatabase()
            }

            AppConfig.NODE_ADDRESS = Api.getDefaultPeers().random()
            App.wallet = Api.createWallet(AppConfig.NODE_ADDRESS, AppConfig.DB_PATH, pass, phrases)

            if (wallet != null) {
                //TODO handle statuses of process
                wallet!!.syncWithNode()
                result = Status.STATUS_OK
            }
        }

        LogUtils.logResponse(result, object {}.methodName())
        return result
    }

    override fun checkPass(pass: String?): Boolean {
        return wallet?.checkWalletPassword(pass ?: return false) ?: false
    }

    override fun changePass(pass: String?) {
        wallet?.changeWalletPassword(pass ?: return)
    }
}
