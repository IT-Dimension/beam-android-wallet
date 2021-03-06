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
package com.mw.beam.beamwallet.core.listeners

import android.os.Handler
import android.os.Looper
import com.mw.beam.beamwallet.core.entities.*
import com.mw.beam.beamwallet.core.entities.dto.TxDescriptionDTO
import com.mw.beam.beamwallet.core.entities.dto.UtxoDTO
import com.mw.beam.beamwallet.core.entities.dto.WalletAddressDTO
import com.mw.beam.beamwallet.core.entities.dto.WalletStatusDTO
import com.mw.beam.beamwallet.core.helpers.ChangeAction
import com.mw.beam.beamwallet.core.helpers.methodName
import com.mw.beam.beamwallet.core.helpers.prepareForLog
import com.mw.beam.beamwallet.core.utils.LogUtils
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Created by vain onnellinen on 10/4/18.
 */
object WalletListener {
    private var uiHandler = Handler(Looper.getMainLooper())
    private val DUMMY_OBJECT = Any()

    var subOnStatus: Subject<WalletStatus> = BehaviorSubject.create<WalletStatus>().toSerialized()
    var subOnTxStatus: Subject<OnTxStatusData> = BehaviorSubject.create<OnTxStatusData>().toSerialized()
    var subOnSyncProgressUpdated: Subject<OnSyncProgressData> = BehaviorSubject.create<OnSyncProgressData>().toSerialized()
    var subOnRecoverProgressUpdated: Subject<Any> = BehaviorSubject.create<Any>().toSerialized()
    var subOnChangeCalculated: Subject<Any> = BehaviorSubject.create<Any>().toSerialized()
    var subOnAllUtxoChanged: Subject<List<Utxo>> = BehaviorSubject.create<List<Utxo>>().toSerialized()
    var subOnAddresses: Subject<OnAddressesData> = BehaviorSubject.create<OnAddressesData>().toSerialized()
    var subOnGeneratedNewAddress: Subject<WalletAddress> = BehaviorSubject.create<WalletAddress>().toSerialized()
    var subOnNodeConnectedStatusChanged: Subject<Boolean> = BehaviorSubject.create<Boolean>().toSerialized()
    var subOnNodeConnectionFailed: Subject<Any> = BehaviorSubject.create<Any>().toSerialized()
    var subOnCantSendToExpired: Subject<Any> = BehaviorSubject.create<Any>().toSerialized()

    @JvmStatic
    fun onStatus(status: WalletStatusDTO) = returnResult(subOnStatus, WalletStatus(status), object {}.methodName())

    @JvmStatic
    fun onTxStatus(action: Int, tx: Array<TxDescriptionDTO>?) = returnResult(subOnTxStatus, OnTxStatusData(ChangeAction.fromValue(action), tx?.map { TxDescription(it) }), object {}.methodName())

    @JvmStatic
    fun onSyncProgressUpdated(done: Int, total: Int) = returnResult(subOnSyncProgressUpdated, OnSyncProgressData(done, total), object {}.methodName())

    @JvmStatic
    fun onRecoverProgressUpdated(done: Int, total: Int, message: String) = returnResult(subOnRecoverProgressUpdated, DUMMY_OBJECT, object {}.methodName())

    @JvmStatic
    fun onChangeCalculated(amount: Long) = returnResult(subOnChangeCalculated, DUMMY_OBJECT, object {}.methodName())

    @JvmStatic
    fun onAllUtxoChanged(utxos: Array<UtxoDTO>?) = returnResult(subOnAllUtxoChanged, utxos?.map { Utxo(it) } ?: emptyList(), object {}.methodName())

    @JvmStatic
    fun onAddresses(own: Boolean, addresses: Array<WalletAddressDTO>?) = returnResult(subOnAddresses, OnAddressesData(own, addresses?.map { WalletAddress(it) }), object {}.methodName())

    @JvmStatic
    fun onGeneratedNewAddress(addr: WalletAddressDTO) = returnResult(subOnGeneratedNewAddress, WalletAddress(addr), object {}.methodName())

    @JvmStatic
    fun onNodeConnectedStatusChanged(isNodeConnected: Boolean) = returnResult(subOnNodeConnectedStatusChanged, isNodeConnected, object {}.methodName())

    @JvmStatic
    fun onNodeConnectionFailed() = returnResult(subOnNodeConnectionFailed, DUMMY_OBJECT, object {}.methodName())

    @JvmStatic
    fun onCantSendToExpired() = returnResult(subOnCantSendToExpired, DUMMY_OBJECT, object {}.methodName())

    private fun <T> returnResult(subject: Subject<T>, result: T, responseName: String) {
        uiHandler.post {
            try {
                subject.onNext(result)

                when (result) {
                    DUMMY_OBJECT -> LogUtils.logResponse("null", responseName)
                    is Array<*> -> LogUtils.logResponse(if (result.isEmpty()) "null" else result.toList().prepareForLog(), responseName)
                    else -> LogUtils.logResponse(result, responseName)
                }
            } catch (e: Exception) {
                LogUtils.logErrorResponse(e, responseName)
            }
        }
    }
}
