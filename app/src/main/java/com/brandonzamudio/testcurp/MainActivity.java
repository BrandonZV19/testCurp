package com.brandonzamudio.testcurp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

public class MainActivity extends AppCompatActivity {
    Button consultar;
    EditText inCurp;
    AlertDialog inIncorrect, dialogDatos,dialogError;
    ProgressDialog progressDialog;

    boolean resumed=false;
    
    static final String url = "https://www.bancomermovil.net:11443/dembmv_mx_web/mbmv_mult_web_mbmv_01/services/digitalAccount/V02/consultaCURP";

    static final String LOG_TAG = "MainActivity";

    LayoutInflater inflater;
    View vistaDatos;
    TextView vPat,vMat,vNombres,vSexo,vNacimiento,vFecha,vNacionalidad,vStatus,vEmisora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Cargando...");
        
        consultar = (Button) findViewById(R.id.bConsult);
        inCurp = (EditText) findViewById(R.id.eCurp);

        inflater = MainActivity.this.getLayoutInflater();
        
        AlertDialog.Builder iiBUilder=new AlertDialog.Builder(MainActivity.this)
                .setTitle("Ups")
                .setMessage("La CURP debe ser de 18 caracteres")
                .setCancelable(false)
                .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        inIncorrect=iiBUilder.create();

        consultar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(LOG_TAG,"Consultar "+inCurp.getText().toString().trim().length());
                if (inCurp.getText().toString().trim().length()!=18){
                    if (resumed) {
                        inIncorrect.show();
                    }
                }else {
                    consultarAction(inCurp.getText().toString().trim());
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inIncorrect.isShowing()){
            inIncorrect.dismiss();
        }
        if (dialogDatos!=null) {
            if (dialogDatos.isShowing()) {
                dialogDatos.dismiss();
            }
        }
        if (dialogError!=null) {
            if (dialogError.isShowing()) {
                dialogError.dismiss();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed=true;
    }

    private void consultarAction(String CURP) {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = null;

        if (MainActivity.this.getParent() != null) {
            progressDialog = new ProgressDialog(MainActivity.this.getParent());
        } else {
            progressDialog = new ProgressDialog(MainActivity.this);
        }

        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Cargando...");

        if (!MainActivity.this.isFinishing()) {
            progressDialog.show();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("aMaterno", "");
            jsonParams.put("nombres", "");
            jsonParams.put("indRENAPO", "CURP");
            jsonParams.put("sexo", "");
            jsonParams.put("curp", CURP);
            jsonParams.put("aPaterno", "");
            jsonParams.put("cveEntidadNac", "");
            jsonParams.put("fechNac", "");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Exception1: " + e.toString());
        }

        String jString = jsonParams.toString();
        Log.e(LOG_TAG, "jString: " + jString);

        ByteArrayEntity entity = null;
        try {
            entity = new ByteArrayEntity(jsonParams.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Exception2: " + e.toString());
        }
        if (entity!=null) {
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        }

        client.post(getApplicationContext(), url, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject obj) {
                String respp= String.valueOf(obj);
                Log.e(LOG_TAG,"reponse: "+respp);
                if (progressDialog!=null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                AlertDialog.Builder iiBUilder=new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Ups")
                        .setMessage("Error desconocido")
                        .setCancelable(false)
                        .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });

                dialogError=iiBUilder.create();

                try {
                    if (!obj.isNull("status")){
                        JSONObject status=obj.getJSONObject("status");
                        if (!status.isNull("code")){
                            int code=status.getInt("code");
                            if (code==200){
                                mostrarInfo(obj.getJSONObject("response"));
                            }else {
                                String descrip=status.getString("description");
                                dialogError.setMessage(descrip);
                                dialogError.show();
                            }
                        }else {
                            String descrip=status.getString("description");
                            dialogError.setMessage(descrip);
                            dialogError.show();
                        }
                    }else {
                        dialogError.show();
                    }

                } catch (JSONException e) {
                    Log.e(LOG_TAG,"Exception 3"+e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (progressDialog!=null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this,"Error de conexión "+statusCode+".",Toast.LENGTH_SHORT).show();
            }
        });

    }

    @SuppressLint("InflateParams")
    private void mostrarInfo(JSONObject json){
        Log.e(LOG_TAG,"mostrarInfo: "+json.toString());

        if (vistaDatos != null) {
            vistaDatos = null;
        }

        vistaDatos = inflater.inflate(R.layout.vista_datos, null);
        vPat=vistaDatos.findViewById(R.id.apPat);
        vMat=vistaDatos.findViewById(R.id.apMat);
        vNombres=vistaDatos.findViewById(R.id.nombres);
        vSexo=vistaDatos.findViewById(R.id.sexo);
        vNacimiento=vistaDatos.findViewById(R.id.entidadNac);
        vFecha=vistaDatos.findViewById(R.id.fechaNac);
        vNacionalidad=vistaDatos.findViewById(R.id.nacionalidad);
        vStatus=vistaDatos.findViewById(R.id.status);
        vEmisora=vistaDatos.findViewById(R.id.entidadEmi);

        try {
            vPat.setText(json.getString("aPaterno"));
            vMat.setText(json.getString("aMaterno"));
            vNombres.setText(json.getString("nombres"));
            vSexo.setText(json.getString("sexo"));
            vNacimiento.setText(json.getString("cveEntidadNac"));
            vFecha.setText(json.getString("fechNac"));
            vNacionalidad.setText(json.getString("nacionalidad"));
            vStatus.setText(json.getString("curpStatus"));
            vEmisora.setText(json.getString("cveEntidadEmisora"));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(LOG_TAG,"Exception 4"+e.toString());
        }

        if (dialogDatos != null) {
            if (dialogDatos.isShowing()) {
                dialogDatos.dismiss();
            }
            dialogDatos = null;
        }

        AlertDialog.Builder builderL = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Resultado:")
                .setNegativeButton("Cerrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        if (vistaDatos.getParent() == null) {
            dialogDatos = builderL.create();
            dialogDatos.setView(vistaDatos);
            dialogDatos.show();
        } else {
            vistaDatos = null;

            vistaDatos = inflater.inflate(R.layout.vista_datos, null);
            vPat=vistaDatos.findViewById(R.id.apPat);
            vMat=vistaDatos.findViewById(R.id.apMat);
            vNombres=vistaDatos.findViewById(R.id.nombres);
            vSexo=vistaDatos.findViewById(R.id.sexo);
            vNacimiento=vistaDatos.findViewById(R.id.entidadNac);
            vFecha=vistaDatos.findViewById(R.id.fechaNac);
            vNacionalidad=vistaDatos.findViewById(R.id.nacionalidad);
            vStatus=vistaDatos.findViewById(R.id.status);
            vEmisora=vistaDatos.findViewById(R.id.entidadEmi);

            try {
                         vPat.setText(json.getString("aPaterno"));
                         vMat.setText(json.getString("aMaterno"));
                     vNombres.setText(json.getString("nombres"));
                        vSexo.setText(json.getString("sexo"));
                  vNacimiento.setText(json.getString("cveEntidadNac"));
                       vFecha.setText(json.getString("fechNac"));
                vNacionalidad.setText(json.getString("nacionalidad"));
                      vStatus.setText(json.getString("curpStatus"));
                     vEmisora.setText(json.getString("cveEntidadEmisora"));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(LOG_TAG,"Exception 4"+e.toString());
            }

            dialogDatos = builderL.create();
            dialogDatos.setView(vistaDatos);
            dialogDatos.show();
        }
        
    }

}
