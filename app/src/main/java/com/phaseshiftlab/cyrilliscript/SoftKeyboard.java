/*
 * Copyright (C) 2008-2009 The Android Open Source Project
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

package com.phaseshiftlab.cyrilliscript;

import android.app.Dialog;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.facebook.stetho.Stetho;
import com.phaseshiftlab.cyrilliscript.events.RxBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = true;

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private InputMethodManager mInputMethodManager;

    private LatinKeyboardView mInputView;
    private MainWritingView mDrawingInputView;

    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard_en;
    private LatinKeyboard mQwertyKeyboard_es;
    private LatinKeyboard mCurKeyboard;

    public EditorInfo eInfo;

    private static final int SHIFT_STATE_INITIAL = 1;
    private static final int SHIFT_STATE_INTERMEDIATE = 2;
    private static final int SHIFT_STATE_FINAL = 3;
    private int shiftState;

    private String mWordSeparators;

    private final String TAG = SoftKeyboard.class.getSimpleName();
    private MainView mMainView;

    private RxBus rxBus = RxBus.getInstance();

    //region Initialization methods

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override
    public void onCreate() {
        getAssets();
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
    }

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        if ((mQwertyKeyboard_en != null) && (mQwertyKeyboard_es != null)) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }

        // Inflates the XMLs of the Keyboards
        mQwertyKeyboard_es = new LatinKeyboard(this, R.xml.qwerty_es);
        mQwertyKeyboard_en = new LatinKeyboard(this, R.xml.qwerty_en);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);

        subscribeToTopics();

    }

    private void subscribeToTopics() {
        rxBus.receive(Map.class, s -> {
            String recognized = (String) s.get("RECOGNIZED");
            Log.d("Cyrilliscript", "RECOGNIZED received " + recognized);
            if(recognized != null) {
                mComposing.append(recognized);
                updateCandidates();
            }
        });
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);

        mMainView = (MainView) getLayoutInflater().inflate(R.layout.main_view, null);
        mDrawingInputView = (MainWritingView) mMainView.findViewById(R.id.drawing_input_view);
        setCurrentQwerty();
        setLatinKeyboard(mCurKeyboard);


        //return mInputView;
        return mMainView;
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);

        // Apply the selected keyboard to the input view.
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        switch (subtype.getLocale()) {
            case "es_ES":
                mQwertyKeyboard_es = new LatinKeyboard(this, R.xml.qwerty_es);
                break;
            case "en_US":
                mQwertyKeyboard_en = new LatinKeyboard(this, R.xml.qwerty_en);
                break;
            case "en_UK":
                mQwertyKeyboard_en = new LatinKeyboard(this, R.xml.qwerty_en);
                break;
            default:
        }
        setLatinKeyboard(mCurKeyboard);

        // Hides the Keyboard when Services Starts
        mInputView.closing();
    }


    /**
     * Sets the Prediction and Shift & Candidates based on the InputType
     * Switch between Keyboards Symbols & Qwerty.
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;

        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;


        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).

                setCurrentQwerty();
                mPredictionOn = true;

                // Log Editorinfo Flags
                final String flagsString = toFlagsString(attribute.inputType & InputType.TYPE_MASK_FLAGS);
                Log.i(TAG, "Flags: " + flagsString);


                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;

                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
//                updateShiftKeyState(attribute);
                if (getCurrentInputConnection().getCursorCapsMode(attribute.inputType) != 0) {
                    handleShift();
                }
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                setCurrentQwerty();
//                updateShiftKeyState(attribute);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && ic.getCursorCapsMode(attribute.inputType) != 0) {
                    handleShift();
                }
        }

        // Sets the right Enter key (Go, Next, Search, Enter)
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        eInfo = attribute;
    }


    private static String toFlagsString(final int flags) {
        final ArrayList<String> flagsArray = new ArrayList<>();
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
            flagsArray.add("TYPE_TEXT_FLAG_NO_SUGGESTIONS");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_MULTI_LINE))
            flagsArray.add("TYPE_TEXT_FLAG_MULTI_LINE");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE))
            flagsArray.add("TYPE_TEXT_FLAG_IME_MULTI_LINE");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_CAP_WORDS))
            flagsArray.add("TYPE_TEXT_FLAG_CAP_WORDS");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES))
            flagsArray.add("TYPE_TEXT_FLAG_CAP_SENTENCES");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS))
            flagsArray.add("TYPE_TEXT_FLAG_CAP_CHARACTERS");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
            flagsArray.add("TYPE_TEXT_FLAG_AUTO_CORRECT");
        if (0 != (flags & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE))
            flagsArray.add("TYPE_TEXT_FLAG_AUTO_COMPLETE");
        return flagsArray.isEmpty() ? "" : Arrays.toString(flagsArray.toArray());
    }


    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);

        setCandidatesViewShown(true);

        return mCandidateView;


    }

    //endregion

    //region Finish Input

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();


        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        setCurrentQwerty();
        if (mInputView != null) {
            mInputView.closing();
        }
        System.gc();

        shiftState = SHIFT_STATE_INITIAL;
    }
    //endregion

    //region Language Switch

    // Loads the right keyboard on mCurKeyboard based on Locate
    private void setCurrentQwerty() {
        if ((mQwertyKeyboard_en != null) && (mQwertyKeyboard_es != null)) {
            InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
            if (subtype == null) {
                subtype = mInputMethodManager.getLastInputMethodSubtype();
            }

            switch (subtype.getLocale()) {
                case "es_ES":
                    mCurKeyboard = mQwertyKeyboard_es;
                    break;
                case "en_US":
                    mCurKeyboard = mQwertyKeyboard_en;
                    break;
                case "en_UK":
                    mCurKeyboard = mQwertyKeyboard_en;
                    break;
                default:
            }
        }
    }

    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
        final boolean shouldSupportLanguageSwitchKey =
                mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());

        //Shows the Globe icon if there're more Languages
        nextKeyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
        mInputView.setKeyboard(nextKeyboard);

        //Sets the Flag in the Space Bar
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    //Language Switch button is pressed
    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
        setCurrentQwerty();     // Sets the mCurKeyboard based on the Subtype.Locale (es_ES, en_US, en_UK)
        setLatinKeyboard(mCurKeyboard);

        // Sets the proper Enter key (Go, Next, etc)
        mCurKeyboard.setImeOptions(getResources(), eInfo.imeOptions);
    }

    // Event when the Language Change
    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.closing();
        switch (subtype.getLocale()) {
            case "es_ES":
                mCurKeyboard = mQwertyKeyboard_es;
                break;
            case "en_US":
                mCurKeyboard = mQwertyKeyboard_en;
                break;
            case "en_UK":
                mCurKeyboard = mQwertyKeyboard_en;
                break;
            default:
        }
        setLatinKeyboard(mCurKeyboard);
    }
    //endregion

    //region Intercepts KEY Down & Up events (Which raises before OnKey)

    /**
     * Intecepts: Back, Delete, Enter, and Hardkeys (External Keyboard)
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState() & KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }

                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        // Go on with the rest of the keys
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    //endregion

    //region Send Keystrokes

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
//        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }
//endregion

    //region Shift Keys

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
//    private void updateShiftKeyState(EditorInfo attr) {
//        if (attr != null
//                && mInputView != null
//                && mCurKeyboard == mInputView.getKeyboard())
//        {
//            int caps = 0;
//            EditorInfo ei = getCurrentInputEditorInfo();
//            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
//                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
//            }
//            mInputView.setShifted(mCapsLock || caps != 0);
//        }
//    }

    // Double tap on Shiftkey in less than 500ms is considered CapLocks
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 500 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mCurKeyboard == currentKeyboard) {

            if (shiftState == SHIFT_STATE_INITIAL) {
                mCurKeyboard.setShifted(true);
                shiftState = SHIFT_STATE_INTERMEDIATE;
                ((LatinKeyboard) mInputView.getKeyboard()).setShiftIconToSticky(true);  //Green Arrow
                mCurKeyboard.mShiftKey.on = false;                                        // LED Off
            } else if (shiftState == SHIFT_STATE_INTERMEDIATE) {
                mCurKeyboard.setShifted(true);
                shiftState = SHIFT_STATE_FINAL;
                ((LatinKeyboard) mInputView.getKeyboard()).setShiftIconToSticky(true);  //Green Arrow
                mCurKeyboard.mShiftKey.on = true;                                         // LED On
            } else if (shiftState == SHIFT_STATE_FINAL) {
                mCurKeyboard.setShifted(false);
                shiftState = SHIFT_STATE_INITIAL;
                ((LatinKeyboard) mInputView.getKeyboard()).setShiftIconToSticky(false); // Black Arrow
                mCurKeyboard.mShiftKey.on = false;                                        // LED Off
            }
        } else if (currentKeyboard == mSymbolsKeyboard) {
            setLatinKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            setLatinKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }


    }
//endregion

    //region When a Key is pressed
    // Implementation of KeyboardViewListener
    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
//            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            shiftState = SHIFT_STATE_FINAL;
            handleShift();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                shiftState = SHIFT_STATE_FINAL;
                setCurrentQwerty();
                setLatinKeyboard(mCurKeyboard);
                handleShift();
            } else {
                ((LatinKeyboard) mInputView.getKeyboard()).setShiftIconToSticky(false);
                setLatinKeyboard(mSymbolsShiftedKeyboard);
                handleShift();
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }

        if (shiftState == SHIFT_STATE_INTERMEDIATE && primaryCode != Keyboard.KEYCODE_SHIFT && primaryCode != Keyboard.KEYCODE_MODE_CHANGE) {
            shiftState = SHIFT_STATE_FINAL;
            handleShift();
        }
    }

    public void onPress(int primaryCode) {
    }

    public void onRelease(int primaryCode) {
    }
    //endregion

    //region Handle Characters (Coming from OnKey)

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
//            updateShiftKeyState(getCurrentInputEditorInfo());
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
    }

    //endregion

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
//        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    //region Word Separators

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char) code));
    }

    //endregion

    //region Candidates (Suggestions)

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
//            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    //endregion

    //region Swipe Methods
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    //endregion


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String sUUID = intent.getStringExtra("UUID");

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {

            ic.beginBatchEdit();
            if (mComposing.length() > 0) {
                commitTyped(ic);
            }
            ic.commitText("Tag leido: " + sUUID, 0);
            ic.endBatchEdit();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void clearDrawingCanvas(View view) {
        Log.d("Cyrilliscript", "Clear Drawing called");
        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("CLEAR", "true");
        rxBus.post(eventMap);
    }

    public void deleteLastPath(View view) {
        Log.d("Cyrilliscript", "Clear Drawing called");
        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("DELETE_LAST_PATH", "true");
        rxBus.post(eventMap);
    }

    public void restoreLastPath(View view) {
        Log.d("Cyrilliscript", "Clear Drawing called");
        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("RESTORE_LAST_PATH", "true");
        rxBus.post(eventMap);
    }

    public void sendBackspace(View view) {
        Log.d("Cyrilliscript", "backspace called");
        handleBackspace();
    }

    public void sendEnter(View view) {
        Log.d("Cyrilliscript", "enter called");
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
    }
}
