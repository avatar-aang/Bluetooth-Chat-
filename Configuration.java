package admin.encryption;

import android.app.Application;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

/**
 * Created by Admin on 4/8/2018.
 */

public class Configuration extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EmojiManager.install(new GoogleEmojiProvider());
    }
}
