/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.transaction;

import com.android.internal.telephony.Phone;
import com.google.android.mms.util.SqliteWrapper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;


/**
 * Container of transaction settings. Instances of this class are contained
 * within Transaction instances to allow overriding of the default APN
 * settings or of the MMS Client.
 */
public class TransactionSettings {
    private static final String TAG = "TransactionSettings";
    private static final boolean DEBUG = true;
    private static final boolean LOCAL_LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    private String mServiceCenter;
    private String mProxyAddress;
    private int mProxyPort = -1;

    /**
     * Constructor that uses the default settings of the MMS Client.
     *
     * @param context The context of the MMS Client
     */
    public TransactionSettings(Context context, String apnName) {
        String selection = (apnName != null)?
                Telephony.Carriers.APN + "='"+apnName+"'": null;
        
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current"),
                            null, selection, null, null);

        if (cursor == null) {
            Log.e(TAG, "Apn is not found in Database!");
            return;
        }

        try {
            while (cursor.moveToNext() && TextUtils.isEmpty(mServiceCenter)) {
                // Read values from APN settings
                if (isValidApnType(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)), Phone.APN_TYPE_MMS)) {
                    mServiceCenter = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC));
                    mProxyAddress = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY));
                    if (isProxySet()) {
                        String portString = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT));
                        try {
                            mProxyPort = Integer.parseInt(portString);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Bad port number format: " + portString, e);
                        }
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Constructor that overrides the default settings of the MMS Client.
     *
     * @param mmscUrl The MMSC URL
     * @param proxyAddr The proxy address
     * @param proxyPort The port used by the proxy address
     * immediately start a SendTransaction upon completion of a NotificationTransaction,
     * false otherwise.
     */
    public TransactionSettings(String mmscUrl, String proxyAddr, int proxyPort) {
        mServiceCenter = mmscUrl;
        mProxyAddress = proxyAddr;
        mProxyPort = proxyPort;
    }

    public String getMmscUrl() {
        return mServiceCenter;
    }

    public String getProxyAddress() {
        return mProxyAddress;
    }

    public int getProxyPort() {
        return mProxyPort;
    }

    public boolean isProxySet() {
        return (mProxyAddress != null) && (mProxyAddress.trim().length() != 0);
    }

    static private boolean isValidApnType(String types, String requestType) {
        String[] typeList;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            typeList = new String[1];
            typeList[0] = Phone.APN_TYPE_ALL;
        } else {
            typeList = types.split(",");
        }
        
        for (String t : typeList) {
            if (t.equals(requestType) || t.equals(Phone.APN_TYPE_ALL)) {
                return true;
            }
        }
        return false;
    }
}
