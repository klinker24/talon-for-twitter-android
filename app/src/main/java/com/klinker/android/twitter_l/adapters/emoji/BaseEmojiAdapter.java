/*
 * Copyright 2013 Luke Klinker
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

package com.klinker.android.twitter_l.adapters.emoji;

import android.content.Context;
import android.widget.BaseAdapter;

import com.klinker.android.twitter_l.views.widgets.EmojiKeyboard;

public abstract class BaseEmojiAdapter extends BaseAdapter {

    protected Context context;
    protected EmojiKeyboard keyboard;

    public BaseEmojiAdapter(Context context, EmojiKeyboard keyboard) {
        this.context = context;
        this.keyboard = keyboard;
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
