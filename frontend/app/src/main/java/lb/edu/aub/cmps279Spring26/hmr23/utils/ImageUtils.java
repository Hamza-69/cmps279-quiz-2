package lb.edu.aub.cmps279Spring26.hmr23.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private ImageUtils() {}

    

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] bytes = baos.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    

    public static Bitmap uriToBitmap(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    

    public static Bitmap scaleBitmap(Bitmap source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return source;
        }
        float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newW = Math.round(width * scale);
        int newH = Math.round(height * scale);
        return Bitmap.createScaledBitmap(source, newW, newH, true);
    }
}
