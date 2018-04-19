package admin.encryption;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class ChatActivity extends AppCompatActivity {

    private Thread inputThread;
    private Thread outputThread;
    private ChatAdapter chatAdapter;
    private Queue inputQueue;
    private boolean available = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeWidget();
        initializeChat();
    }

    private void initializeWidget() {

        chatAdapter = new ChatAdapter();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        RecyclerView list = findViewById(R.id.chat_recycler_view);
        list.setAdapter(chatAdapter);
        list.setLayoutManager(layoutManager);

        EmojiEditText inputChat = findViewById(R.id.chat_edit_text);

        ImageView popup = findViewById(R.id.chat_emoji_image);

        View.OnClickListener popupListener = param -> {

            LinearLayout layout = findViewById(R.id.chat_linear_layout);
            EmojiPopup emojiPopup = EmojiPopup.Builder.fromRootView(layout).build(inputChat);
            emojiPopup.toggle();
        };
        popup.setOnClickListener(popupListener);


        View.OnClickListener sendClickListener = param->{
            String input = inputChat.getText().toString();
            if(!input.isEmpty()){
                inputQueue.add(input);
            }
            inputChat.setText("");
        };
        ImageView sender = findViewById(R.id.chat_send_image);
        sender.setOnClickListener(sendClickListener);

    }

    private void initializeChat() {

        inputQueue = new ArrayDeque();

        Handler inputHandler = new InputHandler();
        InputRunnable inputRunnable = new InputRunnable(inputHandler);
        inputThread = new Thread(inputRunnable);


        Handler outputHandler = new OutputHandler();
        OutputRunnable outputRunnable = new OutputRunnable(outputHandler);
        outputThread = new Thread(outputRunnable);

        inputThread.start();
        outputThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(inputThread.isAlive()){
            inputThread.interrupt();
        }

        if(outputThread.isAlive()){
            outputThread.interrupt();
        }

        try {
            IOStream.IN_STREAM.close();
            IOStream.OUT_STREAM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InputHandler extends Handler {
        @Override
        public void handleMessage(Message input) {
            super.handleMessage(input);
            if(available) {
                ChatModel chatModel = (ChatModel) input.obj;
                chatAdapter.addChild(chatModel);
            }else {
                Toast.makeText(ChatActivity.this,"Connection lost !!!",Toast.LENGTH_LONG).show();
            }
        }
    }

    class OutputHandler extends Handler {
        @Override
        public void handleMessage(Message output) {
            super.handleMessage(output);
            if(available) {
                ChatModel chatModel = (ChatModel) output.obj;
                chatAdapter.addChild(chatModel);
            }else {
                Toast.makeText(ChatActivity.this,"Connection lost !!!",Toast.LENGTH_LONG).show();
            }
        }
    }

    class InputRunnable implements Runnable {

        private Handler handler;

        public InputRunnable(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            DataInputStream  stream = IOStream.IN_STREAM;
            while (available){

                String time = Util.getCurrentTime();
                boolean local = false;

                Message output = Message.obtain();

                try {
                    String comment = stream.readUTF();
                    ChatModel chatModel = new ChatModel(comment,time,local);
                    output.obj = chatModel;
                } catch (IOException e) {
                    e.printStackTrace();
                    available = false;
                    handler.sendMessage(output);
                    return;
                }
                handler.sendMessage(output);
            }
        }
    }

    class OutputRunnable implements Runnable {

        private Handler handler;

        public OutputRunnable(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            DataOutputStream stream = IOStream.OUT_STREAM;
            while (available){
                if(!inputQueue.isEmpty()){
                    String comment = (String) inputQueue.remove();
                    String time = Util.getCurrentTime();
                    boolean local = true;
                    ChatModel chatModel = new ChatModel(comment,time,local);
                    Message output = Message.obtain();
                    try {

                        stream.writeUTF(comment);
                        stream.flush();
                        output.obj = chatModel;
                        handler.sendMessage(output);

                    } catch (IOException e) {
                        e.printStackTrace();
                        available = false;
                        handler.sendMessage(output);
                    }
                }
            }
        }
    }

    public class ChatAdapter  extends RecyclerView.Adapter {

        private ArrayList chatModelList;
        public ChatAdapter() {
            super();
            chatModelList = new ArrayList();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup container, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(container.getContext());
            View parent = inflater.inflate(R.layout.layout_chat_model,container,false);
            RecyclerView.ViewHolder holder = new ChatHolder(parent);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            onPopulateView(holder, chatModelList.get(position),position);
        }

        public void onPopulateView(RecyclerView.ViewHolder viewHolder, Object object , int position) {

            ChatHolder holder = (ChatHolder) viewHolder;
            ChatModel chatModel = (ChatModel) object;
            holder.updateWidget(chatModel);
        }

        public void addChild(Object val, int position) {
            chatModelList.add(position,val);
            notifyItemInserted(position);
        }



        public void addChild(Object val) {
            int position = chatModelList.size();
            addChild(val,position);
        }

        @Override
        public int getItemCount() {
            return chatModelList.size();
        }

        class ChatHolder extends RecyclerView.ViewHolder {

            private LinearLayout layout;
            private TextView comment, time;

            public ChatHolder(View itemView) {
                super(itemView);
                initializeWidget(itemView);
            }

            private void initializeWidget(View parent) {
                layout = parent.findViewById(R.id.comment_linear_layout);
                comment = parent.findViewById(R.id.comment_text);
                time = parent.findViewById(R.id.comment_time);
            }

            public void updateWidget(ChatModel chatModel) {

                comment.setText(chatModel.comment);
                time.setText(chatModel.time);

                if(chatModel.local) {
                    //sender
                    int background = R.drawable.chat_out_balloon;
                    layout.setBackgroundResource(background);

                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) layout.getLayoutParams();
                    params.setMarginStart(72);

                }else {
                    //receiver
                    int background = R.drawable.chat_in_balloon;
                    layout.setBackgroundResource(background);

                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) layout.getLayoutParams();
                    params.setMarginEnd(72);
                }

            }
        }
    }

    class ChatModel {

        public String comment, time;
        public boolean local;

        public ChatModel(String comment, String time, boolean local) {
            this.comment = comment;
            this.time = time;
            this.local = local;
        }
    }

}
