package com.klinker.android.twitter.ui.compose;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.AutoCompetePeopleAdapter;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.QustomDialogBuilder;

public class ComposeDMActivity extends Compose {

    public void setUpLayout() {
        isDM = true;

        setContentView(R.layout.compose_dm_activity);

        setUpSimilar();

        contactEntry = (HoloEditText) findViewById(R.id.contact_entry);
        contactEntry.setVisibility(View.VISIBLE);

        String screenname = getIntent().getStringExtra("screenname");

        if (screenname != null) {
            contactEntry.setText("@" + screenname);
            contactEntry.setSelection(contactEntry.getText().toString().length());
        }

        autocomplete = new ListPopupWindow(context);
        autocomplete.setAnchorView(contactEntry);
        autocomplete.setHeight(toDP(110));
        autocomplete.setWidth(toDP(275));
        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context, data.getCursor(currentAccount, contactEntry.getText().toString()), contactEntry));
        autocomplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);

        autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                autocomplete.dismiss();
            }
        });

        contactEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                String searchText = contactEntry.getText().toString();

                try {
                    if (searchText.substring(searchText.length() - 1, searchText.length()).equals("@")) {
                        autocomplete.show();
                    } else if (searchText.substring(searchText.length() - 1, searchText.length()).equals(" ")) {
                        autocomplete.dismiss();
                    } else if (autocomplete.isShowing()) {
                        String[] split = searchText.split(" ");
                        String adapterText;
                        if (split.length > 1) {
                            adapterText = split[split.length - 1];
                        } else {
                            adapterText = split[0];
                        }
                        adapterText = adapterText.replace("@", "");
                        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context, data.getCursor(currentAccount, adapterText), contactEntry));
                    }
                } catch (Exception e) {
                    // there is no text
                    autocomplete.dismiss();
                }

            }
        });

        if (settings.addonTheme) {
            try {
                Resources resourceAddon = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                int back = resourceAddon.getIdentifier("reply_entry_background", "drawable", settings.addonThemePackage);
                contactEntry.setBackgroundDrawable(resourceAddon.getDrawable(back));
            } catch (Exception e) {
                // theme does not include a reply entry box
            }
        }

        ImageButton at = (ImageButton) findViewById(R.id.at_button);
        at.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context, sharedPrefs.getInt("current_account", 1)).
                        setTitle(getResources().getString(R.string.type_user)).
                        setTitleColor(getResources().getColor(R.color.app_color)).
                        setDividerColor(getResources().getColor(R.color.app_color));

                qustomDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                qustomDialogBuilder.setPositiveButton(getResources().getString(R.string.add_user), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        contactEntry.append(qustomDialogBuilder.text.getText().toString());
                    }
                });

                qustomDialogBuilder.show();
            }
        });

        reply.setHint(getResources().getString(R.string.compose_tweet_hint));

        if (!settings.useEmoji) {
            emojiButton.setVisibility(View.GONE);
        } else {
            emojiKeyboard.setAttached((HoloEditText) reply);

            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    }
                }
            });

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm = (InputMethodManager)getSystemService(
                                        INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button_changing});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    } else {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                emojiKeyboard.setVisibility(true);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.keyboard_button_changing});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    }
                }
            });
        }

    }

    public void setUpReplyText() {

    }

    public boolean doneClick() {
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        String status = editText.getText().toString();

        if(!contactEntry.getText().toString().contains(" ")) {
            // Check for blank text
            if (status.trim().length() > 0 && status.length() <= 140) {
                // update status
                sendStatus(status);
                return true;
            } else {
                if (editText.getText().length() <= 140) {
                    // EditText is empty
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        } else {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.one_recepient), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void sendStatus(String status) {
        new SendDirectMessage().execute(status);
    }

}
