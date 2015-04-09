/**
 * Copyright 2013 Maarten Pennings extended by SimplicityApks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software in a product, an acknowledgment in the product
 * documentation would be appreciated but is not required.
 */

package com.pbrane.mike.ipvxsubnet;

import android.app.Activity;
import android.content.DialogInterface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
//import android.util.Log;
//import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

class CustomKeyboard implements android.content.DialogInterface.OnClickListener {

    private KeyboardView mKeyboardView;
    private Activity     mHostActivity;
//    private boolean hapticFeedback;

    public CustomKeyboard(Activity host, int viewid, int layoutid) {
        mHostActivity= host;
        mKeyboardView= (KeyboardView)mHostActivity.findViewById(viewid);
        mKeyboardView.setKeyboard(new Keyboard(mHostActivity, layoutid));
        mKeyboardView.setPreviewEnabled(false); // NOTE Do not show the preview balloons

		OnKeyboardActionListener mOnKeyboardActionListener = new OnKeyboardActionListener() {

			// add your own special keys here:
			public final static int CodeDelete = -5; // Keyboard.KEYCODE_DELETE

			@Override
			public void onKey(int primaryCode, int[] keyCodes) {
				View focusCurrent = mHostActivity.getWindow().getCurrentFocus();
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
				} else if (primaryCode == Keyboard.KEYCODE_DONE) {
					// We need to send the DONE to the host activity to process the event
					mHostActivity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
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
		mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        // Hide the standard keyboard initially
        mHostActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    // Returns whether the CustomKeyboard is visible.
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }

    // Make the CustomKeyboard visible, and hide the system keyboard for view v.
    public void showCustomKeyboard( View v ) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if (v != null) {
			((InputMethodManager)mHostActivity.getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
		}
    }

    // Make the CustomKeyboard invisible.
    public void hideCustomKeyboard() {
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }

    public void registerEditText(int resid) {
        // Find the EditText 'resid'
        final EditText edittext= (EditText)mHostActivity.findViewById(resid);
        // Make the custom keyboard appear
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
        // Disable standard keyboard hard way
        // NOTE There is also an easy way: 'edittext.setInputType(InputType.TYPE_NULL)' (but you
        // will not have a cursor, and no 'edittext.setCursorVisible(true)' doesn't work )
        edittext.setOnTouchListener(new OnTouchListener() {
            @Override 
            public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;
                int inType = edittext.getInputType();       // Backup the input type
                edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                edittext.onTouchEvent(event);               // Call native handler
                edittext.setInputType(inType);              // Restore input type
                edittext.setCursorVisible(true);
                return true; // Consume touch event
            }
        });
        // Disable spell check (hex strings look like words to Android)
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