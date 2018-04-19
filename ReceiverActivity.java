package admin.encryption;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ReceiverActivity extends AppCompatActivity {

    private Animation animation;
    private ImageView scanner;
    private boolean available;
    private boolean isReceiving;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        initializeWidget();
    }

    private void initializeWidget() {

        animation = AnimationUtils.loadAnimation(this,R.anim.scanner_rotation);
        LinearInterpolator interpolator = new LinearInterpolator();
        animation.setInterpolator(interpolator);

        scanner = findViewById(R.id.scanner_image);

        AppCompatButton appCompatScan = findViewById(R.id.scanner_button);

        View.OnClickListener onClickListener = param->{
            if(!isReceiving){
                isReceiving = true;
                startToReceive();
                appCompatScan.setText("WAITING FOR SENDER");
            }
        };
        appCompatScan.setOnClickListener(onClickListener);

    }

    private void startToReceive(){

        isReceiving = false;
        available = true;
        animation.reset();
        scanner.startAnimation(animation);
        ReceiverHandler handler = new ReceiverHandler();
        Runnable runnable = new ReceiverRunnable(handler);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private class ReceiverHandler extends Handler {
        @Override
        public void handleMessage(Message input) {

            super.handleMessage(input);

            boolean isTimeOut = input.arg1 == BluetoothConfiguration.TIME_OUT_DETECT;

            if(isTimeOut && !available){

                Toast.makeText(ReceiverActivity.this,"Connection TimeOut !!!",Toast.LENGTH_LONG).show();
                AppCompatButton appCompatScan = findViewById(R.id.scanner_button);
                appCompatScan.setText("TIME OUT !!! PRESS TO RETRY");
                scanner.clearAnimation();
                return;
            }

            BluetoothSocket socket = (BluetoothSocket) input.obj;
            BluetoothDevice device = socket.getRemoteDevice();
            Toast.makeText(ReceiverActivity.this,"Connected to "+device.getName(),Toast.LENGTH_LONG).show();

            try {
                IOStream.IN_STREAM = new DataInputStream(socket.getInputStream());
                IOStream.OUT_STREAM = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            isReceiving = false;
            scanner.clearAnimation();

            byte [] bytes = new byte[13];
            String protocol = null;
            long size = 0;
            try {
                IOStream.IN_STREAM.read(bytes,0,13);
                protocol = new String(bytes,0,13);
                IOStream.IN_STREAM.read(bytes,0,8);
                size = Util.bytesToLong(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            BluetoothConfiguration.FILE_SIZE = size;

            Log.d("spice--size",protocol+size);

            if(protocol.equals("FILE_PROTOCOL")){
                Intent intent = new Intent(ReceiverActivity.this,FileReceiverActivity.class);
                startActivity(intent);
            }else {
                Intent intent = new Intent(ReceiverActivity.this,ChatActivity.class);
                startActivity(intent);
            }
        }
    }

    private class ReceiverRunnable implements Runnable {

        private Handler handler;
        public ReceiverRunnable(Handler handler){
            this.handler = handler;
        }

        @Override
        public void run() {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothServerSocket serverSocket = null;
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(BluetoothConfiguration.NAME, UUID.fromString(BluetoothConfiguration.UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(serverSocket == null){
                return;
            }
            while (available){
                Message output = handler.obtainMessage();
                try {
                    BluetoothSocket socket = serverSocket.accept(BluetoothConfiguration.TIME_OUT);

                    if(socket != null){
                        output.obj = socket;
                        output.arg1 = BluetoothConfiguration.TIME_OUT_NOT_DETECT;
                        handler.sendMessage(output);
                    }
                    available = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    available = false;
                    output.arg1 = BluetoothConfiguration.TIME_OUT_DETECT;
                    handler.sendMessage(output);
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
