package com.mobilsignandroid;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MobilSign extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobil_sign);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_mobil_sign, menu);
        return true;
    }
}
