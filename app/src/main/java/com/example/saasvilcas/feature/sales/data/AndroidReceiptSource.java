package com.example.saasvilcas.feature.sales.data;
import android.content.Context;
import com.example.saasvilcas.shared.media.*;
import com.example.saasvilcas.feature.sales.domain.SalesException;
/** Adapts shared image preparation to the sales error contract. */
public final class AndroidReceiptSource implements ReceiptSource {
    private final AndroidImageSource source;
    public AndroidReceiptSource(Context context) { source=new AndroidImageSource(context); }
    @Override public byte[] jpeg(String uri) throws SalesException {
        try { return source.jpeg(uri); }
        catch(ImageException error) { throw new SalesException(error.getMessage(),0,false); }
    }
}
