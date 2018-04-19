package admin.encryption;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileSenderActivity extends AppCompatActivity {

    private static final int ACTION_ADD = 0x02;
    private static final int ACTION_UPDATE = 0x04;
    private static final int ACTION_FINISH = 0x08;
    private static final int ACTION_ERROR = 0x10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_sender);

        initializeWidget();
        initialize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            IOStream.IN_STREAM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            IOStream.OUT_STREAM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeWidget() {

        FileAdapter fileAdapter = new FileAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);

        RecyclerView list = findViewById(R.id.recycler_view);
        list.setAdapter(fileAdapter);
        list.setLayoutManager(layoutManager);
    }

    private void initialize() {

        Handler handler = new FileHandler();
        FileRunnable runnable = new FileRunnable(handler);
        Thread thread = new Thread(runnable);
        thread.start();
    }


    class FileHandler extends Handler {
        @Override
        public void handleMessage(Message input) {
            super.handleMessage(input);
            RecyclerView list = findViewById(R.id.recycler_view);
            FileAdapter fileAdapter = (FileAdapter) list.getAdapter();
            switch (input.arg1){
                case ACTION_ADD:
                    fileAdapter.addChild(input.obj);
                    //fileAdapter.finishProgress(input.arg2, FileAdapter.Status.RECEIVING);
                    break;
                case ACTION_UPDATE:
                    fileAdapter.updateProgress(input.arg2, (Long) input.obj);
                    break;
                case ACTION_FINISH:
                    fileAdapter.finishProgress(input.arg2, FileAdapter.Status.DONE);
                    break;
                case ACTION_ERROR:
                    fileAdapter.finishProgress(input.arg2, FileAdapter.Status.FAILED);
                    break;
                default:
            }
        }
    }

    class FileRunnable implements Runnable {

        private Handler handler;
        public FileRunnable(Handler handler){
            this.handler = handler;
        }
        @Override
        public void run() {

            DataOutputStream outputStream = IOStream.OUT_STREAM;
            ArrayList paths = getIntent().getStringArrayListExtra("files");
            long count = paths.size();
            int loop = 0;

            while (loop < count) {


                Message output = null;
                output = Message.obtain();

                File file = new File((String) paths.get(loop));
                long size = file.length();
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                String [] slice = file.getName().split("\\.");
                String name = String.format("%"+(31-slice[1].length())+"s",slice[0])+"."+slice[1];
                byte [] names = name.getBytes();
                byte [] bytes = new byte[64];
                byte [] sizes = Util.longToBytes(size);
                try {
                    outputStream.write(names,0,32);
                    outputStream.write(sizes,0,8);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();

                    output = Message.obtain();
                    output.arg1 = ACTION_ERROR;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                }

                output = Message.obtain();
                output.obj = new FileModel(file.getName(),0,size,FileAdapter.Status.SENDING);
                output.arg1 = ACTION_ADD;

                handler.sendMessage(output);

                long received = 0;
                long receiveCount = size / 64;


                for(int index = 0 ; index < receiveCount ; index++){
                    try {
                        inputStream.read(bytes,0,64);
                        outputStream.write(bytes,0,64);
                        outputStream.flush();
                        received += 64;

                        output = Message.obtain();
                        output.obj = received;
                        output.arg1 = ACTION_UPDATE;
                        output.arg2 = loop;
                        handler.sendMessage(output);

                    } catch (IOException e) {
                        e.printStackTrace();
                        output = Message.obtain();
                        output.arg1 = ACTION_ERROR;
                        output.arg2 = loop;
                        handler.sendMessage(output);
                    }
                }
                int remain = (int) (size % 64);
                try {
                    inputStream.read(bytes,0,remain);
                    outputStream.write(bytes,0,remain);
                    outputStream.flush();
                    received += remain;

                    output = Message.obtain();
                    output.obj = received;
                    output.arg1 = ACTION_UPDATE;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                } catch (IOException e) {
                    e.printStackTrace();
                    output = Message.obtain();
                    output.arg1 = ACTION_ERROR;
                    output.arg2 = loop;
                    handler.sendMessage(output);
                }

                output = Message.obtain();
                output.arg1 = ACTION_FINISH;
                output.arg2 = loop;
                handler.sendMessage(output);

                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                loop++;
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
