package com.ps3test;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.content.Context;

/**
 * Preuve de concept - Affiche les inputs d'une manette PS3
 * Brancher la manette en USB OTG ou Bluetooth sur le telephone
 */
public class MainActivity extends Activity implements InputManager.InputDeviceListener {

    private GamepadView gamepadView;
    private String controllerName = "Aucune manette detectee";

    // Joysticks
    private float leftX = 0, leftY = 0;
    private float rightX = 0, rightY = 0;

    // Gachettes
    private float l2 = 0, r2 = 0;

    // Boutons
    private boolean btnX = false, btnO = false, btnTri = false, btnSq = false;
    private boolean btnL1 = false, btnR1 = false, btnL3 = false, btnR3 = false;
    private boolean btnStart = false, btnSelect = false, btnPS = false;
    private boolean dpadUp = false, dpadDown = false, dpadLeft = false, dpadRight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gamepadView = new GamepadView(this);
        setContentView(gamepadView);

        InputManager im = (InputManager) getSystemService(Context.INPUT_SERVICE);
        im.registerInputDeviceListener(this, null);

        // Detecter manette deja connectee
        detectController();
    }

    private void detectController() {
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev != null && (dev.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                controllerName = dev.getName();
                break;
            }
            if (dev != null && (dev.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                controllerName = dev.getName();
                break;
            }
        }
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        detectController();
        gamepadView.invalidate();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        controllerName = "Manette deconnectee!";
        gamepadView.invalidate();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        detectController();
    }

    private boolean isGamepad(InputDevice device) {
        if (device == null) return false;
        int src = device.getSources();
        return (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        InputDevice dev = event.getDevice();
        if (!isGamepad(dev)) return super.onGenericMotionEvent(event);

        leftX  = event.getAxisValue(MotionEvent.AXIS_X);
        leftY  = event.getAxisValue(MotionEvent.AXIS_Y);
        rightX = event.getAxisValue(MotionEvent.AXIS_Z);
        rightY = event.getAxisValue(MotionEvent.AXIS_RZ);

        l2 = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        r2 = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);

        // Si LTRIGGER/RTRIGGER ne marchent pas, essayer BRAKE/GAS
        if (l2 == 0) l2 = event.getAxisValue(MotionEvent.AXIS_BRAKE);
        if (r2 == 0) r2 = event.getAxisValue(MotionEvent.AXIS_GAS);

        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        dpadLeft  = hatX < -0.5f;
        dpadRight = hatX >  0.5f;
        dpadUp    = hatY < -0.5f;
        dpadDown  = hatY >  0.5f;

        gamepadView.invalidate();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputDevice dev = event.getDevice();
        if (!isGamepad(dev)) return super.onKeyDown(keyCode, event);

        setButton(keyCode, true);
        gamepadView.invalidate();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        InputDevice dev = event.getDevice();
        if (!isGamepad(dev)) return super.onKeyUp(keyCode, event);

        setButton(keyCode, false);
        gamepadView.invalidate();
        return true;
    }

    private void setButton(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:      btnX = pressed;      break; // Cross
            case KeyEvent.KEYCODE_BUTTON_B:      btnO = pressed;      break; // Circle
            case KeyEvent.KEYCODE_BUTTON_Y:      btnTri = pressed;    break; // Triangle
            case KeyEvent.KEYCODE_BUTTON_X:      btnSq = pressed;     break; // Square
            case KeyEvent.KEYCODE_BUTTON_L1:     btnL1 = pressed;     break;
            case KeyEvent.KEYCODE_BUTTON_R1:     btnR1 = pressed;     break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: btnL3 = pressed;     break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: btnR3 = pressed;     break;
            case KeyEvent.KEYCODE_BUTTON_START:  btnStart = pressed;  break;
            case KeyEvent.KEYCODE_BUTTON_SELECT: btnSelect = pressed; break;
            case KeyEvent.KEYCODE_BUTTON_MODE:   btnPS = pressed;     break;
            case KeyEvent.KEYCODE_DPAD_UP:       dpadUp = pressed;    break;
            case KeyEvent.KEYCODE_DPAD_DOWN:     dpadDown = pressed;  break;
            case KeyEvent.KEYCODE_DPAD_LEFT:     dpadLeft = pressed;  break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:    dpadRight = pressed; break;
        }
    }

    // ===================================================
    // Vue custom qui dessine l'etat de la manette
    // ===================================================
    class GamepadView extends View {
        private Paint paintBg, paintText, paintTitle, paintStick, paintStickBg;
        private Paint paintBtnOff, paintBtnOn, paintTrigger, paintDpad;

        public GamepadView(Context ctx) {
            super(ctx);
            setFocusable(true);

            paintBg = new Paint();
            paintBg.setColor(Color.rgb(30, 30, 30));

            paintTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintTitle.setColor(Color.rgb(0, 200, 100));
            paintTitle.setTextSize(48);
            paintTitle.setTextAlign(Paint.Align.CENTER);

            paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setColor(Color.WHITE);
            paintText.setTextSize(32);

            paintStickBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintStickBg.setColor(Color.rgb(60, 60, 60));
            paintStickBg.setStyle(Paint.Style.STROKE);
            paintStickBg.setStrokeWidth(3);

            paintStick = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintStick.setColor(Color.rgb(0, 200, 255));

            paintBtnOff = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintBtnOff.setColor(Color.rgb(80, 80, 80));

            paintBtnOn = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintBtnOn.setColor(Color.rgb(255, 50, 50));

            paintTrigger = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintTrigger.setColor(Color.rgb(255, 165, 0));

            paintDpad = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintDpad.setColor(Color.rgb(200, 200, 200));
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();

            // Fond
            c.drawRect(0, 0, w, h, paintBg);

            // Titre
            c.drawText("PS3 GAMEPAD TEST", w / 2f, 60, paintTitle);
            paintText.setTextSize(28);
            paintText.setColor(Color.rgb(180, 180, 180));
            paintText.setTextAlign(Paint.Align.CENTER);
            c.drawText(controllerName, w / 2f, 100, paintText);

            // ---- Joystick gauche ----
            float lCx = w * 0.2f, lCy = h * 0.5f, stickR = Math.min(w, h) * 0.15f;
            drawStick(c, lCx, lCy, stickR, leftX, leftY, "L");

            // ---- Joystick droit ----
            float rCx = w * 0.8f, rCy = h * 0.5f;
            drawStick(c, rCx, rCy, stickR, rightX, rightY, "R");

            // ---- Boutons ----
            float btnArea = w * 0.5f;
            float btnY = h * 0.35f;
            float btnR = 30;
            float btnSpacing = 75;

            paintText.setTextSize(22);
            paintText.setColor(Color.WHITE);
            paintText.setTextAlign(Paint.Align.CENTER);

            // Triangle, Circle, Cross, Square (disposition PlayStation)
            drawButton(c, btnArea, btnY - btnSpacing, btnR, btnTri, "\u25B3");      // Triangle haut
            drawButton(c, btnArea + btnSpacing, btnY, btnR, btnO, "O");              // Circle droite
            drawButton(c, btnArea, btnY + btnSpacing, btnR, btnX, "X");              // Cross bas
            drawButton(c, btnArea - btnSpacing, btnY, btnR, btnSq, "\u25A1");        // Square gauche

            // L1 R1
            drawButton(c, w * 0.15f, h * 0.15f, 35, btnL1, "L1");
            drawButton(c, w * 0.85f, h * 0.15f, 35, btnR1, "R1");

            // L3 R3
            drawButton(c, w * 0.2f, h * 0.82f, 20, btnL3, "L3");
            drawButton(c, w * 0.8f, h * 0.82f, 20, btnR3, "R3");

            // Start Select PS
            drawButton(c, w * 0.45f, h * 0.75f, 18, btnSelect, "SEL");
            drawButton(c, w * 0.55f, h * 0.75f, 18, btnStart, "STA");
            drawButton(c, w * 0.5f, h * 0.85f, 22, btnPS, "PS");

            // ---- Gachettes L2 R2 ----
            drawTrigger(c, w * 0.08f, h * 0.15f, 60, 20, l2, "L2");
            drawTrigger(c, w * 0.85f, h * 0.15f, 60, 20, r2, "R2");

            // ---- D-Pad ----
            float dCx = w * 0.35f, dCy = h * 0.55f;
            float dS = 25;
            drawDpadBtn(c, dCx, dCy - dS * 2, dS, dpadUp);    // Haut
            drawDpadBtn(c, dCx, dCy + dS * 2, dS, dpadDown);  // Bas
            drawDpadBtn(c, dCx - dS * 2, dCy, dS, dpadLeft);  // Gauche
            drawDpadBtn(c, dCx + dS * 2, dCy, dS, dpadRight); // Droite

            // ---- Valeurs numeriques ----
            paintText.setTextSize(24);
            paintText.setColor(Color.rgb(150, 150, 150));
            paintText.setTextAlign(Paint.Align.LEFT);
            c.drawText(String.format("LX:%.2f LY:%.2f", leftX, leftY), 20, h - 60, paintText);
            c.drawText(String.format("RX:%.2f RY:%.2f", rightX, rightY), 20, h - 25, paintText);
            paintText.setTextAlign(Paint.Align.RIGHT);
            c.drawText(String.format("L2:%.2f R2:%.2f", l2, r2), w - 20, h - 25, paintText);
        }

        private void drawStick(Canvas c, float cx, float cy, float radius, float sx, float sy, String label) {
            // Cercle de fond
            c.drawCircle(cx, cy, radius, paintStickBg);
            // Croix centrale
            c.drawLine(cx - radius, cy, cx + radius, cy, paintStickBg);
            c.drawLine(cx, cy - radius, cx, cy + radius, paintStickBg);
            // Point du stick
            float dotX = cx + sx * radius;
            float dotY = cy + sy * radius;
            paintStick.setColor(Color.rgb(0, 200, 255));
            c.drawCircle(dotX, dotY, 18, paintStick);
            // Label
            paintText.setTextSize(20);
            paintText.setColor(Color.rgb(120, 120, 120));
            paintText.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, cx, cy + radius + 30, paintText);
        }

        private void drawButton(Canvas c, float cx, float cy, float r, boolean pressed, String label) {
            c.drawCircle(cx, cy, r, pressed ? paintBtnOn : paintBtnOff);
            paintText.setTextSize(20);
            paintText.setColor(Color.WHITE);
            paintText.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, cx, cy + 7, paintText);
        }

        private void drawTrigger(Canvas c, float x, float y, float w, float h, float value, String label) {
            // Fond
            RectF bg = new RectF(x, y, x + w, y + h);
            c.drawRoundRect(bg, 5, 5, paintBtnOff);
            // Remplissage
            RectF fill = new RectF(x, y, x + w * value, y + h);
            c.drawRoundRect(fill, 5, 5, paintTrigger);
            // Label
            paintText.setTextSize(18);
            paintText.setColor(Color.WHITE);
            paintText.setTextAlign(Paint.Align.CENTER);
            c.drawText(label, x + w / 2, y - 8, paintText);
        }

        private void drawDpadBtn(Canvas c, float cx, float cy, float size, boolean pressed) {
            RectF r = new RectF(cx - size, cy - size, cx + size, cy + size);
            c.drawRoundRect(r, 4, 4, pressed ? paintBtnOn : paintBtnOff);
        }
    }
}
