package com.mobilsignandroid;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MobilSign extends Activity {

	private MobilSignClient aClient;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        aClient = new MobilSignClient();
        setContentView(R.layout.activity_mobil_sign);
        
        final Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	IntentIntegrator integrator = new IntentIntegrator(MobilSign.this);
                integrator.initiateScan();  
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
          if (scanResult != null) {
            // handle scan result
             String contantsString =  scanResult.getContents()==null?"0":scanResult.getContents();
             if (contantsString.equalsIgnoreCase("0")) {
                 Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();
             }else {
                 Toast.makeText(this, contantsString, Toast.LENGTH_LONG).show();
            }
          }
          else{
              Toast.makeText(this, "Problem to secan the barcode.", Toast.LENGTH_LONG).show();
          }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_mobil_sign, menu);
        return true;
    }
}

