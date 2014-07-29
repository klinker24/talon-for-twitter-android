package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.widget.EditText;
import com.klinker.android.twitter.AbstractTalonTest;
import com.klinker.android.twitter.adapters.AutoCompletePeopleAdapter;
import com.klinker.android.twitter_l.utils.AutoCompleteHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertEquals;

public class AutoCompleteHelperTest extends AbstractTalonTest {

    private AutoCompleteHelper helper;
    private EditText editText;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        helper = new AutoCompleteHelper();
        editText = new EditText(activity);
    }

    @Test
    public void test_autoCompleteEmptyUserName() {
        editText.setText("@");
        editText.setSelection(1);
        helper.completeTweet(editText, "klinker41", '@');
        assertEquals("@klinker41", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteEmptyHashtag() {
        editText.setText("#");
        editText.setSelection(1);
        helper.completeTweet(editText, "test", '#');
        assertEquals("#test", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteEmptyPartialName() {
        editText.setText("@klin");
        editText.setSelection(5);
        helper.completeTweet(editText, "klinker41", '@');
        assertEquals("@klinker41", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteEmptyPartialHashtag() {
        editText.setText("#te");
        editText.setSelection(3);
        helper.completeTweet(editText, "test", '#');
        assertEquals("#test", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteMiddle() {
        editText.setText("testing a username auto complete for @klin in the middle of a tweet");
        editText.setSelection(42);
        helper.completeTweet(editText, "klinker41", '@');
        assertEquals("testing a username auto complete for @klinker41 in the middle of a tweet", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteEnd() {
        editText.setText("test at end @talonA");
        editText.setSelection(19);
        helper.completeTweet(editText, "talonAndroid", '@');
        assertEquals("test at end @talonAndroid", editText.getText().toString());
    }

    @Test
    public void test_autoCompleteBeginning() {
        editText.setText("#test is fun!");
        editText.setSelection(5);
        helper.completeTweet(editText, "androidTesting", '#');
        assertEquals("#androidTesting is fun!", editText.getText().toString());
    }

}