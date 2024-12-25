/*
 * Copyright (C) 2015 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.messaging_sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.klinker.android.messaging_sample.R;

import java.util.Arrays;

public class PermissionActivity extends Activity {

    private static final int REQUEST_CODE_ASK_PERMISSIONS_WRITE_SETTINGS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE
        }, 0);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (processRequestPermissionsResult(grantResults)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.System.canWrite(this)) {
                requestPermissionsManageWriteSettings();
            } else {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean("request_permissions", false)
                        .commit();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        } else finish();
    }

    private void requestPermissionsManageWriteSettings() {
        new AlertDialog.Builder(this)
                .setMessage(com.klinker.android.send_message.R.string.write_settings_permission)
                .setPositiveButton(com.klinker.android.send_message.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        try {
                            startActivityForResult(intent, REQUEST_CODE_ASK_PERMISSIONS_WRITE_SETTINGS);
                        } catch (Exception e) {
                            Log.e("MainActivity", "error starting permission intent", e);
                        }
                    }
                }).show();
    }

    private boolean processRequestPermissionsResult(int[] grantResults) {
        boolean allGranted = true;
        for (int grantResult : grantResults) {
            if (grantResult != 0) {
                allGranted = false;
                break;
            }
        }
        return allGranted;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS_WRITE_SETTINGS) {
            if (android.provider.Settings.System.canWrite(this)) {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean("request_permissions", false)
                        .commit();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                finish();
            }
        }
    }

}
