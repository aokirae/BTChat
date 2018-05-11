package jp.ac.titech.itpro.sdl.btchat.message;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatMessage implements Parcelable {
    final static String FIELD_SEQ = "seq";
    final static String FIELD_TIME = "time";
    final static String FIELD_CONTENT = "content";
    final static String FIELD_SENDER = "sender";

    // 通し番号
    public int seq;
    // 時間
    public long time;
    public String content;
    public String sender;

    public ChatMessage(int seq, long time, String content, String sender) {
        this.seq = seq;
        this.time = time;
        this.content = content;
        this.sender = sender;
    }

    private ChatMessage(Parcel in) {
        seq = in.readInt();
        time = in.readLong();
        content = in.readString();
        sender = in.readString();
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(seq);
        dest.writeLong(time);
        dest.writeString(content);
        dest.writeString(sender);
    }

    public static final Parcelable.Creator<ChatMessage> CREATOR =
            new Parcelable.Creator<ChatMessage>() {
                @Override
                public ChatMessage createFromParcel(Parcel src) {
                    return new ChatMessage(src);
                }

                @Override
                public ChatMessage[] newArray(int size) {
                    return new ChatMessage[size];
                }
            };
}
