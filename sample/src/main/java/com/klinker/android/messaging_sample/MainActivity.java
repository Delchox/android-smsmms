/*
 * Copyright 2014 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.messaging_sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.Telephony;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.klinker.android.logger.Log;
import com.klinker.android.logger.OnLogListener;
import com.klinker.android.send_message.ApnUtils;
import com.klinker.android.send_message.BroadcastUtils;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Transaction;
import com.klinker.android.send_message.Utils;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends Activity {

    private final static int REQUEST_CODE_OPEN_DIRECTORY_TREE = 0x12e;
    private Settings settings;

    private Button setDefaultAppButton;
    private Button selectApns;
    private EditText fromField;
    private EditText toField;
    private EditText messageField;
    private ImageView imageToSend;
    private Button sendButton;
    private RecyclerView log;

    private LogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("request_permissions", true) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        initSettings();
        initViews();
        initActions();
        initLogging();

        BroadcastUtils.sendExplicitBroadcast(this, new Intent(), "test action");
    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY_TREE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        String documentTreeUriId = DocumentsContract.getTreeDocumentId(uri);
                        String documentTreeStrUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentTreeUriId).toString();
                        processInitLogging(documentTreeStrUri);
                        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, flags);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                        sharedPreferences.edit().putBoolean("grand_uri_permissions", true).apply();
                    }
                }
            }
        }
    }

    private void initSettings() {
        settings = Settings.get(this);

        if (TextUtils.isEmpty(settings.getMmsc()) &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            initApns();
        }
    }

    private void initApns() {
        ApnUtils.initDefaultApns(this, new ApnUtils.OnApnFinishedListener() {
            @Override
            public void onFinished() {
                settings = Settings.get(MainActivity.this, true);
            }
        });
    }

    private void initViews() {
        setDefaultAppButton = (Button) findViewById(R.id.set_as_default);
        selectApns = (Button) findViewById(R.id.apns);
        fromField = (EditText) findViewById(R.id.from);
        toField = (EditText) findViewById(R.id.to);
        messageField = (EditText) findViewById(R.id.message);
        imageToSend = (ImageView) findViewById(R.id.image);
        sendButton = (Button) findViewById(R.id.send);
        log = (RecyclerView) findViewById(R.id.log);
    }

    private void initActions() {
        if (Utils.isDefaultSmsApp(this)) {
            setDefaultAppButton.setVisibility(View.GONE);
        } else {
            setDefaultAppButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDefaultSmsApp();
                }
            });
        }

        selectApns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initApns();
            }
        });

        fromField.setText(Utils.getMyPhoneNumber(this));
        toField.setText(Utils.getMyPhoneNumber(this));

        imageToSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSendImage();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        log.setHasFixedSize(false);
        log.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(new ArrayList<String>());
        log.setAdapter(logAdapter);
    }

    private void initLogging() {
        if (Build.VERSION.SDK_INT >= 29) {
            Log.setDebug(getContentResolver(), true);
            // SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            // boolean requestPermissions = sharedPreferences.getBoolean("grand_uri_permissions", false);
            // if (!requestPermissions) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY_TREE);
            // }
        } else {
            Log.setDebug(null, true);
            String path = Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getPath();
            processInitLogging(path);
        }
    }

    private void processInitLogging(String path) {
        Log.setPath(path);
        Log.setLogListener(new OnLogListener() {
            @Override
            public void onLogged(String tag, String message) {
                //logAdapter.addItem(tag + ": " + message);
            }
        });
    }

    private void setDefaultSmsApp() {
        setDefaultAppButton.setVisibility(View.GONE);
        Intent intent =
                new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                getPackageName());
        startActivity(intent);
    }

    private void toggleSendImage() {
        if (imageToSend.isEnabled()) {
            imageToSend.setEnabled(false);
            imageToSend.setAlpha(0.3f);
        } else {
            imageToSend.setEnabled(true);
            imageToSend.setAlpha(1.0f);
        }
    }

    public void sendMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                com.klinker.android.send_message.Settings sendSettings = new com.klinker.android.send_message.Settings();
                sendSettings.setMmsc(settings.getMmsc());
                sendSettings.setProxy(settings.getMmsProxy());
                sendSettings.setPort(settings.getMmsPort());
                sendSettings.setUseSystemSending(true);

                Transaction transaction = new Transaction(MainActivity.this, sendSettings);

                Message message = new Message(messageField.getText().toString(), toField.getText().toString());

                if (imageToSend.isEnabled()) {
                    message.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.android));
                }

                transaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
            }
        }).start();
    }

}
