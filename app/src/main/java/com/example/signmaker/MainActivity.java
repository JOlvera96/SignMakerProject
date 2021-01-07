package com.example.signmaker;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private ToggleButton toggleButton;

    //1)
    Button IdDesconectar;
    TextView IdBufferIn, IdBufferIn2, IdBufferIn3;
    //ArcProgress arc_progress;
    private ToggleButton btn_crono;
    Chronometer crono;
    long elapsedTime = 0;
    String currentTime = "";
    int tiempo = 0;
    //-------------------------------------------
    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;
    // Identificador unico de servicio - SPP UUID
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String para la direccion MAC
    private static String address = null;
    private static String pesoMA = null;
    private static double calorias;
    private static int bpm;
    //-------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    DataStringIN.append(readMessage);

                    int endOfLineIndex = DataStringIN.indexOf(";");

                    if (endOfLineIndex > 0) {

                        final String[] myArray = DataStringIN.toString().split(" ");
                        IdBufferIn.setText(myArray[0]+ "\nRPM");

                        int vel = Integer.parseInt(myArray[2]);
                        //arc_progress.setProgress(vel);

                        IdBufferIn3.setText(myArray[1]+ "\nBPM");
                        bpm = Integer.parseInt(myArray[1]);

                        /*if (bpm>200){
                            AlertDialog.Builder builder = new AlertDialog.Builder(UserInterfaz.this);
                            builder.setTitle("Importante");
                            builder.setMessage("Estas realizando demasiado esfuerzo fisico, deberias tener cuidado pues podria causarte un infarto");
                            builder.setPositiveButton("OK",null);
                            builder.create();
                            builder.show();
                        }*/

                        btn_crono.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                                if (isChecked){
                                    crono.setBase(SystemClock.elapsedRealtime());
                                    crono.start();
                                } else {
                                    crono.stop();
                                }
                            }
                        });
                        long minutes = ((SystemClock.elapsedRealtime() - crono.getBase()) / 1000) / 60;
                        String min = Long.toString(minutes);
                        tiempo = Integer.parseInt(min);
                        elapsedTime = SystemClock.elapsedRealtime();

                        int peso = Integer.parseInt(pesoMA);
                        if (vel <= 16){
                            calorias = (0.049*(peso*2.2)*tiempo);
                        }else{
                            calorias = (0.071*(peso*2.2)*tiempo);
                        }
                        int cal = (int) calorias;
                        IdBufferIn2.setText(cal+ "\nCalorias");

                        String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                        //IdBufferIn.setText("Dato: " + dataInPrint);//<-<- PARTE A MODIFICAR >->->
                        DataStringIN.delete(0, DataStringIN.length());

                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        VerificarEstadoBT();

        // Configuracion onClick listeners para los botones
        // para indicar que se realizara cuando se detecte
        // el evento de Click
        /*IdSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                String dato = IdText1.getText().toString();
                MyConexionBT.write(dato);
            }
        });*/

        /*IdApagar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MyConexionBT.write("0");
            }
        });*/

//        IdDesconectar.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (btSocket!=null)
//                {
//                    try {btSocket.close();}
//                    catch (IOException e)
//                    { Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();;}
//                }
//                finish();
//            }
//        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Consigue la direccion MAC desde DeviceListActivity via intent
        Bundle intent = getIntent().getExtras();
        //Consigue la direccion MAC desde DeviceListActivity via EXTRA
        address = intent.getString(DispositivosBT.EXTRA_DEVICE_ADDRESS);//<-<- PARTE A MODIFICAR >->->
        pesoMA = intent.getString(DispositivosBT.PESO);
        //Setea la direccion MAC
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try
        {
            btSocket.connect();
        }
        catch (IOException e) {
            try
            {
                btSocket.close();
            }
            catch (IOException e2) {}
        }

        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        { // Cuando se sale de la aplicación esta parte permite
            // que no se deje abierto el socket
            btSocket.close();
        } catch (IOException e2) {}
    }

    //Comprueba que el dispositivo Bluetooth Bluetooth está disponible y solicita que se active si está desactivado
    private void VerificarEstadoBT() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}