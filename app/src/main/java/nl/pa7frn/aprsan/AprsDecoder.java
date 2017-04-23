package nl.pa7frn.aprsan;

class AprsDecoder {
    private static final int POSITION_SYMBOL_TABLE = 9;
    private static final int POSITION_SYMBOL_CODE = 19;
    private static final int COMPR_POSITION_SYMBOL_TABLE = 1;
    private static final int COMPR_POSITION_SYMBOL_CODE = 10;
    private static final int POSITION_TS_SYMBOL_TABLE = 16;
    private static final int POSITION_TS_SYMBOL_CODE = 26;
    private static final int COMPR_POSITION_TS_SYMBOL_TABLE = 8;
    private static final int COMPR_POSITION_TS_SYMBOL_CODE = 17;
    private static final int OBJECT_SYMBOL_TABLE = 26;
    private static final int OBJECT_SYMBOL_CODE = 36;
    private static final int COMPR_OBJECT_SYMBOL_TABLE = 18;
    private static final int COMPR_OBJECT_SYMBOL_CODE = 27;
    private static final int MICE_SYMBOL_TABLE = 8;
    private static final int MICE_SYMBOL_CODE = 7;

    private String getInfoField(String packet) {
        String infoField = "";
        int fieldStart = packet.indexOf(":");
        if (fieldStart > -1) {
            fieldStart++;
            if (fieldStart < packet.length()) {
                infoField = packet.substring(fieldStart);
            }
        }
        return infoField;
    }

    private String getSymbol(String infoField, int posSymbolTable, int posSymbolCode) {
        if (infoField.length() < (posSymbolCode+1) ) {return ""; }
        String iconCode = "";
        iconCode += infoField.charAt(posSymbolTable);
        iconCode += infoField.charAt(posSymbolCode);
        return iconCode;
    }

    private String getCompressedSymbol(String infoField, int posSymbolTable, int posSymbolCode) {
        if (infoField.length() < (posSymbolCode+1) ) {return ""; }
        String iconCode = "";
        char chCheck = infoField.charAt(posSymbolTable);
        if ((chCheck < '0') || (chCheck > '9')) {
            iconCode += infoField.charAt(posSymbolTable);
            iconCode += infoField.charAt(posSymbolCode);
        }
        return iconCode;
    }

    private String getPositionSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_POSITION_SYMBOL_TABLE, COMPR_POSITION_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, POSITION_SYMBOL_TABLE, POSITION_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getPositionTimestampSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_POSITION_TS_SYMBOL_TABLE, COMPR_POSITION_TS_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, POSITION_TS_SYMBOL_TABLE, POSITION_TS_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getObjectSymbol(String infoField) {
        String iconCode = getCompressedSymbol(infoField, COMPR_OBJECT_SYMBOL_TABLE, COMPR_OBJECT_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(infoField, OBJECT_SYMBOL_TABLE, OBJECT_SYMBOL_CODE);
        }
        return iconCode;
    }

    private String getMicEIcon(String infoField) {
        if (infoField.length() < (MICE_SYMBOL_TABLE+1) ) {return ""; }
        String iconCode = "";
        iconCode += infoField.charAt(MICE_SYMBOL_TABLE);
        iconCode += infoField.charAt(MICE_SYMBOL_CODE);
        return iconCode;
    }

    private String getItemSymbol(String infoField) {
        int idx = infoField.indexOf("!");
        if (idx < 0) {
            idx = infoField.indexOf("_");
        }

        if (idx < 0) { return ""; }

        String itemField = infoField.substring(idx);
        String iconCode = getCompressedSymbol(itemField, COMPR_POSITION_SYMBOL_TABLE, COMPR_POSITION_SYMBOL_CODE);
        if (iconCode.equals("")) {
            iconCode = getSymbol(itemField, POSITION_SYMBOL_TABLE, POSITION_SYMBOL_CODE);
        }
        return iconCode;
    }

    boolean decodeAprsSymbol(String packet, AprsSymbol aprsSymbol) {

        if (packet.equals("")) { return false; }

        String infoField = getInfoField(packet);

        if (infoField.equals("")) { return false; }

        String iconCode;
        char dataType = infoField.charAt(0);
        switch (dataType) {
            case 0x1C:
                iconCode = getMicEIcon(infoField);
                break;
            case 0x1D:
                iconCode = getMicEIcon(infoField);
                break;
            case '!':
                iconCode = getPositionSymbol(infoField);
                break;
            case ')':
                iconCode = getItemSymbol(infoField);
                break;
            case '\'':
                iconCode = getMicEIcon(infoField);
                break;
            case '/':
                iconCode = getPositionTimestampSymbol(infoField);
                break;
            case ';':
                iconCode = getObjectSymbol(infoField);
                break;
            case '=':
                iconCode = getPositionSymbol(infoField);
                break;
            case '@':
                iconCode = getPositionTimestampSymbol(infoField);
                break;
            case 96: //'‘':
                iconCode = getMicEIcon(infoField);
                break;
            default:
                iconCode = "";
        }

        if (iconCode.equals("")) {
            int idx =  infoField.indexOf("!");
            if (idx > 0 ) {
                if (idx < 40) {
                    iconCode = getPositionSymbol(infoField.substring(idx));
                }
            }
        }

        if (iconCode.equals("")) { return false; }

        char symbolTable = iconCode.charAt(0);
        char symbolCode = iconCode.charAt(1);

        int bitmap = symbolCode-33;
        if (bitmap <  0) { return false; }
        if (bitmap > 93) { return false; }

        if (symbolTable != '/') {
            bitmap += 96;
        }

        boolean result = (aprsSymbol.symbol != bitmap);
        aprsSymbol.symbol = bitmap;

        return result;

    }
}