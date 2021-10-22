package com.klinker.android.twitter_l.services.background_refresh;
/*
 * Copyright 2014 Luke Klinker
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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.utils.api_helper.DirectMessageDownload;

public class SecondDMRefreshService extends Worker {

    private final Context context;
    public SecondDMRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(SecondDMRefreshService.class).build());
    }

    @NonNull
    @Override
    public Result doWork() {
        DirectMessageDownload.download(context, true, false);
        return Result.success();
    }
}