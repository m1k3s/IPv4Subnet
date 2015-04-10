package com.pbrane.mike.ipvxsubnet;

import android.app.Activity;
import android.content.DialogInterface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
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

class CustomIPvXKeyboard implements android.content.DialogInterface.OnClickListener {

    private KeyboardView keyboardView;
    private Activity hostActivity;
	private Keyboard ipv4Keyboard;
	private Keyboard ipv6Keyboard;
	private int keyboardState = R.integer.ipv4_mode;

    public CustomIPvXKeyboard(Activity host, int viewid, final int layoutid) {
        hostActivity = host;
		ipv4Keyboard = new Keyboard(hostActivity, layoutid, R.integer.ipv4_mode);
		ipv6Keyboard = new Keyboard(hostActivity, layoutid, R.integer.ipv6_mode);
        keyboardView = (KeyboardView) hostActivity.findViewById(viewid);
        keyboardView.setKeyboard(ipv4Keyboard);

        keyboardView.setPreviewEnabled(false); // do not show preview balloons

		OnKeyboardActionListener onKeyboardActionListener = new OnKeyboardActionListener() {

			// add special keys
			public final static int CodeDelete = -5; // Keyboard.KEYCODE_DELETE
			public final static int CodeHex = 55000; // used to switch between ipv4|ipv6 input mode
			public final static int CodeDColon = 55001; // double colon ::
			public final static int CodeSlash128 = 55002; // slash 128 /128
			public final static int CodeSlash64 = 55003; // slash 64 /64
			public final static int CodeBlank = 55004; // blank key

			@Override
			public void onKey(int primaryCode, int[] keyCodes) {
				View focusCurrent = hostActivity.getWindow().getCurrentFocus();
				if (focusCurrent == null || focusCurrent.getClass() != EditText.class) {
					return;
				}
				EditText edittext = (EditText) focusCurrent;
				Editable editable = edittext.getText();

				int start = edittext.getSelectionStart();
				// delete the selection, if chars are selected:
				int end = edittext.getSelectionEnd();
				if (end > start) {
					editable.delete(start, end);
				}
				// Apply the key to the edittext
				if (primaryCode == CodeDelete) {
					if (editable != null && start > 0) {
						editable.delete(start - 1, start);
					}
				} else if (primaryCode == CodeHex) {
					if (keyboardView != null) {
						if (keyboardState == R.integer.ipv4_mode) {
							if (ipv6Keyboard == null) {
								ipv6Keyboard = new Keyboard(hostActivity, layoutid, R.integer.ipv6_mode);
							}
							keyboardView.setKeyboard(ipv6Keyboard);
							keyboardState = R.integer.ipv6_mode;
						} else {
							if (ipv4Keyboard == null) {
								ipv4Keyboard = new Keyboard(hostActivity, layoutid, R.integer.ipv4_mode);
							}
							keyboardView.setKeyboard(ipv4Keyboard);
							keyboardState = R.integer.ipv4_mode;
						}
					}
				} else if (primaryCode == Keyboard.KEYCODE_DONE) {
					// We need to send the DONE to the _host_ activity to process the event
					hostActivity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
				} else if (primaryCode == CodeDColon) {
					editable.insert(start, "::");
				} else if (primaryCode == CodeSlash128) {
					editable.insert(start, "/128");
				} else if (primaryCode == CodeSlash64) {
					editable.insert(start, "/64");
				} else if (primaryCode == CodeBlank) {
					Log.i("CustomIPvXKeyboard", "Pressed a blank key");
				} else { // insert character
					editable.insert(start, Character.toString((char) primaryCode));
				}
			}

			@Override
			public void onPress(int arg0) {
			}

			@Override
			public void onRelease(int primaryCode) {
			}

			@Override
			public void onText(CharSequence text) {
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
		keyboardView.setOnKeyboardActionListener(onKeyboardActionListener);
        // Hide the default keyboard initially
        hostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public boolean isCustomKeyboardVisible() {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    // make the CustomKeyboard visible, and hide the system keyboard for view.
    public void showCustomKeyboard(View v  ) {
        keyboardView.setVisibility(View.VISIBLE);
        keyboardView.setEnabled(true);
        if (v != null) {
			((InputMethodManager) hostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
    }

    public void hideCustomKeyboard() {
        keyboardView.setVisibility(View.GONE);
        keyboardView.setEnabled(false);
    }

    public void registerEditText(int resid) {
        final EditText edittext= (EditText) hostActivity.findViewById(resid);
        // set the custom keyboard visible
        edittext.setOnFocusChangeListener(new OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit
            // box gets focus, but also hide it when the edit box loses focus
            @Override 
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
					showCustomKeyboard(v);
				} else {
					hideCustomKeyboard();
				}
            }
        });
        edittext.setOnClickListener(new OnClickListener() {
            // NOTE By setting the on click listener, we can show the custom keyboard again,
            // by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override 
            public void onClick(View v) {
                showCustomKeyboard(v);
            }
        });
        edittext.setOnTouchListener(new OnTouchListener() {
            @Override 
            public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // disable standard keyboard
                edittext.onTouchEvent(event);               // call native handler
                edittext.setInputType(inType);              // restore input type
                edittext.setCursorVisible(true);
                return true; // consume touch event
            }
        });
        // disable spell check
        edittext.setInputType(edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        //
        // Try to show cursor the complicated way:
        // @source http://androidpadanam.wordpress.com/2013/05/29/customkeyboard-example/
        // fixes the cursor not movable bug
        //
        OnTouchListener otl = new OnTouchListener() {
        	 
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!(isCustomKeyboardVisible())) {
                    showCustomKeyboard(v);
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Layout layout = ((EditText) v).getLayout();
                        float x = event.getX() + edittext.getScrollX();
                        int offset = layout.getOffsetForHorizontal(0, x);
                        if (offset > 0) {
							if (x > layout.getLineMax(0)) {
								edittext.setSelection(offset);     // touch was at the end of the text
							} else {
								edittext.setSelection(offset - 1);
							}
						}
                        break;
                    case MotionEvent.ACTION_MOVE:
                        layout = ((EditText) v).getLayout();
                        x = event.getX() + edittext.getScrollX();
                        offset = layout.getOffsetForHorizontal(0, x);
                        if (offset > 0) {
							if (x > layout.getLineMax(0)) {
								edittext.setSelection(offset);     // Touchpoint was at the end of the text
							} else {
								edittext.setSelection(offset - 1);
							}
						}
                        break;
 
                }
                return true;
            }
        };
        edittext.setOnTouchListener(otl); 
    }
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
	}
}