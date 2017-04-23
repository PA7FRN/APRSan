package nl.pa7frn.aprsan;

import android.graphics.Bitmap;

class AprsSymbol {
    public int symbol;
    public int overlay;
}

public class AprsSymbols {

    private Bitmap allSymbols;

    AprsSymbols(Bitmap aSymbols) {
        allSymbols = aSymbols;
    }

    Bitmap getSymbolBitmap(AprsSymbol aprsSymbol) {
        int symboleSize = 192; // 190 .. 195

        int idxX = aprsSymbol.symbol % 16;
        int idxY = aprsSymbol.symbol / 16;

        return Bitmap.createScaledBitmap(
                Bitmap.createBitmap(allSymbols, idxX*symboleSize, idxY*symboleSize, symboleSize, symboleSize),
                64,64,false
        );
    }

}
