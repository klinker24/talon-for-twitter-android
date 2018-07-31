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

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.utils.api_helper.DirectMessageDownload;

public class SecondDMRefreshService extends SimpleJobService {

    public static void startNow(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(SecondDMRefreshService.class)
                .setTag("second-dm-refresh-now")
                .setTrigger(Trigger.executionWindow(0,0))
                .build();

        dispatcher.mustSchedule(myJob);
    }

    @Override
    public int onRunJob(JobParameters parameters) {
        DirectMessageDownload.download(this, true, false);
        return 0;
    }
}