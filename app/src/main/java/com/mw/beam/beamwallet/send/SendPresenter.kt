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

package com.mw.beam.beamwallet.send

import com.mw.beam.beamwallet.base_screen.BasePresenter
import com.mw.beam.beamwallet.core.helpers.convertToGroth
import io.reactivex.disposables.Disposable

/**
 * Created by vain onnellinen on 11/13/18.
 */
class SendPresenter(currentView: SendContract.View, currentRepository: SendContract.Repository, private val state: SendState)
    : BasePresenter<SendContract.View, SendContract.Repository>(currentView, currentRepository),
        SendContract.Presenter {
    private lateinit var walletStatusSubscription: Disposable
    private val tokenRegex = Regex("[^A-Fa-f0-9]")
    private val MAX_TOKEN_LENGTH = 80

    override fun onViewCreated() {
        super.onViewCreated()
        view?.init()
    }

    override fun onSend() {
        if (view?.hasErrors(state.walletStatus?.available ?: 0) == false) {
            val amount = view?.getAmount()
            val fee = view?.getFee()
            val comment = view?.getComment()
            val token = view?.getToken()

            if (amount != null && fee != null && token != null && state.isTokenValid) {
                repository.sendMoney(token, comment, amount.convertToGroth(), fee)
                view?.close()
            }
        }
    }

    override fun onTokenChanged(rawToken: String?) {
        var clearedToken = rawToken?.replace(tokenRegex, "")

        if (!clearedToken.isNullOrEmpty() && clearedToken.length > MAX_TOKEN_LENGTH) {
            clearedToken = clearedToken.substring(0, MAX_TOKEN_LENGTH)
        }

        if (rawToken == clearedToken) {
            val isTokenEmpty = rawToken.isNullOrEmpty()

            if (isTokenEmpty != state.isTokenEmpty) {
                view?.updateUI(!isTokenEmpty)
            }

            if (!isTokenEmpty) {
                if (repository.checkAddress(rawToken)) {
                    view?.clearAddressError()
                    state.isTokenValid = true
                } else {
                    view?.setAddressError()
                    state.isTokenValid = false
                }
            } else {
                state.isTokenValid = false
            }

            state.isTokenEmpty = isTokenEmpty
        } else {
            view?.clearToken(clearedToken)
        }
    }

    override fun onAmountChanged() {
        view?.clearErrors()
    }

    override fun initSubscriptions() {
        super.initSubscriptions()

        walletStatusSubscription = repository.getWalletStatus().subscribe {
            state.walletStatus = it
        }
    }

    override fun getSubscriptions(): Array<Disposable>? = arrayOf(walletStatusSubscription)

    override fun hasStatus(): Boolean = true
}
