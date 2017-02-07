package err.sfp.SocialNetworking;

import android.graphics.Bitmap;

/**
 * Created by Err on 17-1-2.
 */

//TODO optimize memory for Item class for each layout for overhead reduction.
public class ItemInfo {
    public Bitmap image;
    public String title;
    public String subTitle;
    public String message;
    public long dateMillis;
    public byte[] databaseId;
    public byte[] senderId;
    public String songUrl;
    public ItemInfo(Bitmap image, String title, String subTitle, String message,
                    long dateMillis, byte[] databaseId, byte[] senderId, String songUrl) {
        this.image = image;
        this.title = title;
        this.subTitle = subTitle;
        this.message = message;
        this.dateMillis = dateMillis;
        this.databaseId = databaseId;
        this.senderId = senderId;
        this.songUrl = songUrl;
    }

    public static ItemInfo[] initArray(int size) {
        ItemInfo[] ii = new ItemInfo[size];
        for(int i = 0; i < size; i++)
            ii[i] = new ItemInfo(null, null, null, null, 0, new byte[0], new byte[0], null);

        return ii;
    }
    // setters, getters are more expensive than variables (in Android)
}
