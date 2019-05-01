package com.example.raspviewerwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.prefs.Preferences;

public class ConfigActivity extends AppCompatActivity
{
    static final String site ="http://www.mrk-bsuir.by/ru";
    int widgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    Intent resultValue;

    String P = "assad";
    static public String WIDGET_PREF = "wigdet_pref";

    boolean firstLaunch = true;

    static Bitmap bitmapImage;

    boolean seted = false;
    File rasp = null;

    int pageCount;
    int circleId = -1;

    int r = 50;
    int dR = 5;

    float x1 = 50, y1 = 50, x2 = 200, y2 = 200, xs, ys;

    EditText txt;
    Button btn;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            widgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if(widgetID == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish();

        resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);

        setResult(RESULT_CANCELED, resultValue);

        setContentView(R.layout.activity_config);
        image = findViewById(R.id.imageView2);
        btn = findViewById(R.id.button2);
        txt = findViewById(R.id.editText);
    }

    public void updateRect()
    {
        runOnUiThread(() -> {
            Bitmap fin = Bitmap.createBitmap(bitmapImage);
            Canvas canvas = new Canvas(fin);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setARGB(60, 0, 0, 0);
            canvas.drawRect(0, 0, fin.getWidth(), y1, p);
            canvas.drawRect(0, 0, x1, bitmapImage.getHeight(), p);
            canvas.drawRect(x2, 0, fin.getWidth(), bitmapImage.getHeight(), p);
            canvas.drawRect(0, y2, fin.getWidth(), bitmapImage.getHeight(), p);
            canvas.drawRect(0, y1, x1, y2, p);
            canvas.drawRect(x1, 0, x2, y1, p);
            canvas.drawRect(x2, y1, bitmapImage.getWidth(), y2, p);
            canvas.drawRect(x1, y2, x2, bitmapImage.getHeight(), p);
            p.setARGB(255, 0,191,255);
            canvas.drawCircle(x1, (y1 + y2) / 2, r + dR, p);
            canvas.drawCircle(x2, (y1 + y2) / 2, r + dR, p);
            canvas.drawCircle((x1 + x2) / 2, y1, r + dR, p);
            canvas.drawCircle((x1 + x2) / 2, y2, r + dR, p);
            p.setARGB(255, 255,255,255);
            canvas.drawCircle(x1, (y1 + y2) / 2, r, p);
            canvas.drawCircle(x2, (y1 + y2) / 2, r, p);
            canvas.drawCircle((x1 + x2) / 2, y1, r, p);
            canvas.drawCircle((x1 + x2) / 2, y2, r, p);

            image.setImageBitmap(fin);
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!seted)
            return true;
        int[] loc = new int[2];
        image.getLocationOnScreen(loc);
        float x = event.getX() - loc[0];
        float y = event.getY() - loc[1];
        x = x * bitmapImage.getWidth() / image.getWidth();
        y = y * bitmapImage.getHeight() / image.getHeight();


        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if(Math.sqrt((x - x1)*(x - x1) + (y - ((y1 + y2) / 2)) * (y - ((y1 + y2) / 2))) < (r + dR))
            {
                circleId = 1;
            }
            else if(Math.sqrt((x - x2)*(x - x2) + (y - ((y1 + y2) / 2)) * (y - ((y1 + y2) / 2))) < (r + dR))
            {
                circleId = 3;
            }
            else if(Math.sqrt((y - y1)*(y - y1) + (x - ((x1 + x2) / 2)) * (x - ((x1 + x2) / 2))) < (r + dR))
            {
                circleId = 2;
            }
            else if(Math.sqrt((y - y2)*(y - y2) + (x - ((x1 + x2) / 2)) * (x - ((x1 + x2) / 2))) < (r + dR))
            {
                circleId = 4;
            }

            xs = x;
            ys = y;
            Log.d(P, Float.toString(x));
            Log.d(P, Float.toString(y));

            Log.d(P, Integer.toString(circleId));
            return true;
        }
        else if(event.getAction() == MotionEvent.ACTION_UP)
        {
            circleId = -1;
            return true;
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE)
        {

            switch (circleId)
            {
                case -1:
                    x1 -= xs - x;
                    x2 -= xs - x;
                    y1 -= ys - y;
                    y2 -= ys - y;
                    ys = y;
                    xs = x;
                    break;
                case 1:
                    x1 = x;
                    break;
                case 2:
                    y1 = y;
                    break;
                case 3:
                    x2 = x;
                    break;
                case 4:
                    y2 = y;
                    break;
            }
            if (x1 > x2)
            {
                float temp = x1;
                x1 = x2;
                x2 = temp;
            }
            if (y1 > y2)
            {
                float temp = y1;
                y1 = y2;
                y2 = temp;
            }

            if(x1 < 0)
                x1 = 0;
            if(y1 < 0)
                y1 = 0;
            if(x1 > bitmapImage.getWidth())
                x1 = bitmapImage.getWidth();
            if(y1 > bitmapImage.getHeight())
                y1 = bitmapImage.getHeight();

            if(x2 < 0)
                x2 = 0;
            if(y2 < 0)
                y2 = 0;
            if(x2 > bitmapImage.getWidth())
                x2 = bitmapImage.getWidth();
            if(y > bitmapImage.getHeight())
                y2 = bitmapImage.getHeight();


            updateRect();
        }
        return true;
    }

    public void cropButton(View v)
    {
        class Task extends AsyncTask<Void, Void, Void>
        {
            @Override
            protected Void doInBackground(Void... voids)
            {
                try
                {
                    if(firstLaunch)
                    {
                        Log.d("wigdet_pref", "first");
                        firstLaunch = false;
                        rasp = File.createTempFile("pref", "post");
                        String pdfUrlNew = "";
                        URL siteUrl = new URL(site);
                        try (BufferedReader bf1 = new BufferedReader(new InputStreamReader(siteUrl.openStream()))) {
                            StringBuilder sb1 = new StringBuilder();
                            String line1 = null;
                            while ((line1 = bf1.readLine()) != null) {
                                sb1.append(line1);
                            }
                            int pos1 = sb1.indexOf("Объявления");
                            int end1 = sb1.indexOf(".pdf", pos1);
                            pdfUrlNew = sb1.substring(pos1 + 76, end1 + 4);
                        }
                        URL file = new URL(pdfUrlNew);

                        try (InputStream is = file.openStream()) {
                            int len;
                            byte[] buf = new byte[2048 * 2];
                            try (FileOutputStream fos = new FileOutputStream(rasp)) {
                                while ((len = is.read(buf)) != -1) {
                                    fos.write(buf, 0, len);
                                }
                            }
                        }
                    }

                    Log.d("wigdet_pref", "Render start");
                    try (PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(rasp, ParcelFileDescriptor.MODE_READ_ONLY)))
                    {
                        pageCount = renderer.getPageCount();
                        int pageI = Integer.parseInt("0" + txt.getText().toString()) - 1;
                        if(pageI > pageCount || pageI <= 0)
                        {
                            runOnUiThread(() -> Toast.makeText(ConfigActivity.this, "Такой страницы НЯМА", Toast.LENGTH_LONG).show());
                            return null;
                        }
                        PdfRenderer.Page page = renderer.openPage(pageI);
                        bitmapImage = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                        page.render(bitmapImage, new Rect(0, 0, page.getWidth() * 2, page.getHeight() * 2), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();
                        Log.d("wigdet_pref", "Render Finish");
                    }
                    File tmpFile = File.createTempFile("image", ".png");
                    SharedPreferences sp = getSharedPreferences(WIDGET_PREF, MODE_PRIVATE);
                    sp.edit().putString("FileNameImage", tmpFile.getPath()).apply();
                    String path = tmpFile.getPath();
                    try(FileOutputStream fos = new FileOutputStream(tmpFile))
                    {
                        bitmapImage.compress(Bitmap.CompressFormat.PNG, 0, fos);
                    }
                    seted = true;
                    runOnUiThread(() -> btn.setVisibility(View.VISIBLE));
                    updateRect();

                }
                catch (Exception e)
                {
                    runOnUiThread(() -> Toast.makeText(ConfigActivity.this, "Ой-ей. Что-то пошло не так...", Toast.LENGTH_LONG).show());
                    Log.i("ggp", "error\n", e);
                }
                return null;
            }
        }
        Toast.makeText(ConfigActivity.this, "Загружаем...\n(Может занять до минуты. Будьте терпиливы)", Toast.LENGTH_LONG).show();
        new Task().execute();
    }

    public void finalAction(View v)
    {
        int pageI = Integer.parseInt(txt.getText().toString()) - 1;
        if(pageI > pageCount || pageI < 0)
        {
            Toast.makeText(ConfigActivity.this, "Такой страницы НЯМА", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("x1" + widgetID, (int)x1);
        editor.putInt("x2" + widgetID, (int)x2);
        editor.putInt("y1" + widgetID, (int)y1);
        editor.putInt("y2" + widgetID, (int)y2);
        editor.putInt("page" + widgetID, pageI);
        editor.apply();
        setResult(RESULT_OK, resultValue);
        Intent intentUpdate = new Intent(this, RaspWidget.class);
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this, RaspWidget.class));
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intentUpdate);
        finish();
    }
}