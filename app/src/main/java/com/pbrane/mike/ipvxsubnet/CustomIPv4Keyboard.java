package com.pbrane.mike.ipvxsubnet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.DialogInterface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

// custom keyboard class based on SimplicityApks tutorial on XDA
// and Maarten Pennings CustomKeyboard class code.

class CustomIPv4Keyboard implements android.content.DialogInterface.OnClickListener {

    private KeyboardView keyboardView;
    private Activity hostActivity;
//    final CustomIPv4Keyboard INSTANCE = new CustomIPv4Keyboard();

    CustomIPv4Keyboard() { // avoids instantiation

    }

    void init(Activity host, int viewID, int layoutID) {
        hostActivity = host;
        keyboardView = (KeyboardView) hostActivity.findViewById (viewID);
        keyboardView.setKeyboard (new Keyboard (hostActivity, layoutID) );
        keyboardView.setPreviewEnabled (false); // do not show preview balloons

        OnKeyboardActionListener onKeyboardActionListener = new OnKeyboardActionListener() {
            @Override
            public void onKey (int primaryCode, int[] keyCodes) {
                View currentFocus = hostActivity.getWindow().getCurrentFocus();
                if (currentFocus == null || currentFocus.getClass() != EditText.class) {
                    return;
                }
                EditText edittext = (EditText) currentFocus;
                Editable editable = edittext.getText();

                int start = edittext.getSelectionStart();
                // delete the selection, if chars are selected:
                int end = edittext.getSelectionEnd();
                if (end > start) {
                    editable.delete (start, end);
                }
                // Apply the key to the edittext
                if (primaryCode == Keyboard.KEYCODE_DELETE) {
                    if (editable != null && start > 0) {
                        editable.delete (start - 1, start);
                    }
                } else if (primaryCode == Keyboard.KEYCODE_DONE) {
                    // We need to send the DONE to the _host_ activity to process the event
                    hostActivity.dispatchKeyEvent (new KeyEvent (KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER) );
                } else if (primaryCode == Keyboard.KEYCODE_ALT) { // clear the EditText and TextView
                    edittext.setText ("");
                    edittext.setHint (R.string.ip_hint);
                    ( (IPv4Activity) hostActivity).getTextView().setText ("");
                } else { // insert character
                    editable.insert (start, Character.toString ( (char) primaryCode) );
                }
            }

            @Override
            public void onPress (int primaryCode) {
            }

            @Override
            public void onRelease (int primaryCode) {
            }

            @Override
            public void onText (CharSequence text) {
            }

            @Override
            public void swipeDown() {
            }

            @Override
            public void swipeLeft() {
            }

            @Override
            public void swipeRight() {
            }

            @Override
            public void swipeUp() {
            }
        };
        keyboardView.setOnKeyboardActionListener (onKeyboardActionListener);
        // Hide the system keyboard initially
        hostActivity.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private boolean isCustomKeyboardVisible() {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    // make the CustomKeyboard visible with slide animation and fade in
    // and hide the system keyboard for view.
    private void showCustomKeyboard(View v) {
        keyboardView.animate().translationY (0).alpha (1.0f)
        .setListener (new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd (Animator animation) {
                super.onAnimationEnd (animation);
                keyboardView.setVisibility (View.VISIBLE);
                keyboardView.setEnabled (true);
            }
        });
        if (v != null) { // hide the system keyboard
            ( (InputMethodManager) hostActivity.getSystemService (Activity.INPUT_METHOD_SERVICE) ).hideSoftInputFromWindow (v.getWindowToken(), 0);
        }
    }

    // hide the CustomKeyboard with slide animation and fade out
    void hideCustomKeyboard() {
        keyboardView.animate().translationY (keyboardView.getHeight() ).alpha (0.0f)
        .setListener (new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd (Animator animation) {
                super.onAnimationEnd (animation);
                keyboardView.setVisibility (View.GONE);
                keyboardView.setEnabled (false);
            }
        });
    }

    void registerEditText(int resid) {
        final EditText edittext = (EditText) hostActivity.findViewById (resid);
        // set the custom keyboard visible
        edittext.setOnFocusChangeListener (new OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit
            // box gets focus, but also hide it when the edit box loses focus
            @Override
            public void onFocusChange (View v, boolean hasFocus) {
                if (hasFocus) {
                    showCustomKeyboard (v);
                } else {
                    hideCustomKeyboard();
                }
            }
        });
        edittext.setOnClickListener (new OnClickListener() {
            // NOTE By setting the on click listener, we can show the custom keyboard again,
            // by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override
            public void onClick (View v) {
                showCustomKeyboard (v);
            }
        });
        edittext.setOnTouchListener (new OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // backup the input type
                edittext.setInputType (InputType.TYPE_NULL); // disable standard keyboard
                edittext.onTouchEvent (event);              // call native handler
                edittext.setInputType (inType);             // restore input type
                edittext.setCursorVisible (true);
                return true; // consume touch event
            }
        });
        // disable spell check
        edittext.setInputType (edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        // show the cursor
        // http://androidpadanam.wordpress.com/2013/05/29/customkeyboard-example/
        OnTouchListener otl = new OnTouchListener() {

            @Override
            public boolean onTouch (View v, MotionEvent event) {
                if (! (isCustomKeyboardVisible() ) ) {
                    showCustomKeyboard (v);
                }
                switch (event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        Layout layout = ( (EditText) v).getLayout();
                        float x = event.getX() + edittext.getScrollX();
                        int offset = layout.getOffsetForHorizontal (0, x);
                        if (offset > 0) {
                            if (x > layout.getLineMax (0) ) {
                                edittext.setSelection (offset);    // touch was at the end of the text
                            } else {
                                edittext.setSelection (offset - 1);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        layout = ( (EditText) v).getLayout();
                        x = event.getX() + edittext.getScrollX();
                        offset = layout.getOffsetForHorizontal (0, x);
                        if (offset > 0) {
                            if (x > layout.getLineMax (0) ) {
                                edittext.setSelection (offset);    // Touchpoint was at the end of the text
                            } else {
                                edittext.setSelection (offset - 1);
                            }
                        }
                        break;

                }
                return true;
            }
        };
        edittext.setOnTouchListener (otl);
    }

    @Override
    public void onClick (DialogInterface dialog, int which) {
    }
}
